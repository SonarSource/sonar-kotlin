/*
 * SonarSource SLang
 * Copyright (C) 2018-2021 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.kotlin.plugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.sensor.error.AnalysisError;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.issue.IssueLocation;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.CoreMetrics;
import org.sonarsource.slang.testing.AbstractSensorTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarsource.slang.testing.TextRangeAssert.assertTextRange;

class KotlinSensorTest extends AbstractSensorTest {

  @Test
  void test_one_rule() {
    InputFile inputFile = createInputFile("file1.kt", "" +
      "fun main(args: Array<String>) {\nprint (1 == 1);}");
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("S1764");
    sensor(checkFactory).execute(context);
    Collection<Issue> issues = context.allIssues();
    assertThat(issues).hasSize(1);
    Issue issue = issues.iterator().next();
    assertThat(issue.ruleKey().rule()).isEqualTo("S1764");
    IssueLocation location = issue.primaryLocation();
    assertThat(location.inputComponent()).isEqualTo(inputFile);
    assertThat(location.message()).isEqualTo("Correct one of the identical sub-expressions on both sides this operator");
    assertTextRange(location.textRange()).hasRange(2, 12, 2, 13);
  }

  @Test
  void test_commented_code() {
    InputFile inputFile = createInputFile("file1.kt", "" +
      "fun main(args: Array<String>) {\n" +
      "//fun foo () { if (true) {print(\"string literal\");}}\n" +
      "print (1 == 1);\n" +
      "print(b);\n" +
      "//a b c ...\n" +
      "foo();\n" +
      "// Coefficients of polynomial\n" +
      "val b = DoubleArray(n) // linear\n" +
      "val c = DoubleArray(n + 1) // quadratic\n" +
      "val d = DoubleArray(n) // cubic\n" +
      "}");
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("S125");
    sensor(checkFactory).execute(context);
    Collection<Issue> issues = context.allIssues();
    assertThat(issues).hasSize(1);
    Issue issue = issues.iterator().next();
    assertThat(issue.ruleKey().rule()).isEqualTo("S125");
    IssueLocation location = issue.primaryLocation();
    assertThat(location.inputComponent()).isEqualTo(inputFile);
    assertThat(location.message()).isEqualTo("Remove this commented out code.");
  }

  @Test
  void simple_file() {
    InputFile inputFile = createInputFile("file1.kt", "" +
      "fun main(args: Array<String>) {\nprint (1 == 1); print(\"abc\"); }\ndata class A(val a: Int)");
    context.fileSystem().add(inputFile);
    sensor(checkFactory()).execute(context);
    assertThat(context.highlightingTypeAt(inputFile.key(), 1, 0)).containsExactly(TypeOfText.KEYWORD);
    assertThat(context.highlightingTypeAt(inputFile.key(), 1, 3)).isEmpty();
    assertThat(context.measure(inputFile.key(), CoreMetrics.NCLOC).value()).isEqualTo(3);
    assertThat(context.measure(inputFile.key(), CoreMetrics.COMMENT_LINES).value()).isZero();
    assertThat(context.measure(inputFile.key(), CoreMetrics.FUNCTIONS).value()).isEqualTo(1);
    assertThat(context.measure(inputFile.key(), CoreMetrics.CLASSES).value()).isEqualTo(1);
    assertThat(context.cpdTokens(inputFile.key()).get(1).getValue()).isEqualTo("print(1==1);print(\"LITERAL\");}");
    assertThat(context.measure(inputFile.key(), CoreMetrics.COMPLEXITY).value()).isEqualTo(1);
    assertThat(context.measure(inputFile.key(), CoreMetrics.STATEMENTS).value()).isEqualTo(2);

    // FIXME
    //assertThat(logTester.logs()).contains("1 source files to be analyzed");
  }

  @Test
  void test_issue_suppression() {
    InputFile inputFile = createInputFile("file1.kt", "" +
      "@SuppressWarnings(\"kotlin:S1764\")\n" +
      "fun main() {\nprint (1 == 1);}\n" +
      "@SuppressWarnings(value=[\"kotlin:S1764\"])\n" +
      "fun main2() {\nprint (1 == 1);}");
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("S1764");
    sensor(checkFactory).execute(context);
    Collection<Issue> issues = context.allIssues();
    assertThat(issues).isEmpty();
  }

  @Test
  void test_issue_not_suppressed() {
    InputFile inputFile = createInputFile("file1.kt", "" +
      "@SuppressWarnings(\"S1764\")\n" +
      "fun main() {\nprint (1 == 1);}\n" +
      "@SuppressWarnings(value=[\"scala:S1764\"])\n" +
      "fun main2() {\nprint (1 == 1);}");
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("S1764");
    sensor(checkFactory).execute(context);
    Collection<Issue> issues = context.allIssues();
    assertThat(issues).hasSize(2);
  }

  @Test
  void test_fail_parsing() {
    InputFile inputFile = createInputFile("file1.kt", "" +
      "enum class A { <!REDECLARATION!>FOO<!>,<!REDECLARATION!>FOO<!> }");
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("S1764");
    sensor(checkFactory).execute(context);
    Collection<AnalysisError> analysisErrors = context.allAnalysisErrors();
    assertThat(analysisErrors).hasSize(1);
    AnalysisError analysisError = analysisErrors.iterator().next();
    assertThat(analysisError.inputFile()).isEqualTo(inputFile);
    assertThat(analysisError.message()).isEqualTo("Unable to parse file: file1.kt");
    TextPointer textPointer = analysisError.location();
    assertThat(textPointer).isNotNull();
    assertThat(textPointer.line()).isEqualTo(1);
    assertThat(textPointer.lineOffset()).isEqualTo(14);

    assertThat(logTester.logs()).contains(String.format("Unable to parse file: %s. Parse error at position 1:14", inputFile.uri()));
  }

  @Override
  protected String repositoryKey() {
    return KotlinPlugin.KOTLIN_REPOSITORY_KEY;
  }

  @Override
  protected KotlinLanguage language() {
    return new KotlinLanguage(new MapSettings().asConfig());
  }

  private KotlinSensor sensor(CheckFactory checkFactory) {
    return new KotlinSensor(checkFactory, fileLinesContextFactory, new NoSonarFilter(), language());
  }

}
