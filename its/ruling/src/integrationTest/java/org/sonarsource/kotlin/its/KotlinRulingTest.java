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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonarsource.scanner.integrationtester.dsl.EngineVersion;
import com.sonarsource.scanner.integrationtester.dsl.Log;
import com.sonarsource.scanner.integrationtester.dsl.ScannerInput;
import com.sonarsource.scanner.integrationtester.dsl.SonarProjectContext;
import com.sonarsource.scanner.integrationtester.dsl.SonarServerContext;
import com.sonarsource.scanner.integrationtester.dsl.issue.FileLevelIssue;
import com.sonarsource.scanner.integrationtester.dsl.issue.Issue;
import com.sonarsource.scanner.integrationtester.dsl.issue.TextRangeIssue;
import com.sonarsource.scanner.integrationtester.runner.ScannerRunner;
import com.sonarsource.scanner.integrationtester.runner.ScannerRunnerConfig;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ruling test running the analyzer through the sonar-scanner-integration-tester (SIT) instead of the orchestrator:
 * the scanner engine runs in-process against a mock server, so no SonarQube server and no license are needed.
 * Issues are read directly off the in-process scanner report and diffed here in Java against the golden files
 * under {@code src/integrationTest/resources/expected}, rather than routing through the sonar-lits-plugin.
 * <p>
 * {@code test_kotlin_language_server} is not ported here: it needs a real Gradle build to supply the Java
 * classpath and its golden files are project-dir-relative. It stays on the orchestrator in {@code :its:sq-integration}.
 */
class KotlinRulingTest {

  private static final String LANGUAGE_KEY = "kotlin";

  private static final String REPO_KEY = "kotlin";

  /** Analyses run against {@code its/}: golden component keys are {@code <projectKey>:sources/kotlin/<corpus>/...}. */
  private static final Path BASE_DIRECTORY = new File("..").toPath().toAbsolutePath().normalize();

  private static final Path EXPECTED_ROOT = new File("src/integrationTest/resources/expected/kotlin").toPath();

  private static final Path ACTUAL_ROOT = new File("build/reports/ruling").toPath();

  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

  private static final boolean REPORT_ALL = "true".equals(System.getProperty("reportAll"));

  private static final String HEADER_FORMAT = "/\\*\n \\* Copyright \\d{4}-\\d{4} JetBrains s\\.r\\.o\\.";

  private static final Path KOTLIN_PLUGIN_LOCATION = FileLocation.byWildcardFilename(
    new File("../../sonar-kotlin-plugin/build/libs"), "sonar-kotlin-plugin.jar")
    .getFile().toPath();

  private static SonarServerContext serverContext;

  @BeforeAll
  static void setUp() {
    var activeRules = RulingRules.nativeRules(KOTLIN_PLUGIN_LOCATION, LANGUAGE_KEY, Map.of(
      "S1451", Map.of(
        "headerFormat", HEADER_FORMAT,
        "isRegularExpression", "true")));

    serverContext = SonarServerContext.builder()
      .withProduct(SonarServerContext.Product.SERVER)
      .withServerEdition(SonarServerContext.ServerEdition.ENTERPRISE)
      .withEngineVersion(EngineVersion.latestRelease())
      // SIT's mock server indexes files by statically declared suffixes; the plugin's own KotlinLanguage extension
      // does not make the engine index .kt/.kts files by itself.
      .withLanguage(LANGUAGE_KEY, "Kotlin", ".kt,.kts")
      .withPlugin(KOTLIN_PLUGIN_LOCATION)
      .withProjectContext(SonarProjectContext.builder().withActiveRules(activeRules).build())
      .build();
  }

  @AfterAll
  static void tearDown() {
    ScannerRunner.closeAllClassLoaders();
  }

  @Test
  @EnabledIfEnvironmentVariable(named = "KOTLIN_COMPILER_IT_ENABLED", matches = "true")
  void test_kotlin_compiler() throws IOException {
    List<String> exclusions = List.of(
      "**/testData/**/*",
      "sources/kotlin/kotlin/compiler/daemon/src/org/jetbrains/kotlin/daemon/CompileServiceImpl.kt",
      "sources/kotlin/kotlin/compiler/psi/src/org/jetbrains/kotlin/psi/psiUtil/ktPsiUtil.kt",
      "sources/kotlin/kotlin/compiler/psi/src/org/jetbrains/kotlin/psi/psiUtil/psiUtils.kt",
      "sources/kotlin/kotlin/j2k/src/org/jetbrains/kotlin/j2k/ast/Statements.kt",
      "sources/kotlin/kotlin/libraries/stdlib/js/src/org.w3c/org.w3c.dom.kt",
      "sources/kotlin/kotlin/libraries/stdlib/js/src/org.w3c/org.khronos.webgl.kt"
    );
    analyzeAndAssertDifferences("kotlin", Map.of(
      "sonar.inclusions", "sources/kotlin/kotlin/**/*.kt",
      "sonar.exclusions", String.join(",", exclusions)));
  }

  @Test
  void test_resources_sources() throws IOException {
    analyzeAndAssertDifferences("test-resources-sources", Map.of(
      "sonar.inclusions", "ruling/src/integrationTest/resources/sources/kotlin/**/*.kt",
      "sonar.java.libraries", System.getProperty("gradle.main.compile.classpath").replace(File.pathSeparatorChar, ',')
    ));
  }

  @Test
  void test_kotlin_android() throws IOException {
    analyzeAndAssertDifferences("android-architecture-components", Map.of(
      "sonar.inclusions", "sources/kotlin/android-architecture-components/**/*.kt",
      "sonar.exclusions", "**/testData/**/*"
    ));
  }

  @Test
  void test_kotlin_corda() throws IOException {
    analyzeAndAssertDifferences("corda", Map.of(
      "sonar.inclusions", "sources/kotlin/corda/**/*.kt",
      "sonar.exclusions", "**/testData/**/*"
    ));
  }

  @Test
  void test_kotlin_intellij_rust() throws IOException {
    analyzeAndAssertDifferences("intellij-rust", Map.of(
      "sonar.inclusions", "sources/kotlin/intellij-rust/**/*.kt",
      "sonar.exclusions", "**/testData/**/*"
    ));
  }

  @Test
  void test_kotlin_okio() throws IOException {
    analyzeAndAssertDifferences("okio", Map.of(
      "sonar.inclusions", "sources/kotlin/okio/**/*.kt",
      "sonar.exclusions", "**/testData/**/*"));
  }

  private void analyzeAndAssertDifferences(String projectName, Map<String, String> additionalProperties) throws IOException {
    String projectKey = REPO_KEY + "-" + projectName + "-project";

    Map<String, String> properties = new HashMap<>(additionalProperties);
    properties.put("sonar.slang.converter.validation", "log");
    properties.put("sonar.slang.duration.statistics", "true");
    properties.put("sonar.kotlin.performance.measure", "true");
    properties.put("sonar.internal.analysis.failFast", "true");

    Path performanceMeasuresDirectory = Path.of("build", "performance", projectName);
    Files.createDirectories(performanceMeasuresDirectory);
    properties.put("sonar.kotlin.performance.measure.json",
      performanceMeasuresDirectory.resolve("sonar.kotlin.performance.measure.json").toString());

    var input = ScannerInput.create(projectKey, BASE_DIRECTORY)
      .withSourceDirs(".")
      .withSourceEncoding("utf-8")
      .withScmDisabled()
      .withCpdExclusionForAllFiles()
      .withScannerProperties(properties)
      .build();

    var result = ScannerRunner.run(serverContext, input, ScannerRunnerConfig.builder().build());

    var errorLogs = result.logOutput().stream()
      .filter(log -> log.level() == Log.Level.ERROR)
      .map(log -> log.message() + (log.stacktrace() != null ? "\n" + log.stacktrace() : ""))
      .reduce("", (a, b) -> a + "\n" + b);
    assertThat(result.exitCode()).describedAs("Scanner should succeed. Errors:%s", errorLogs).isZero();

    var actualIssuesByRule = groupActualIssues(projectKey, result.scannerOutputReader().getProject().getAllIssues());
    var expectedIssuesByRule = REPORT_ALL ? Map.<String, SortedMap<String, List<Integer>>>of() : loadExpectedIssuesByRule(projectName);

    var ruleKeys = new TreeSet<>(actualIssuesByRule.keySet());
    ruleKeys.addAll(expectedIssuesByRule.keySet());

    // Dump every rule that has a golden file, not just the ones that still fire: a rule that used to report
    // issues but now reports zero must still overwrite its golden file with an empty one when copied over,
    // otherwise the stale golden file keeps reporting "missing" forever.
    dumpActualIssues(projectName, ruleKeys, actualIssuesByRule);

    var differences = new ArrayList<String>();
    for (var ruleKey : ruleKeys) {
      differences.addAll(diffIssues(ruleKey,
        expectedIssuesByRule.getOrDefault(ruleKey, Collections.emptySortedMap()),
        actualIssuesByRule.getOrDefault(ruleKey, Collections.emptySortedMap())));
    }
    assertThat(differences).isEmpty();
  }

  private static Map<String, SortedMap<String, List<Integer>>> groupActualIssues(String projectKey, List<Issue> issues) {
    Map<String, SortedMap<String, List<Integer>>> byRule = new TreeMap<>();
    for (Issue issue : issues) {
      String componentKey;
      int line;
      if (issue instanceof TextRangeIssue tri) {
        componentKey = projectKey + ":" + tri.componentPath();
        line = tri.line();
      } else if (issue instanceof FileLevelIssue fli) {
        componentKey = projectKey + ":" + fli.componentPath();
        line = 0;
      } else {
        // ProjectIssue: no componentPath(), key it by the project itself.
        componentKey = projectKey;
        line = 0;
      }
      byRule.computeIfAbsent(issue.ruleKey(), k -> new TreeMap<>())
        .computeIfAbsent(componentKey, k -> new ArrayList<>())
        .add(line);
    }
    byRule.values().forEach(byComponent -> byComponent.values().forEach(Collections::sort));
    return byRule;
  }

  private static Map<String, SortedMap<String, List<Integer>>> loadExpectedIssuesByRule(String projectName) throws IOException {
    Map<String, SortedMap<String, List<Integer>>> byRule = new TreeMap<>();
    Path expectedDir = EXPECTED_ROOT.resolve(projectName);
    try (var files = Files.list(expectedDir)) {
      for (var file : files.sorted().toList()) {
        byRule.put(ruleKeyFromFileName(file.getFileName().toString()), readIssuesFile(file));
      }
    }
    return byRule;
  }

  private static SortedMap<String, List<Integer>> readIssuesFile(Path file) throws IOException {
    var json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
    SortedMap<String, List<Integer>> byComponent = new TreeMap<>();
    for (var entry : json.entrySet()) {
      List<Integer> lines = new ArrayList<>();
      entry.getValue().getAsJsonArray().forEach(line -> lines.add(line.getAsInt()));
      Collections.sort(lines);
      byComponent.put(entry.getKey(), lines);
    }
    return byComponent;
  }

  private static void dumpActualIssues(String projectName, Set<String> ruleKeys, Map<String, SortedMap<String, List<Integer>>> actualIssuesByRule) throws IOException {
    Path actualDir = ACTUAL_ROOT.resolve(projectName);
    Files.createDirectories(actualDir);
    for (var ruleKey : ruleKeys) {
      var byComponent = actualIssuesByRule.getOrDefault(ruleKey, Collections.emptySortedMap());
      var json = new JsonObject();
      for (var entry : byComponent.entrySet()) {
        var lines = new JsonArray();
        entry.getValue().forEach(lines::add);
        json.add(entry.getKey(), lines);
      }
      Files.writeString(actualDir.resolve(fileNameFromRuleKey(ruleKey)), GSON.toJson(json));
    }
  }

  /**
   * Golden files are named {@code <repoKey>-<rule>.json} (e.g. {@code kotlin-S100.json}). Decoding by stripping
   * the known, fixed {@code "kotlin-"} prefix - rather than splitting on the first '-' - keeps the round-trip
   * correct even if a rule id itself ever contained a dash; the ruling module only ever activates rules under
   * the single, fixed "kotlin" repository key.
   */
  private static String ruleKeyFromFileName(String fileName) {
    var base = fileName.substring(0, fileName.length() - ".json".length());
    var prefix = REPO_KEY + "-";
    if (!base.startsWith(prefix)) {
      throw new IllegalStateException("Expected file name '" + fileName + "' does not start with '" + prefix + "'");
    }
    return REPO_KEY + ":" + base.substring(prefix.length());
  }

  private static String fileNameFromRuleKey(String ruleKey) {
    var prefix = REPO_KEY + ":";
    if (!ruleKey.startsWith(prefix)) {
      throw new IllegalStateException("Expected rule key '" + ruleKey + "' does not start with '" + prefix + "'");
    }
    return REPO_KEY + "-" + ruleKey.substring(prefix.length()) + ".json";
  }

  /**
   * A multiset diff per rule, then per component: line lists are compared rather than sets, because the same
   * rule can validly report twice on the same line for two different reasons, and a naive set diff would
   * silently swallow a duplicate that should show up as a difference.
   */
  private static List<String> diffIssues(String ruleKey, SortedMap<String, List<Integer>> expected, SortedMap<String, List<Integer>> actual) {
    Set<String> componentKeys = new TreeSet<>(expected.keySet());
    componentKeys.addAll(actual.keySet());
    List<String> differences = new ArrayList<>();
    for (String componentKey : componentKeys) {
      List<Integer> missing = new ArrayList<>(expected.getOrDefault(componentKey, List.of()));
      List<Integer> unexpected = new ArrayList<>(actual.getOrDefault(componentKey, List.of()));
      for (Iterator<Integer> it = unexpected.iterator(); it.hasNext(); ) {
        // line must stay boxed: missing.remove(line) must resolve to remove(Object), not remove(int index).
        Integer line = it.next();
        if (missing.remove(line)) {
          it.remove();
        }
      }
      var path = componentKey.contains(":") ? componentKey.substring(componentKey.indexOf(':') + 1) : componentKey;
      missing.forEach(line -> differences.add("[" + ruleKey + "] " + (line == 0 ? path : path + ":" + line) + " missing"));
      unexpected.forEach(line -> differences.add("[" + ruleKey + "] " + (line == 0 ? path : path + ":" + line) + " unexpected"));
    }
    return differences;
  }
}
