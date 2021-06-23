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
package org.sonarsource.kotlin.externalreport.androidlint;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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
class AndroidLintSensorTest {

  private static final Path PROJECT_DIR = Paths.get("src", "test", "resources", "externalreport", "androidlint");

  private final List<String> analysisWarnings = new ArrayList<>();

  @BeforeEach
  void setup() {
    analysisWarnings.clear();
  }

  @Rule
  public ThreadLocalLogTester logTester = new ThreadLocalLogTester();

  @Test
  void test_descriptor() {
    DefaultSensorDescriptor sensorDescriptor = new DefaultSensorDescriptor();
    new AndroidLintSensor(analysisWarnings::add).describe(sensorDescriptor);
    assertThat(sensorDescriptor.name()).isEqualTo("Import of Android Lint issues");
    assertThat(sensorDescriptor.languages()).isEmpty();
    assertNoErrorWarnDebugLogs(logTester);
  }

  @Test
  void issues_with_sonarqube() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting("lint-results.xml");
    assertThat(externalIssues).hasSize(4);

    ExternalIssue first = externalIssues.get(0);
    assertThat(first.primaryLocation().inputComponent().key()).isEqualTo("androidlint-project:AndroidManifest.xml");
    assertThat(first.ruleKey()).hasToString("external_android-lint:AllowBackup");
    assertThat(first.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(first.severity()).isEqualTo(Severity.MINOR);
    assertThat(first.primaryLocation().message()).isEqualTo(
      "On SDK version 23 and up, your app data will be automatically backed up and restored on app install. Consider adding the attribute `android:fullBackupContent` to specify an `@xml` resource which configures which files to backup. More info: https://developer.android.com/training/backup/autosyncapi.html");
    assertThat(first.primaryLocation().textRange().start().line()).isEqualTo(2);

    ExternalIssue second = externalIssues.get(1);
    assertThat(second.primaryLocation().inputComponent().key()).isEqualTo("androidlint-project:A.java");
    assertThat(second.ruleKey()).hasToString("external_android-lint:GoogleAppIndexingWarning");
    assertThat(second.primaryLocation().textRange().start().line()).isEqualTo(1);

    ExternalIssue third = externalIssues.get(2);
    assertThat(third.primaryLocation().inputComponent().key()).isEqualTo("androidlint-project:B.kt");
    assertThat(third.ruleKey()).hasToString("external_android-lint:GoogleAppIndexingWarning");
    assertThat(third.primaryLocation().textRange().start().line()).isEqualTo(2);

    ExternalIssue fourth = externalIssues.get(3);
    assertThat(fourth.primaryLocation().inputComponent().key()).isEqualTo("androidlint-project:build.gradle");
    assertThat(fourth.ruleKey()).hasToString("external_android-lint:GradleDependency");
    assertThat(fourth.primaryLocation().textRange().start().line()).isEqualTo(3);

    assertNoErrorWarnDebugLogs(logTester);
  }

  @Test
  void no_issues_without_report_paths_property() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting(null);
    assertThat(externalIssues).isEmpty();
    assertNoErrorWarnDebugLogs(logTester);
  }

  @Test
  void no_issues_with_invalid_report_path() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting("invalid-path.txt");
    assertThat(externalIssues).isEmpty();
    assertThat(onlyOneLogElement(logTester.logs(LoggerLevel.WARN)))
      .startsWith("Unable to import Android Lint report file(s):")
      .contains("invalid-path.txt")
      .endsWith("The report file(s) can not be found. Check that the property 'sonar.androidLint.reportPaths' is correctly configured.");
    assertThat(analysisWarnings).hasSize(1);
    assertThat(analysisWarnings.get(0))
      .startsWith("Unable to import 1 Android Lint report file(s).")
      .endsWith("Please check that property 'sonar.androidLint.reportPaths' is correctly configured and the analysis logs for more details.");
  }

  @Test
  void no_issues_with_invalid_checkstyle_file() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting("not-android-lint-file.xml");
    assertThat(externalIssues).isEmpty();
    assertThat(onlyOneLogElement(logTester.logs(LoggerLevel.ERROR)))
      .startsWith("No issues information will be saved as the report file '")
      .endsWith("not-android-lint-file.xml' can't be read.");
  }

  @Test
  void no_issues_with_invalid_xml_report() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting("invalid-file.xml");
    assertThat(externalIssues).isEmpty();
    assertThat(onlyOneLogElement(logTester.logs(LoggerLevel.ERROR)))
      .startsWith("No issues information will be saved as the report file '")
      .endsWith("invalid-file.xml' can't be read.");
  }

  @Test
  void issues_when_xml_file_has_errors() throws IOException {
    List<ExternalIssue> externalIssues = executeSensorImporting("lint-results-with-errors.xml");
    assertThat(externalIssues).hasSize(1);

    ExternalIssue first = externalIssues.get(0);
    assertThat(first.primaryLocation().inputComponent().key()).isEqualTo("androidlint-project:AndroidManifest.xml");
    assertThat(first.ruleKey()).hasToString("external_android-lint:UnknownRuleKey");
    assertThat(first.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(first.severity()).isEqualTo(Severity.MAJOR);
    assertThat(first.primaryLocation().message()).isEqualTo("Unknown rule.");
    assertThat(first.primaryLocation().textRange()).isNull();

    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.WARN)).containsExactlyInAnyOrder(
      "No input file found for unknown-file.xml. No android lint issues will be imported on this file.");
    assertThat(logTester.logs(LoggerLevel.DEBUG)).containsExactlyInAnyOrder(
      "Missing information or unsupported file type for id:'', file:'AndroidManifest.xml', message:'Missing rule key.'",
      "Missing information or unsupported file type for id:'UnusedAttribute', file:'binary-file.gif', message:'Valid rule key with binary file.'",
      "Missing information or unsupported file type for id:'UnusedAttribute', file:'', message:'Valid rule key without file path.'",
      "Missing information or unsupported file type for id:'UnusedAttribute', file:'', message:'Valid rule key with invalid location.'",
      "Missing information or unsupported file type for id:'', file:'', message:''");
  }

  private List<ExternalIssue> executeSensorImporting(@Nullable String fileName) throws IOException {
    SensorContextTester context = createContext(PROJECT_DIR);
    if (fileName != null) {
      String path = PROJECT_DIR.resolve(fileName).toAbsolutePath().toString();
      context.settings().setProperty("sonar.androidLint.reportPaths", path);
    }
    new AndroidLintSensor(analysisWarnings::add).execute(context);
    return new ArrayList<>(context.allExternalIssues());
  }

}
