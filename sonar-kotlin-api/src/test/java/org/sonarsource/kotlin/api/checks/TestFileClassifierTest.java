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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;

import static org.assertj.core.api.Assertions.assertThat;

class TestFileClassifierTest {

  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5();

  private static final String[] GLOBS = {
    "**/test/**", "**/tests/**",
    "**/*Test.kt", "**/*Tests.kt", "**/*Spec.kt", "**/*IT.kt"
  };

  private static Configuration config(String... keyValues) {
    var settings = new MapSettings();
    for (int i = 0; i < keyValues.length; i += 2) {
      settings.setProperty(keyValues[i], keyValues[i + 1]);
    }
    return settings.asConfig();
  }

  private static TestFileClassifier classifier(Configuration config) {
    return TestFileClassifier.of(config, GLOBS);
  }

  private static InputFile file(String relativePath) {
    return new TestInputFileBuilder("module", relativePath).build();
  }

  @Test
  void matches_test_paths_when_test_sources_not_configured() {
    var classifier = classifier(config());
    assertThat(classifier.looksLikeTestFile(file("src/main/kotlin/FooTest.kt"))).isTrue();
    assertThat(classifier.looksLikeTestFile(file("a/FooTests.kt"))).isTrue();
    assertThat(classifier.looksLikeTestFile(file("a/FooSpec.kt"))).isTrue();
    assertThat(classifier.looksLikeTestFile(file("a/FooIT.kt"))).isTrue();
    assertThat(classifier.looksLikeTestFile(file("src/test/kotlin/Foo.kt"))).isTrue();
    assertThat(classifier.looksLikeTestFile(file("module/tests/Foo.kt"))).isTrue();
  }

  @Test
  void falls_back_to_generic_test_directories_when_no_patterns_registered() {
    var classifier = TestFileClassifier.of(config()); // no globs registered
    assertThat(classifier.looksLikeTestFile(file("src/test/kotlin/Foo.kt"))).isTrue();
    assertThat(classifier.looksLikeTestFile(file("a/tests/Foo.kt"))).isTrue();
    assertThat(classifier.looksLikeTestFile(file("a/__tests__/Foo.js"))).isTrue();
    // fallback matches directories only, not test-named files
    assertThat(classifier.looksLikeTestFile(file("src/main/kotlin/FooTest.kt"))).isFalse();
  }

  @Test
  void does_not_match_non_test_paths() {
    var classifier = classifier(config());
    assertThat(classifier.looksLikeTestFile(file("src/main/kotlin/Foo.kt"))).isFalse();
    // case-sensitive suffix: "audit.kt" must not match "*IT.kt"
    assertThat(classifier.looksLikeTestFile(file("src/main/kotlin/audit.kt"))).isFalse();
  }

  @Test
  void gate_disables_heuristic_when_test_sources_configured() {
    var testFile = file("src/main/kotlin/FooTest.kt");
    assertThat(classifier(config("sonar.tests", "src/test")).looksLikeTestFile(testFile)).isFalse();
    assertThat(classifier(config("sonar.test.inclusions", "**/*Test.kt")).looksLikeTestFile(testFile)).isFalse();
    assertThat(classifier(config("sonar.test.exclusions", "**/generated/**")).looksLikeTestFile(testFile)).isFalse();
  }

  @Test
  void gate_ignores_blank_property() {
    assertThat(classifier(config("sonar.tests", "  ")).looksLikeTestFile(file("a/FooTest.kt"))).isTrue();
  }

  @Test
  void opt_out_property_disables_heuristic() {
    var classifier = classifier(config(TestFileClassifier.HEURISTIC_DISABLED_KEY, "true"));
    assertThat(classifier.looksLikeTestFile(file("a/FooTest.kt"))).isFalse();
  }

  @Test
  void context_overload_matches_convenience_overload() {
    var classifier = classifier(config());
    var testFile = file("a/FooTest.kt");
    assertThat(classifier.looksLikeTestFile(testFile, TestFileClassifier.Context.empty()))
      .isEqualTo(classifier.looksLikeTestFile(testFile));
  }

  @Test
  void warns_once_when_the_heuristic_first_classifies_a_test_file() {
    var classifier = classifier(config());
    classifier.looksLikeTestFile(file("a/FooTest.kt"));
    classifier.looksLikeTestFile(file("b/BarTest.kt"));
    classifier.looksLikeTestFile(file("c/Main.kt")); // no match, no extra warning

    assertThat(logTester.logs(Level.WARN)).hasSize(1);
    assertThat(logTester.logs(Level.WARN).get(0)).contains("sonar.tests");
  }

  @Test
  void does_not_warn_when_test_sources_are_configured() {
    var classifier = classifier(config("sonar.tests", "src/test"));
    classifier.looksLikeTestFile(file("a/FooTest.kt"));

    assertThat(logTester.logs(Level.WARN)).isEmpty();
  }
}
