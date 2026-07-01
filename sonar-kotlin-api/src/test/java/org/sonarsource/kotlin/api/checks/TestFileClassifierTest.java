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

  @Test
  void matches_test_paths_when_test_sources_not_configured() {
    var classifier = classifier(config());
    assertThat(classifier.looksLikeTestFile("src/main/kotlin/FooTest.kt")).isTrue();
    assertThat(classifier.looksLikeTestFile("a/FooTests.kt")).isTrue();
    assertThat(classifier.looksLikeTestFile("a/FooSpec.kt")).isTrue();
    assertThat(classifier.looksLikeTestFile("a/FooIT.kt")).isTrue();
    assertThat(classifier.looksLikeTestFile("src/test/kotlin/Foo.kt")).isTrue();
    assertThat(classifier.looksLikeTestFile("module/tests/Foo.kt")).isTrue();
  }

  @Test
  void does_not_match_non_test_paths() {
    var classifier = classifier(config());
    assertThat(classifier.looksLikeTestFile("src/main/kotlin/Foo.kt")).isFalse();
    // case-sensitive suffix: "audit.kt" must not match "*IT.kt"
    assertThat(classifier.looksLikeTestFile("src/main/kotlin/audit.kt")).isFalse();
    assertThat(classifier.looksLikeTestFile("")).isFalse();
  }

  @Test
  void gate_disables_heuristic_when_test_sources_configured() {
    var testPath = "src/main/kotlin/FooTest.kt";
    assertThat(classifier(config("sonar.tests", "src/test")).looksLikeTestFile(testPath)).isFalse();
    assertThat(classifier(config("sonar.test.inclusions", "**/*Test.kt")).looksLikeTestFile(testPath)).isFalse();
    assertThat(classifier(config("sonar.test.exclusions", "**/generated/**")).looksLikeTestFile(testPath)).isFalse();
  }

  @Test
  void gate_ignores_blank_property() {
    assertThat(classifier(config("sonar.tests", "  ")).looksLikeTestFile("a/FooTest.kt")).isTrue();
  }

  @Test
  void opt_out_property_disables_heuristic() {
    var classifier = classifier(config(TestFileClassifier.HEURISTIC_DISABLED_KEY, "true"));
    assertThat(classifier.looksLikeTestFile("a/FooTest.kt")).isFalse();
  }

  @Test
  void context_overload_matches_convenience_overload() {
    var classifier = classifier(config());
    assertThat(classifier.looksLikeTestFile("a/FooTest.kt", TestFileClassifier.Context.empty()))
      .isEqualTo(classifier.looksLikeTestFile("a/FooTest.kt"));
  }

  @Test
  void warns_once_when_the_heuristic_first_classifies_a_test_file() {
    var classifier = classifier(config());
    classifier.looksLikeTestFile("a/FooTest.kt");
    classifier.looksLikeTestFile("b/BarTest.kt");
    classifier.looksLikeTestFile("c/Main.kt"); // no match, no extra warning

    assertThat(logTester.logs(Level.WARN)).hasSize(1);
    assertThat(logTester.logs(Level.WARN).get(0)).contains("sonar.tests");
  }

  @Test
  void does_not_warn_when_test_sources_are_configured() {
    var classifier = classifier(config("sonar.tests", "src/test"));
    classifier.looksLikeTestFile("a/FooTest.kt");

    assertThat(logTester.logs(Level.WARN)).isEmpty();
  }
}
