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
package org.sonarsource.slang.externalreport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.ExternalIssue;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.api.utils.log.ThreadLocalLogTester;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

@EnableRuleMigrationSupport
class CheckstyleFormatImporterTest {

  static final Path PROJECT_DIR = Paths.get("src", "test", "resources", "externalreport");

  static final String LINTER_KEY = "test-linter";

  @Rule
  public ThreadLocalLogTester logTester = new ThreadLocalLogTester();

  @Test
  void import_detekt_issues() throws IOException {
    List<ExternalIssue> externalIssues = importIssues("detekt-checkstyle.xml");
    assertThat(externalIssues).hasSize(3);

    ExternalIssue first = externalIssues.get(0);
    assertThat(first.primaryLocation().inputComponent().key()).isEqualTo("externalreport-project:main.kt");
    assertThat(first.ruleKey().rule()).isEqualTo("detekt.EmptyIfBlock");
    assertThat(first.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(first.severity()).isEqualTo(Severity.MINOR);
    assertThat(first.primaryLocation().message()).isEqualTo("This empty block of code can be removed.");
    assertThat(first.primaryLocation().textRange().start().line()).isEqualTo(3);

    ExternalIssue second = externalIssues.get(1);
    assertThat(second.primaryLocation().inputComponent().key()).isEqualTo("externalreport-project:main.kt");
    assertThat(second.ruleKey().rule()).isEqualTo("detekt.MagicNumber");
    assertThat(second.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(second.severity()).isEqualTo(Severity.MAJOR);
    assertThat(second.remediationEffort().longValue()).isEqualTo(5L);
    assertThat(second.primaryLocation().message()).isEqualTo("This expression contains a magic number. Consider defining it to a well named constant.");
    assertThat(second.primaryLocation().textRange().start().line()).isEqualTo(3);

    ExternalIssue third = externalIssues.get(2);
    assertThat(third.primaryLocation().inputComponent().key()).isEqualTo("externalreport-project:A.kt");
    assertThat(third.ruleKey().rule()).isEqualTo("detekt.EqualsWithHashCodeExist");
    assertThat(third.type()).isEqualTo(RuleType.BUG);
    assertThat(third.severity()).isEqualTo(Severity.MAJOR);
    assertThat(third.primaryLocation().message()).isEqualTo("A class should always override hashCode when overriding equals and the other way around.");
    assertThat(third.primaryLocation().textRange().start().line()).isEqualTo(3);

    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
  }

  @Test
  void import_golandci_lint_issues() throws IOException {
    List<ExternalIssue> externalIssues = importIssues("golandci-lint-report.xml");
    assertThat(externalIssues).hasSize(1);

    ExternalIssue first = externalIssues.get(0);
    assertThat(first.primaryLocation().inputComponent().key()).isEqualTo("externalreport-project:TabCharacter.go");
    assertThat(first.ruleKey().rule()).isEqualTo("deadcode");
    assertThat(first.type()).isEqualTo(RuleType.BUG);
    assertThat(first.severity()).isEqualTo(Severity.MAJOR);
    assertThat(first.primaryLocation().message()).isEqualTo("`three` is unused");
    assertThat(first.primaryLocation().textRange().start().line()).isEqualTo(4);

    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
  }

  @Test
  void no_issues_with_invalid_report_path() throws IOException {
    List<ExternalIssue> externalIssues = importIssues("invalid-path.txt");
    assertThat(externalIssues).isEmpty();
    assertThat(onlyOneLogElement(logTester.logs(LoggerLevel.ERROR)))
      .startsWith("No issue information will be saved as the report file '")
      .endsWith("invalid-path.txt' can't be read.");
  }

  @Test
  void no_issues_with_invalid_checkstyle_file() throws IOException {
    List<ExternalIssue> externalIssues = importIssues("not-checkstyle-file.xml");
    assertThat(externalIssues).isEmpty();
    assertThat(onlyOneLogElement(logTester.logs(LoggerLevel.ERROR)))
      .startsWith("No issue information will be saved as the report file '")
      .endsWith("not-checkstyle-file.xml' can't be read.");
  }

  @Test
  void no_issues_with_invalid_xml_report() throws IOException {
    List<ExternalIssue> externalIssues = importIssues("invalid-file.xml");
    assertThat(externalIssues).isEmpty();
    assertThat(onlyOneLogElement(logTester.logs(LoggerLevel.ERROR)))
      .startsWith("No issue information will be saved as the report file '")
      .endsWith("invalid-file.xml' can't be read.");
  }

  @Test
  void issues_when_xml_file_has_errors() throws IOException {
    List<ExternalIssue> externalIssues = importIssues("detekt-checkstyle-with-errors.xml");
    assertThat(externalIssues).hasSize(2);

    ExternalIssue first = externalIssues.get(0);
    assertThat(first.primaryLocation().inputComponent().key()).isEqualTo("externalreport-project:main.kt");
    assertThat(first.ruleKey().rule()).isEqualTo("detekt.UnknownRuleKey");
    assertThat(first.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(first.severity()).isEqualTo(Severity.MAJOR);
    assertThat(first.primaryLocation().message()).isEqualTo("Error at file level with an unknown rule key.");
    assertThat(first.primaryLocation().textRange()).isNull();

    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.WARN)).containsExactlyInAnyOrder(
      "No input file found for not-existing-file.kt. No test-linter issues will be imported on this file.");
    assertThat(logTester.logs(LoggerLevel.DEBUG)).containsExactlyInAnyOrder(
      "Unexpected error without any message for rule: 'detekt.EmptyIfBlock'");
  }

  private List<ExternalIssue> importIssues(String fileName) throws IOException {
    SensorContextTester context = createContext();
    CheckstyleFormatImporter importer = new CheckstyleFormatImporter(context, LINTER_KEY);
    importer.importFile(PROJECT_DIR.resolve(fileName).toAbsolutePath().toFile());
    return new ArrayList<>(context.allExternalIssues());
  }

  static SensorContextTester createContext() throws IOException {
    SensorContextTester context = SensorContextTester.create(PROJECT_DIR);
    Files.list(PROJECT_DIR)
      .filter(file -> !Files.isDirectory(file))
      .forEach(file -> addFileToContext(context, PROJECT_DIR, file));
    return context;
  }

  private static void addFileToContext(SensorContextTester context, Path projectDir, Path file) {
    try {
      String projectId = projectDir.getFileName().toString() + "-project";
      context.fileSystem().add(TestInputFileBuilder.create(projectId, projectDir.toFile(), file.toFile())
        .setCharset(UTF_8)
        .setLanguage(file.toString().substring(file.toString().lastIndexOf('.') + 1))
        .setContents(new String(Files.readAllBytes(file), UTF_8))
        .setType(InputFile.Type.MAIN)
        .build());
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  static String onlyOneLogElement(List<String> elements) {
    assertThat(elements).hasSize(1);
    return elements.get(0);
  }

}
