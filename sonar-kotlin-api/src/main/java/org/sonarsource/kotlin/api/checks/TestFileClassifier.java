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
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.WildcardPattern;

/**
 * Test-file heuristic for rule execution only; it never affects metrics, which use the platform
 * {@link org.sonar.api.batch.fs.InputFile#type()}. Applies only when the project has not declared its
 * test sources.
 *
 * <p>Registered once with the project {@link Configuration} (which gates the heuristic) plus path
 * globs and, optionally, a {@code detector} for richer checks that reads a caller-supplied
 * {@link Context}. Per file, {@link #looksLikeTestFile(InputFile)} returns the classification.
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

  private static final Predicate<Context> NO_DETECTOR = context -> false;

  private final List<WildcardPattern> patterns;
  private final Predicate<Context> detector;
  private final boolean testSourcesConfigured;
  // Warn once, here, so the heuristic behaves the same for every analyzer using this classifier.
  private boolean heuristicWarningEmitted = false;

  private TestFileClassifier(List<WildcardPattern> patterns, Predicate<Context> detector, boolean testSourcesConfigured) {
    this.patterns = patterns;
    this.detector = detector;
    this.testSourcesConfigured = testSourcesConfigured;
  }

  /** Registers path {@code globs} (Ant patterns); with none, a generic set of test directories is used. */
  public static TestFileClassifier of(Configuration configuration, String... globs) {
    return of(configuration, NO_DETECTOR, globs);
  }

  /** As {@link #of(Configuration, String...)}, plus a {@code detector} matched when no glob does. */
  public static TestFileClassifier of(Configuration configuration, Predicate<Context> detector, String... globs) {
    List<WildcardPattern> patterns = globs.length == 0
      ? DEFAULT_PATTERNS
      : Arrays.stream(globs).map(WildcardPattern::create).collect(Collectors.toUnmodifiableList());
    return new TestFileClassifier(patterns, detector, isTestSourceConfigured(configuration));
  }

  /** Path-only classification; convenience overload with an empty {@link Context}. */
  public boolean looksLikeTestFile(InputFile inputFile) {
    return looksLikeTestFile(inputFile, Context.empty());
  }

  /**
   * True when test sources are not configured and either a glob matches the project-relative path or the
   * detector accepts {@code context}. Warns once when the heuristic first classifies a file.
   */
  @SuppressWarnings("deprecation") // relativePath() is the only project-relative accessor
  public boolean looksLikeTestFile(InputFile inputFile, Context context) {
    String path = inputFile.relativePath();
    boolean detected = !testSourcesConfigured
      && (patterns.stream().anyMatch(pattern -> pattern.match(path)) || detector.test(context));
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
   * Per-file input a detector inspects. An analyzer implements it to carry what its detector needs
   * (e.g. the parsed tree) and downcasts to that implementation.
   */
  public interface Context {

    /** A context carrying no information; detectors needing more return false. */
    static Context empty() {
      return EMPTY_CONTEXT;
    }
  }
}
