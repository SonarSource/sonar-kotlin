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
package org.sonarsource.kotlin.api.checks;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.WildcardPattern;

/**
 * Decides whether a file is a test file for rule-execution purposes only; it never influences metric
 * computation, which always relies on the platform {@link org.sonar.api.batch.fs.InputFile#type()}.
 *
 * <p>An analyzer registers its test-file path patterns together with the project {@link Configuration}
 * that gates the heuristic (evaluated once at registration, as it is a per-project fact). The
 * heuristic is a fallback for when the project has not declared its test sources.
 *
 * <p>Usage:
 * <pre>{@code
 * // once per analysis, where Configuration is available (e.g. the sensor):
 * var testFiles = TestFileClassifier.of(sensorContext.config(), "**​/test/**", "**​/*Test.kt");
 * // per file (matched on the project-relative path, so workspace directories are not matched):
 * if (testFiles.looksLikeTestFile(inputFile)) { ... }
 * }</pre>
 */
public final class TestFileClassifier {

  /** Generic opt-out: disables the test-file heuristic for every analyzer that uses this classifier. */
  public static final String HEURISTIC_DISABLED_KEY = "sonar.testFileHeuristic.disabled";

  private static final Logger LOG = LoggerFactory.getLogger(TestFileClassifier.class);

  private static final String HEURISTIC_APPLIED_WARNING =
    "Test files were detected using a path heuristic because \"sonar.tests\" is not set. To improve the " +
      "analysis accuracy, it is recommended to configure it, e.g.: \"sonar.tests=src/test\".";

  // Single shared empty context; held on the outer class so the interface exposes only empty().
  private static final Context EMPTY_CONTEXT = new Context() {
  };

  // Fallback when no patterns are registered: test directories only, to minimize false positives.
  private static final List<WildcardPattern> DEFAULT_PATTERNS =
    Stream.of("**/test/**", "**/tests/**", "**/__tests__/**")
      .map(WildcardPattern::create)
      .collect(Collectors.toUnmodifiableList());

  private final List<WildcardPattern> patterns;
  private final boolean testSourcesConfigured;
  // Warn once, here, so the heuristic behaves the same for every analyzer using this classifier.
  private boolean heuristicWarningEmitted = false;

  private TestFileClassifier(List<WildcardPattern> patterns, boolean testSourcesConfigured) {
    this.patterns = patterns;
    this.testSourcesConfigured = testSourcesConfigured;
  }

  /**
   * Registers the test-file scope: {@code globs} (Ant path patterns) are matched against the file path.
   * When no globs are given, a generic set of test directories is used as a fallback.
   * The {@code configuration} gates the heuristic; the gate is evaluated once here.
   */
  public static TestFileClassifier of(Configuration configuration, String... globs) {
    List<WildcardPattern> patterns = globs.length == 0
      ? DEFAULT_PATTERNS
      : Arrays.stream(globs).map(WildcardPattern::create).collect(Collectors.toUnmodifiableList());
    return new TestFileClassifier(patterns, isTestSourceConfigured(configuration));
  }

  /**
   * True when the file is recognized as a test file and the project has not configured test sources.
   * Convenience overload with an empty {@link Context}.
   */
  public boolean looksLikeTestFile(InputFile inputFile) {
    return looksLikeTestFile(inputFile, Context.empty());
  }

  /**
   * True when the file's project-relative path matches a registered glob and the project has not
   * configured test sources. Emits a one-time warning the first time the heuristic classifies a file,
   * so users are nudged to set {@code sonar.tests}. {@code context} is unused today; it is the stable
   * per-file extension point.
   */
  @SuppressWarnings("deprecation") // relativePath() is the only project-relative accessor
  public boolean looksLikeTestFile(InputFile inputFile, Context context) {
    String path = inputFile.relativePath();
    boolean detected = !testSourcesConfigured && patterns.stream().anyMatch(pattern -> pattern.match(path));
    if (detected && !heuristicWarningEmitted) {
      heuristicWarningEmitted = true;
      LOG.warn(HEURISTIC_APPLIED_WARNING);
    }
    return detected;
  }

  private static boolean isTestSourceConfigured(Configuration config) {
    return isSet(config, "sonar.tests")
      || isSet(config, "sonar.test.inclusions")
      || isSet(config, "sonar.test.exclusions")
      || config.getBoolean(HEURISTIC_DISABLED_KEY).orElse(false);
  }

  private static boolean isSet(Configuration config, String key) {
    return config.get(key).filter(value -> !value.isBlank()).isPresent();
  }

  /**
   * Per-file information a future classifier may inspect (e.g. the parsed tree, annotations, imports).
   * Unused today; it stays a stable per-file extension point.
   */
  public interface Context {

    /** Returns a context that carries no additional information. */
    static Context empty() {
      return EMPTY_CONTEXT;
    }
  }
}
