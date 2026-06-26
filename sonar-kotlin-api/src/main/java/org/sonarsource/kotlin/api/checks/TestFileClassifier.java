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
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.WildcardPattern;

/**
 * Decides whether a file is a test file for rule-execution purposes only; it never influences metric
 * computation, which always relies on the platform {@link org.sonar.api.batch.fs.InputFile#type()}.
 *
 * <p>An analyzer registers its language's test-file path patterns together with the project
 * {@link Configuration} via {@link #of(Configuration, String...)}. The heuristic is a fallback for
 * when the project has not declared its test sources: the config gate is evaluated once at
 * registration (it is a per-project fact), so the per-file {@link #looksLikeTestFile(String)} takes
 * only the path.
 *
 * <p>Usage:
 * <pre>{@code
 * // once per analysis, where Configuration is available (e.g. the sensor):
 * var testFiles = TestFileClassifier.of(sensorContext.config(), "**​/test/**", "**​/*Test.kt");
 * // per file:
 * if (testFiles.looksLikeTestFile(inputFile.uri().getPath())) { ... }
 * }</pre>
 */
public final class TestFileClassifier {

  /** Generic opt-out: disables the test-file heuristic for every analyzer that uses this classifier. */
  public static final String HEURISTIC_DISABLED_KEY = "sonar.testFileHeuristic.disabled";

  private static final Logger LOG = LoggerFactory.getLogger(TestFileClassifier.class);

  private static final String HEURISTIC_APPLIED_WARNING =
    "Test files were detected using a path heuristic because \"sonar.tests\" is not set. To improve the " +
      "analysis accuracy, it is recommended to configure it, e.g.: \"sonar.tests=src/test\".";

  private final List<WildcardPattern> patterns;
  private final boolean testSourcesConfigured;
  // Warn once, here, so the heuristic behaves the same for every analyzer using this classifier.
  private boolean heuristicWarningEmitted = false;

  private TestFileClassifier(List<WildcardPattern> patterns, boolean testSourcesConfigured) {
    this.patterns = patterns;
    this.testSourcesConfigured = testSourcesConfigured;
  }

  /**
   * Registers the test-file scope: its path patterns (Ant globs) plus the project {@code configuration}
   * that gates the heuristic. The gate is evaluated once here.
   */
  public static TestFileClassifier of(Configuration configuration, String... globs) {
    return new TestFileClassifier(
      Arrays.stream(globs).map(WildcardPattern::create).collect(Collectors.toUnmodifiableList()),
      isTestSourceConfigured(configuration));
  }

  /** True when {@code path} matches a registered pattern and the project has not configured test sources. */
  public boolean looksLikeTestFile(String path) {
    return looksLikeTestFile(path, Context.empty());
  }

  /**
   * True when {@code path} matches a registered pattern and the project has not configured test sources.
   * Emits a one-time warning the first time the heuristic classifies a file, so users are nudged to set
   * {@code sonar.tests}. {@code context} is unused today; it is the stable extension point for future
   * context-aware logic.
   */
  @SuppressWarnings("java:S1172")
  public boolean looksLikeTestFile(String path, Context context) {
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
   * Surrounding information passed alongside the path to {@link #looksLikeTestFile(String, Context)}.
   *
   * <p>Intentionally empty for now: a stable extension point so future accessors (e.g. file content or
   * the analyzed language) can be added without changing the classification signature.
   */
  public interface Context {

    Context EMPTY = new Context() {
    };

    /** Returns a context that carries no additional information. */
    static Context empty() {
      return EMPTY;
    }
  }
}
