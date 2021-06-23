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
package org.sonarsource.kotlin.externalreport.detekt;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.ExternalIssue;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.api.utils.log.ThreadLocalLogTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarsource.kotlin.externalreport.ExternalReportTestUtils.assertNoErrorWarnDebugLogs;
import static org.sonarsource.kotlin.externalreport.ExternalReportTestUtils.createContext;
import static org.sonarsource.kotlin.externalreport.ExternalReportTestUtils.onlyOneLogElement;

@EnableRuleMigrationSupport
class DetektSensorTest {

  private static final Path PROJECT_DIR = Paths.get("src", "test", "resources", "externalreport", "detekt");

  private final List<String> analysisWarnings = new ArrayList<>();

  @Rule
  public ThreadLocalLogTester logTester = new ThreadLocalLogTester();

  @BeforeEach
  void setup() {
    analysisWarnings.clear();
  }

  @Test
  void test_descriptor() {
    DefaultSensorDescriptor sensorDescriptor = new DefaultSensorDescriptor();
    DetektSensor detektSensor = new DetektSensor(analysisWarnings::add);
    detektSensor.describe(sensorDescriptor);
    assertThat(sensorDescriptor.name()).isEqualTo("Import of detekt issues");
    assertThat(sensorDescriptor.languages()).containsOnly("kotlin");
    assertNoErrorWarnDebugLogs(logTester);
  }

  @Test
  void issues_with_sonarqube() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting("detekt-checkstyle.xml");
    assertThat(externalIssues).hasSize(3);

    ExternalIssue first = externalIssues.get(0);
    assertThat(first.primaryLocation().inputComponent().key()).isEqualTo("detekt-project:main.kt");
    assertThat(first.ruleKey().rule()).isEqualTo("EmptyIfBlock");
    assertThat(first.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(first.severity()).isEqualTo(Severity.MINOR);
    assertThat(first.primaryLocation().message()).isEqualTo("This empty block of code can be removed.");
    assertThat(first.primaryLocation().textRange().start().line()).isEqualTo(3);

    ExternalIssue second = externalIssues.get(1);
    assertThat(second.primaryLocation().inputComponent().key()).isEqualTo("detekt-project:main.kt");
    assertThat(second.ruleKey().rule()).isEqualTo("MagicNumber");
    assertThat(second.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(second.severity()).isEqualTo(Severity.INFO);
    assertThat(second.remediationEffort().longValue()).isEqualTo(10L);
    assertThat(second.primaryLocation().message()).isEqualTo("This expression contains a magic number. Consider defining it to a well named constant.");
    assertThat(second.primaryLocation().textRange().start().line()).isEqualTo(3);

    ExternalIssue third = externalIssues.get(2);
    assertThat(third.primaryLocation().inputComponent().key()).isEqualTo("detekt-project:A.kt");
    assertThat(third.ruleKey().rule()).isEqualTo("EqualsWithHashCodeExist");
    assertThat(third.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(third.severity()).isEqualTo(Severity.CRITICAL);
    assertThat(third.primaryLocation().message()).isEqualTo("A class should always override hashCode when overriding equals and the other way around.");
    assertThat(third.primaryLocation().textRange().start().line()).isEqualTo(3);

    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
  }

  @Test
  void no_issues_without_report_paths_property() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(null);
    assertThat(externalIssues).isEmpty();
    assertNoErrorWarnDebugLogs(logTester);
  }

  @Test
  void invalid_report_path_triggers_warnings_in_SQ_UI_and_error_log() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting( "invalid-path.txt");
    assertThat(externalIssues).isEmpty();
    List<String> warnings = logTester.logs(LoggerLevel.WARN);
    assertThat(warnings)
      .hasSize(1)
      .hasSameSizeAs(analysisWarnings);
    assertThat(warnings.get(0))
      .startsWith("Unable to import detekt report file(s):")
      .contains("invalid-path.txt")
      .endsWith("The report file(s) can not be found. Check that the property 'sonar.kotlin.detekt.reportPaths' is correctly configured.");
    assertThat(analysisWarnings.get(0))
      .startsWith("Unable to import 1 detekt report file(s).")
      .endsWith("Please check that property 'sonar.kotlin.detekt.reportPaths' is correctly configured and the analysis logs for more details.");
  }

  @Test
  void multiple_missing_files_are_reported_in_SQ_UI() throws Exception {
    SensorContextTester context = createContext(PROJECT_DIR);
    String validFile = PROJECT_DIR.resolve("detekt-checkstyle.xml").toAbsolutePath().toString();
    context.settings().setProperty("sonar.kotlin.detekt.reportPaths", "invalid1.xml," + validFile + ",invalid2.txt");
    DetektSensor detektSensor = new DetektSensor(analysisWarnings::add);
    detektSensor.execute(context);
    Collection<ExternalIssue> externalIssues = context.allExternalIssues();

    assertThat(externalIssues).hasSize(3);
    assertThat(logTester.logs(LoggerLevel.INFO))
      .hasSize(1)
      .allMatch(info -> info.startsWith("Importing") && info.endsWith("detekt-checkstyle.xml"));

    List<String> warnings = logTester.logs(LoggerLevel.WARN);
    assertThat(warnings)
      .hasSize(1)
      .hasSameSizeAs(analysisWarnings);
    assertThat(warnings.get(0))
      .startsWith("Unable to import detekt report file(s):")
      .contains("invalid1.xml")
      .contains("invalid2.txt")
      .endsWith("The report file(s) can not be found. Check that the property 'sonar.kotlin.detekt.reportPaths' is correctly configured.");
    assertThat(analysisWarnings.get(0))
      .startsWith("Unable to import 2 detekt report file(s).")
      .endsWith("Please check that property 'sonar.kotlin.detekt.reportPaths' is correctly configured and the analysis logs for more details.");
  }

  @Test
  void no_issues_with_invalid_checkstyle_file() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting("not-checkstyle-file.xml");
    assertThat(externalIssues).isEmpty();
    assertThat(onlyOneLogElement(logTester.logs(LoggerLevel.ERROR)))
      .startsWith("No issue information will be saved as the report file '")
      .endsWith("not-checkstyle-file.xml' can't be read.");
  }

  @Test
  void no_issues_with_invalid_xml_report() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting("invalid-file.xml");
    assertThat(externalIssues).isEmpty();
    assertThat(onlyOneLogElement(logTester.logs(LoggerLevel.ERROR)))
      .startsWith("No issue information will be saved as the report file '")
      .endsWith("invalid-file.xml' can't be read.");
  }

  @Test
  void issues_when_xml_file_has_errors() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting("detekt-checkstyle-with-errors.xml");
    assertThat(externalIssues).hasSize(1);

    ExternalIssue first = externalIssues.get(0);
    assertThat(first.primaryLocation().inputComponent().key()).isEqualTo("detekt-project:main.kt");
    assertThat(first.ruleKey().rule()).isEqualTo("UnknownRuleKey");
    assertThat(first.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(first.severity()).isEqualTo(Severity.MAJOR);
    assertThat(first.primaryLocation().message()).isEqualTo("Error at file level with an unknown rule key.");
    assertThat(first.primaryLocation().textRange()).isNull();

    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.WARN)).containsExactlyInAnyOrder(
      "No input file found for not-existing-file.kt. No detekt issues will be imported on this file.");
    assertThat(logTester.logs(LoggerLevel.DEBUG)).containsExactlyInAnyOrder(
      "Unexpected error without any message for rule: 'detekt.EmptyIfBlock'",
      "Unexpected rule key without 'detekt.' suffix: 'invalid-format'");
  }

  private List<ExternalIssue> executeSensorImporting(@Nullable String fileName) throws IOException {
    SensorContextTester context = createContext(PROJECT_DIR);
    if (fileName != null) {
      String path = PROJECT_DIR.resolve(fileName).toAbsolutePath().toString();
      context.settings().setProperty("sonar.kotlin.detekt.reportPaths", path);
    }
    DetektSensor detektSensor = new DetektSensor(analysisWarnings::add);
    detektSensor.execute(context);
    return new ArrayList<>(context.allExternalIssues());
  }

}
