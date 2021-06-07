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
package org.sonarsource.kotlin.externalreport.androidlint

import org.assertj.core.api.Assertions
import org.junit.Rule
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport
import org.sonar.api.batch.rule.Severity
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor
import org.sonar.api.batch.sensor.issue.ExternalIssue
import org.sonar.api.config.internal.MapSettings
import org.sonar.api.rules.RuleType
import org.sonar.api.utils.log.LoggerLevel
import org.sonar.api.utils.log.ThreadLocalLogTester
import org.sonarsource.kotlin.externalreport.ExternalReportTestUtils
import java.io.IOException
import java.nio.file.Paths

private val PROJECT_DIR = Paths.get("src", "test", "resources", "externalreport", "androidlint")

@EnableRuleMigrationSupport
internal class AndroidLintSensorTest {
    
    private val analysisWarnings: MutableList<String> = ArrayList()
    
    @BeforeEach
    fun setup() {
        analysisWarnings.clear()
    }
    
    val logTester = ThreadLocalLogTester()
        @Rule get

    @Test
    fun test_descriptor() {
        val sensorDescriptor = DefaultSensorDescriptor()
        AndroidLintSensor { e: String -> analysisWarnings.add(e) }.describe(sensorDescriptor)
        Assertions.assertThat(sensorDescriptor.name()).isEqualTo("Import of Android Lint issues")
        Assertions.assertThat(sensorDescriptor.languages()).isEmpty()
        ExternalReportTestUtils.assertNoErrorWarnDebugLogs(logTester)
    }

    @Test
    @Throws(IOException::class)
    fun issues_with_sonarqube() {
        val externalIssues = executeSensorImporting("lint-results.xml")
        Assertions.assertThat(externalIssues).hasSize(4)
        val first = externalIssues[0]
        Assertions.assertThat(first.primaryLocation().inputComponent().key())
            .isEqualTo("androidlint-project:AndroidManifest.xml")
        Assertions.assertThat(first.ruleKey()).hasToString("external_android-lint:AllowBackup")
        Assertions.assertThat(first.type()).isEqualTo(RuleType.CODE_SMELL)
        Assertions.assertThat(first.severity()).isEqualTo(Severity.MINOR)
        Assertions.assertThat(first.primaryLocation().message()).isEqualTo(
            "On SDK version 23 and up, your app data will be automatically backed up and restored on app install. Consider adding the attribute `android:fullBackupContent` to specify an `@xml` resource which configures which files to backup. More info: https://developer.android.com/training/backup/autosyncapi.html")
        Assertions.assertThat(first.primaryLocation().textRange()!!.start().line()).isEqualTo(2)
        val second = externalIssues[1]
        Assertions.assertThat(second.primaryLocation().inputComponent().key()).isEqualTo("androidlint-project:A.java")
        Assertions.assertThat(second.ruleKey()).hasToString("external_android-lint:GoogleAppIndexingWarning")
        Assertions.assertThat(second.primaryLocation().textRange()!!.start().line()).isEqualTo(1)
        val third = externalIssues[2]
        Assertions.assertThat(third.primaryLocation().inputComponent().key()).isEqualTo("androidlint-project:B.kt")
        Assertions.assertThat(third.ruleKey()).hasToString("external_android-lint:GoogleAppIndexingWarning")
        Assertions.assertThat(third.primaryLocation().textRange()!!.start().line()).isEqualTo(2)
        val fourth = externalIssues[3]
        Assertions.assertThat(fourth.primaryLocation().inputComponent().key())
            .isEqualTo("androidlint-project:build.gradle")
        Assertions.assertThat(fourth.ruleKey()).hasToString("external_android-lint:GradleDependency")
        Assertions.assertThat(fourth.primaryLocation().textRange()!!.start().line()).isEqualTo(3)
        ExternalReportTestUtils.assertNoErrorWarnDebugLogs(logTester)
    }

    @Test
    @Throws(IOException::class)
    fun no_issues_without_report_paths_property() {
        val externalIssues = executeSensorImporting(null)
        Assertions.assertThat(externalIssues).isEmpty()
        ExternalReportTestUtils.assertNoErrorWarnDebugLogs(logTester)
    }

    @Test
    @Throws(IOException::class)
    fun no_issues_with_invalid_report_path() {
        val externalIssues = executeSensorImporting("invalid-path.txt")
        Assertions.assertThat(externalIssues).isEmpty()
        Assertions.assertThat(ExternalReportTestUtils.onlyOneLogElement(logTester.logs(LoggerLevel.WARN)))
            .startsWith("Unable to import Android Lint report file(s):")
            .contains("invalid-path.txt")
            .endsWith("The report file(s) can not be found. Check that the property 'sonar.androidLint.reportPaths' is correctly configured.")
        Assertions.assertThat(analysisWarnings).hasSize(1)
        Assertions.assertThat(analysisWarnings[0])
            .startsWith("Unable to import 1 Android Lint report file(s).")
            .endsWith("Please check that property 'sonar.androidLint.reportPaths' is correctly configured and the analysis logs for more details.")
    }

    @Test
    @Throws(IOException::class)
    fun no_issues_with_invalid_checkstyle_file() {
        val externalIssues = executeSensorImporting("not-android-lint-file.xml")
        Assertions.assertThat(externalIssues).isEmpty()
        Assertions.assertThat(ExternalReportTestUtils.onlyOneLogElement(logTester.logs(LoggerLevel.ERROR)))
            .startsWith("No issues information will be saved as the report file '")
            .endsWith("not-android-lint-file.xml' can't be read.")
    }

    @Test
    @Throws(IOException::class)
    fun no_issues_with_invalid_xml_report() {
        val externalIssues = executeSensorImporting("invalid-file.xml")
        Assertions.assertThat(externalIssues).isEmpty()
        Assertions.assertThat(ExternalReportTestUtils.onlyOneLogElement(logTester.logs(LoggerLevel.ERROR)))
            .startsWith("No issues information will be saved as the report file '")
            .endsWith("invalid-file.xml' can't be read.")
    }

    @Test
    @Throws(IOException::class)
    fun issues_when_xml_file_has_errors() {
        val externalIssues = executeSensorImporting("lint-results-with-errors.xml")
        Assertions.assertThat(externalIssues).hasSize(1)
        val first = externalIssues[0]
        Assertions.assertThat(first.primaryLocation().inputComponent().key())
            .isEqualTo("androidlint-project:AndroidManifest.xml")
        Assertions.assertThat(first.ruleKey()).hasToString("external_android-lint:UnknownRuleKey")
        Assertions.assertThat(first.type()).isEqualTo(RuleType.CODE_SMELL)
        Assertions.assertThat(first.severity()).isEqualTo(Severity.MAJOR)
        Assertions.assertThat(first.primaryLocation().message()).isEqualTo("Unknown rule.")
        Assertions.assertThat(first.primaryLocation().textRange()).isNull()
        Assertions.assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty()
        Assertions.assertThat(logTester.logs(LoggerLevel.WARN)).containsExactlyInAnyOrder(
            "No input file found for unknown-file.xml. No android lint issues will be imported on this file.")
        Assertions.assertThat(logTester.logs(LoggerLevel.DEBUG)).containsExactlyInAnyOrder(
            "Missing information or unsupported file type for id:'', file:'AndroidManifest.xml', message:'Missing rule key.'",
            "Missing information or unsupported file type for id:'UnusedAttribute', file:'binary-file.gif', message:'Valid rule key with binary file.'",
            "Missing information or unsupported file type for id:'UnusedAttribute', file:'', message:'Valid rule key without file path.'",
            "Missing information or unsupported file type for id:'UnusedAttribute', file:'', message:'Valid rule key with invalid location.'",
            "Missing information or unsupported file type for id:'', file:'', message:''")
    }

    @Throws(IOException::class)
    private fun executeSensorImporting(fileName: String?): List<ExternalIssue> {
        
        val context = ExternalReportTestUtils.createContext(PROJECT_DIR)
        if (fileName != null) {
            val settings = MapSettings()
            val path = PROJECT_DIR.resolve(fileName).toAbsolutePath().toString()
            settings.setProperty("sonar.androidLint.reportPaths", path)
            
            context.setSettings(settings)
        }
        AndroidLintSensor { e: String -> analysisWarnings.add(e) }.execute(context)
        return ArrayList(context.allExternalIssues())
    }
}
