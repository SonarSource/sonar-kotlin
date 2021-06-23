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
package org.sonarsource.slang.plugin;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.error.AnalysisError;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.issue.IssueLocation;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.Language;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.Version;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonarsource.slang.api.ASTConverter;
import org.sonarsource.slang.api.TopLevelTree;
import org.sonarsource.slang.checks.CommentedCodeCheck;
import org.sonarsource.slang.checks.IdenticalBinaryOperandCheck;
import org.sonarsource.slang.checks.StringLiteralDuplicatedCheck;
import org.sonarsource.slang.checks.api.SlangCheck;
import org.sonarsource.slang.parser.SLangConverter;
import org.sonarsource.slang.parser.SlangCodeVerifier;
import org.sonarsource.slang.testing.AbstractSensorTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.sonarsource.slang.plugin.SlangSensorTest.SlangLanguage.SLANG;
import static org.sonarsource.slang.testing.TextRangeAssert.assertTextRange;

class SlangSensorTest extends AbstractSensorTest {

  @Test
  void test_one_rule() {
    InputFile inputFile = createInputFile("file1.slang", "" +
      "fun main() {\nprint (1 == 1);}");
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
  void test_rule_with_gap() {
    InputFile inputFile = createInputFile("file1.slang", "" +
      "fun f() { print(\"string literal\"); print(\"string literal\"); print(\"string literal\"); }");
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("S1192");
    sensor(checkFactory).execute(context);
    Collection<Issue> issues = context.allIssues();
    assertThat(issues).hasSize(1);
    Issue issue = issues.iterator().next();
    assertThat(issue.ruleKey().rule()).isEqualTo("S1192");
    IssueLocation location = issue.primaryLocation();
    assertThat(location.inputComponent()).isEqualTo(inputFile);
    assertThat(location.message()).isEqualTo("Define a constant instead of duplicating this literal \"string literal\" 3 times.");
    assertTextRange(location.textRange()).hasRange(1, 16, 1, 32);
    assertThat(issue.gap()).isEqualTo(2.0);
  }

  @Test
  void test_commented_code() {
    InputFile inputFile = createInputFile("file1.slang", "" +
      "fun main() {\n" +
      "// fun foo() { if (true) {print(\"string literal\");}}\n" +
      "print (1 == 1);\n" +
      "print(b);\n" +
      "// a b c ...\n" +
      "foo();\n" +
      "// Coefficients of polynomial\n" +
      "val b = DoubleArray(n); // linear\n" +
      "val c = DoubleArray(n + 1); // quadratic\n" +
      "val d = DoubleArray(n); // cubic\n" +
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
    InputFile inputFile = createInputFile("file1.slang", "" +
      "fun main(int x) {\nprint (1 == 1); print(\"abc\"); }\nclass A {}");
    context.fileSystem().add(inputFile);
    sensor(checkFactory()).execute(context);
    assertThat(context.highlightingTypeAt(inputFile.key(), 1, 0)).containsExactly(TypeOfText.KEYWORD);
    assertThat(context.highlightingTypeAt(inputFile.key(), 1, 3)).isEmpty();
    assertThat(context.measure(inputFile.key(), CoreMetrics.NCLOC).value()).isEqualTo(3);
    assertThat(context.measure(inputFile.key(), CoreMetrics.COMMENT_LINES).value()).isZero();
    assertThat(context.measure(inputFile.key(), CoreMetrics.FUNCTIONS).value()).isEqualTo(1);
    assertThat(context.measure(inputFile.key(), CoreMetrics.CLASSES).value()).isEqualTo(1);
    assertThat(context.cpdTokens(inputFile.key()).get(1).getValue()).isEqualTo("print(1==1);print(LITERAL);}");
    assertThat(context.measure(inputFile.key(), CoreMetrics.COMPLEXITY).value()).isEqualTo(1);
    assertThat(context.measure(inputFile.key(), CoreMetrics.STATEMENTS).value()).isEqualTo(2);

    // FIXME
    //assertThat(logTester.logs()).contains("1 source files to be analyzed");
  }

  @Test
  void suppress_issues_in_class() {
    InputFile inputFile = createInputFile("file1.slang", "" +
      "@Suppress(\"slang:S1764\")\n" +
      "class { fun main() {\nprint (1 == 1);} }");
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("S1764");
    sensor(checkFactory).execute(context);
    Collection<Issue> issues = context.allIssues();
    assertThat(issues).isEmpty();
  }

  @Test
  void suppress_issues_in_method() {
    InputFile inputFile = createInputFile("file1.slang", "" +
      "class { " +
      "@Suppress(\"slang:S1764\")\n" +
      "fun suppressed() {\nprint (1 == 1);} " +
      "fun notSuppressed() {\nprint (123 == 123);} " +
      "}");
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("S1764");
    sensor(checkFactory).execute(context);
    Collection<Issue> issues = context.allIssues();
    assertThat(issues).hasSize(1);
    Issue issue = issues.iterator().next();
    IssueLocation location = issue.primaryLocation();
    assertTextRange(location.textRange()).hasRange(4, 14, 4, 17);
  }

  @Test
  void suppress_issues_in_var() {
    InputFile inputFile = createInputFile("file1.slang", "class A {void fun bar() {\n" +
      "@Suppress(\"slang:S1764\")\n" +
      "int val b = (1 == 1);\n" +
      "int val c = (1 == 1);\n" +
      "}}");
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("S1764");
    sensor(checkFactory).execute(context);
    Collection<Issue> issues = context.allIssues();
    assertThat(issues).hasSize(1);
    Issue issue = issues.iterator().next();
    IssueLocation location = issue.primaryLocation();
    assertTextRange(location.textRange()).hasRange(4, 18, 4, 19);
  }

  @Test
  void suppress_issues_in_parameter() {
    InputFile inputFile = createInputFile("file1.slang",
      "class A {void fun bar(@Suppress(\"slang:S1764\") int a = (1 == 1), int b = (1 == 1)) {} }");
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("S1764");
    sensor(checkFactory).execute(context);
    Collection<Issue> issues = context.allIssues();
    assertThat(issues).hasSize(1);
    Issue issue = issues.iterator().next();
    IssueLocation location = issue.primaryLocation();
    assertTextRange(location.textRange()).hasRange(1, 79, 1, 80);
  }

  @Test
  void suppress_multiples_issues() {
    InputFile inputFile = createInputFile("file1.slang", "" +
      "@Suppress(\"slang:S1764\", value=\"slang:S1192\")\n" +
      "fun suppressed() {\nprint (1 == 1);print(\"string literal\"); print(\"string literal\"); print(\"string literal\"); } " +
      "@Suppress(value={\"slang:S1764\",\"slang:S1192\"})\n" +
      "fun suppressed() {\nprint (1 == 1);print(\"string literal\"); print(\"string literal\"); print(\"string literal\"); } " +
      "@Suppress(\"slang:S1192\")\n" +
      "@Suppress(\"slang:S1764\")\n" +
      "fun suppressed() {\nprint (1 == 1);print(\"string literal\"); print(\"string literal\"); print(\"string literal\"); } " +
      "@Suppress(value={\"slang:S1764\"})\n" +
      "fun suppressed() {\nprint (1 == 1);}");
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("S1764", "S1192");
    sensor(checkFactory).execute(context);
    Collection<Issue> issues = context.allIssues();
    assertThat(issues).isEmpty();
  }

  @Test
  void do_not_suppress_bad_key() {
    InputFile inputFile = createInputFile("file1.slang", "" +
      "@Suppress(\"slang:S1234\")\n" +
      "fun notSuppressed() {\nprint (1 == 1);} " +
      "@Suppress(\"EQUALITY\")\n" +
      "fun notSuppressed1() {\nprint (123 == 123);} " +
      "@SuppressSonarIssue(\"slang:S1764\")\n" +
      "fun notSuppressed2() {\nprint (1 == 1);}\n" +
      "@Suppress(\"UNUSED_PARAMETER\")\n" +
      "fun notSuppressed3() {\nprint (1 == 1);}\n" +
      "@Suppress(\"unused_parameter\")\n" +
      "fun notSuppressed4() {\nprint (1 == 1);} ");
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("S1764");
    sensor(checkFactory).execute(context);
    Collection<Issue> issues = context.allIssues();
    assertThat(issues).hasSize(5);
  }

  @Test
  void test_fail_input() throws IOException {
    InputFile inputFile = createInputFile("fakeFile.slang", "");
    InputFile spyInputFile = spy(inputFile);
    when(spyInputFile.contents()).thenThrow(IOException.class);
    context.fileSystem().add(spyInputFile);
    CheckFactory checkFactory = checkFactory("S1764");
    sensor(checkFactory).execute(context);
    Collection<AnalysisError> analysisErrors = context.allAnalysisErrors();
    assertThat(analysisErrors).hasSize(1);
    AnalysisError analysisError = analysisErrors.iterator().next();
    assertThat(analysisError.inputFile()).isEqualTo(spyInputFile);
    assertThat(analysisError.message()).isEqualTo("Unable to parse file: fakeFile.slang");
    assertThat(analysisError.location()).isNull();

    assertThat(logTester.logs()).contains(String.format("Unable to parse file: %s. ", inputFile.uri()));
  }

  @Test
  void test_fail_parsing() {
    InputFile inputFile = createInputFile("file1.slang", "" +
      "\n class A {\n" +
      " fun x() {}\n" +
      " fun y() {}");
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("ParsingError");
    sensor(checkFactory).execute(context);

    Collection<Issue> issues = context.allIssues();
    assertThat(issues).hasSize(1);
    Issue issue = issues.iterator().next();
    assertThat(issue.ruleKey().rule()).isEqualTo("ParsingError");
    IssueLocation location = issue.primaryLocation();
    assertThat(location.inputComponent()).isEqualTo(inputFile);
    assertThat(location.message()).isEqualTo("A parsing error occurred in this file.");
    assertTextRange(location.textRange()).hasRange(2, 0, 2, 10);

    Collection<AnalysisError> analysisErrors = context.allAnalysisErrors();
    assertThat(analysisErrors).hasSize(1);
    AnalysisError analysisError = analysisErrors.iterator().next();
    assertThat(analysisError.inputFile()).isEqualTo(inputFile);
    assertThat(analysisError.message()).isEqualTo("Unable to parse file: file1.slang");
    TextPointer textPointer = analysisError.location();
    assertThat(textPointer).isNotNull();
    assertThat(textPointer.line()).isEqualTo(2);
    assertThat(textPointer.lineOffset()).isEqualTo(1);

    assertThat(logTester.logs()).contains(String.format("Unable to parse file: %s. Parse error at position 2:1", inputFile.uri()));
  }

  @Test
  void test_fail_parsing_without_parsing_error_rule_activated() {
    InputFile inputFile = createInputFile("file1.slang", "{");
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("S1764");
    sensor(checkFactory).execute(context);
    assertThat(context.allIssues()).isEmpty();
    assertThat(context.allAnalysisErrors()).hasSize(1);
  }

  @Test
  void test_empty_file() {
    InputFile inputFile = createInputFile("empty.slang", "\t\t  \r\n  \n ");
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("S1764");
    sensor(checkFactory).execute(context);
    Collection<AnalysisError> analysisErrors = context.allAnalysisErrors();
    assertThat(analysisErrors).isEmpty();
    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.WARN)).isEmpty();
  }

  @Test
  void test_failure_in_check() {
    InputFile inputFile = createInputFile("file1.slang", "fun f() {}");
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = mock(CheckFactory.class);
    Checks checks = mock(Checks.class);
    SlangCheck failingCheck = init ->
      init.register(TopLevelTree.class, (ctx, tree) -> {
        throw new IllegalStateException("BOUM");
      });
    when(checks.ruleKey(failingCheck)).thenReturn(RuleKey.of(repositoryKey(), "failing"));
    when(checkFactory.create(repositoryKey())).thenReturn(checks);
    when(checks.all()).thenReturn(Collections.singletonList(failingCheck));
    sensor(checkFactory).execute(context);

    Collection<AnalysisError> analysisErrors = context.allAnalysisErrors();
    assertThat(analysisErrors).hasSize(1);
    AnalysisError analysisError = analysisErrors.iterator().next();
    assertThat(analysisError.inputFile()).isEqualTo(inputFile);
    assertThat(logTester.logs()).contains("Cannot analyse 'file1.slang': BOUM");
  }

  @Test
  void test_descriptor() {
    DefaultSensorDescriptor sensorDescriptor = new DefaultSensorDescriptor();
    SlangSensor sensor = sensor(mock(CheckFactory.class));
    sensor.describe(sensorDescriptor);
    assertThat(sensorDescriptor.languages()).hasSize(1);
    assertThat(sensorDescriptor.languages()).containsExactly("slang");
    assertThat(sensorDescriptor.name()).isEqualTo("SLang Sensor");
  }

  @Test
  void test_cancellation() {
    InputFile inputFile = createInputFile("file1.slang", "" +
      "fun main() {\nprint (1 == 1);}");
    context.fileSystem().add(inputFile);
    CheckFactory checkFactory = checkFactory("S1764");
    context.setCancelled(true);
    sensor(checkFactory).execute(context);
    Collection<Issue> issues = context.allIssues();
    assertThat(issues).isEmpty();
  }

  @Test
  void test_sonarlint_context() {
    SonarRuntime sonarLintRuntime = SonarRuntimeImpl.forSonarLint(Version.create(3, 9));
    SensorContextTester context = SensorContextTester.create(baseDir);
    InputFile inputFile = createInputFile("file1.slang", "" +
      "fun main(int x) {\nprint (1 == 1); print(\"abc\"); }\nclass A {}");
    context.fileSystem().add(inputFile);
    context.setRuntime(sonarLintRuntime);
    sensor(checkFactory("S1764")).execute(context);

    assertThat(context.allIssues()).hasSize(1);

    // No CPD, highlighting and metrics in SonarLint
    assertThat(context.highlightingTypeAt(inputFile.key(), 1, 0)).isEmpty();
    assertThat(context.measure(inputFile.key(), CoreMetrics.NCLOC)).isNull();
    assertThat(context.cpdTokens(inputFile.key())).isNull();

    // FIXME
    //assertThat(logTester.logs()).contains("1 source files to be analyzed");
  }

  @Override
  protected String repositoryKey() {
    return "slang";
  }

  @Override
  protected Language language() {
    return SLANG;
  }

  private SlangSensor sensor(CheckFactory checkFactory) {
    return new SlangSensor(new NoSonarFilter(), fileLinesContextFactory, SLANG) {
      @Override
      protected ASTConverter astConverter(SensorContext sensorContext) {
        return new SLangConverter();
      }

      @Override
      protected Checks<SlangCheck> checks() {
        Checks<SlangCheck> checks = checkFactory.create(repositoryKey());
        checks.addAnnotatedChecks(
          StringLiteralDuplicatedCheck.class,
          new CommentedCodeCheck(new SlangCodeVerifier()),
          IdenticalBinaryOperandCheck.class);
        return checks;
      }

      @Override
      protected String repositoryKey() {
        return SlangSensorTest.this.repositoryKey();
      }
    };
  }

  enum SlangLanguage implements Language {
    SLANG;

    @Override
    public String getKey() {
      return "slang";
    }

    @Override
    public String getName() {
      return "SLang";
    }

    @Override
    public String[] getFileSuffixes() {
      return new String[]{".slang"};
    }
  }


}
