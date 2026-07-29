/*
 * SonarSource Kotlin
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonarsource.kotlin.its;

import com.sonar.orchestrator.locator.FileLocation;
import com.sonarsource.scanner.integrationtester.dsl.ActiveRule;
import com.sonarsource.scanner.integrationtester.dsl.EngineVersion;
import com.sonarsource.scanner.integrationtester.dsl.Log;
import com.sonarsource.scanner.integrationtester.dsl.ScannerInput;
import com.sonarsource.scanner.integrationtester.dsl.ScannerOutputReader;
import com.sonarsource.scanner.integrationtester.dsl.ScannerResult;
import com.sonarsource.scanner.integrationtester.dsl.ScannerResultSuccess;
import com.sonarsource.scanner.integrationtester.dsl.SonarProjectContext;
import com.sonarsource.scanner.integrationtester.dsl.SonarServerContext;
import com.sonarsource.scanner.integrationtester.dsl.issue.Issue;
import com.sonarsource.scanner.integrationtester.dsl.issue.TextRangeIssue;
import com.sonarsource.scanner.integrationtester.runner.ScannerRunner;
import com.sonarsource.scanner.integrationtester.runner.ScannerRunnerConfig;
import com.sonarsource.scanner.integrationtester.utility.QualityProfileLoader;
import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import org.junit.jupiter.api.AfterAll;

/**
 * Base class for the plugin integration tests running through the sonar-scanner-integration-tester (SIT) instead of
 * the orchestrator. Analyses run the scanner engine in-process against a mock server (so there is no server
 * lifecycle and no license), and results are read from the {@link ScannerOutputReader} - the same protobuf report
 * the scanner would send to a server - rather than from the Web API.
 * <p>
 * Note: unlike the Web API {@code Issues.Issue}, scanner-output issues expose only rule key, message, text range and
 * flows. There is no server-side enrichment (type / severity / impacts / clean-code attribute / effort), and no
 * project-level aggregate measures (those are computed by Compute Engine), so assertions on those cannot be
 * reproduced here. Tests that depend on them stay in the {@code :its:sq-integration} module.
 */
public abstract class SitTestBase {

  public static final FileLocation KOTLIN_PLUGIN_LOCATION = FileLocation.byWildcardFilename(
    new File("../../sonar-kotlin-plugin/build/libs"), "sonar-kotlin-plugin.jar");

  protected static final String LANGUAGE_KEY = "kotlin";

  @AfterAll
  static void closeClassLoaders() {
    // The engine runs in isolated class loaders; they leak (together with the analyzer's native resources) unless
    // they are explicitly closed at the end of each test class.
    ScannerRunner.closeAllClassLoaders();
  }

  protected static AnalysisResult analyze(String projectKey, String projectDir) {
    return analyze(projectKey, projectDir, List.of(), Map.of());
  }

  protected static AnalysisResult analyze(String projectKey, String projectDir, Map<String, String> properties) {
    return analyze(projectKey, projectDir, List.of(), properties);
  }

  protected static AnalysisResult analyze(String projectKey, String projectDir, List<ActiveRule> activeRules) {
    return analyze(projectKey, projectDir, activeRules, Map.of());
  }

  protected static AnalysisResult analyze(String projectKey, String projectDir, List<ActiveRule> activeRules, Map<String, String> properties) {
    var projectContext = SonarProjectContext.builder();
    if (!activeRules.isEmpty()) {
      projectContext.withActiveRules(activeRules);
    }

    var serverContext = SonarServerContext.builder()
      .withProduct(SonarServerContext.Product.SERVER)
      .withServerEdition(SonarServerContext.ServerEdition.ENTERPRISE)
      .withEngineVersion(EngineVersion.latestRelease())
      // SIT's mock server indexes files by statically declared suffixes; the plugin's own KotlinLanguage extension
      // does not make the engine index .kt/.kts files by itself.
      .withLanguage(LANGUAGE_KEY, "Kotlin", ".kt,.kts")
      .withPlugin(KOTLIN_PLUGIN_LOCATION.getFile().toPath())
      .withProjectContext(projectContext.build())
      .build();

    var input = ScannerInput.create(projectKey, new File(projectDir).toPath().toAbsolutePath().normalize())
      .withSourceDirs(".")
      .withScmDisabled()
      .withScannerProperties(new HashMap<>(properties))
      .build();

    return new AnalysisResult(ScannerRunner.run(serverContext, input, ScannerRunnerConfig.builder().build()));
  }

  /**
   * The active rules of one of the quality-profile XMLs that the orchestrator used to restore server-side. The same
   * files are reused verbatim, so the set of activated rules is unchanged.
   */
  protected static List<ActiveRule> profileRules(String profileXmlResource) {
    return QualityProfileLoader.loadActiveRulesFromXmlProfile(Path.of("src/integrationTest/resources", profileXmlResource));
  }

  /**
   * Unlike the orchestrator (where an unconfigured project falls back to the "Sonar way" server-side default
   * profile), SIT's mock server activates only the rules explicitly passed to {@link #analyze}. Tests that used to
   * rely on the default profile must activate the rules they check for.
   */
  protected static ActiveRule activeRule(String ruleKey) {
    return ActiveRule.builder()
      .withKey(LANGUAGE_KEY, ruleKey)
      .withName(ruleKey)
      .withLanguageKey(LANGUAGE_KEY)
      .withSeverity(ActiveRule.Severity.MAJOR)
      .build();
  }

  /**
   * Wrapper over {@link ScannerResult} exposing the assertions the orchestrator-based tests used to run against the
   * Web API, but reading from the scanner output report instead.
   */
  public static final class AnalysisResult {
    private final ScannerResult result;
    @Nullable
    private final ScannerOutputReader reader;

    AnalysisResult(ScannerResult result) {
      this.result = result;
      this.reader = result instanceof ScannerResultSuccess success ? success.scannerOutputReader() : null;
    }

    public ScannerOutputReader reader() {
      if (reader == null) {
        throw new IllegalStateException("Scanner failed (exit code " + result.exitCode() + "):\n" + errorLogs());
      }
      return reader;
    }

    public List<Issue> allIssues() {
      return reader().getProject().getAllIssues();
    }

    public List<Issue> issuesForRule(String ruleKey) {
      return allIssues().stream().filter(issue -> issue.ruleKey().equals(ruleKey)).toList();
    }

    /** All issues imported from an external linter report. */
    public List<TextRangeIssue> externalIssues() {
      return allIssues().stream()
        .filter(issue -> issue.ruleKey().startsWith("external_"))
        .filter(TextRangeIssue.class::isInstance)
        .map(TextRangeIssue.class::cast)
        .toList();
    }

    @Nullable
    public Integer measureAsInt(String relativePath, String metricKey) {
      ScannerOutputReader.AnalyzedComponent component = reader().getFile(relativePath);
      if (component == null) {
        return null;
      }
      ScannerOutputReader.Measure measure = component.getMeasure(metricKey);
      return measure == null ? null : ((Number) measure.value()).intValue();
    }

    public int analyzedFileCount() {
      return reader().getFiles().size();
    }

    public String logMessages() {
      return result.logOutput().stream()
        .map(Log::message)
        .filter(Objects::nonNull)
        .reduce("", (a, b) -> a + "\n" + b);
    }

    public String errorLogs() {
      return result.logOutput().stream()
        .filter(log -> log.level() == Log.Level.ERROR)
        .map(log -> log.message() + (log.stacktrace() != null ? "\n" + log.stacktrace() : ""))
        .reduce("", (a, b) -> a + "\n" + b);
    }
  }
}
