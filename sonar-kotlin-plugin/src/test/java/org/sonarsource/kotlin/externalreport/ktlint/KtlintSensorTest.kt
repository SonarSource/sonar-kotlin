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
package org.sonarsource.kotlin.externalreport.ktlint

import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.sonar.api.batch.rule.Severity
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor
import org.sonar.api.batch.sensor.issue.ExternalIssue
import org.sonar.api.rules.RuleType
import org.sonar.api.utils.log.LoggerLevel
import org.sonar.api.utils.log.ThreadLocalLogTester
import org.sonarsource.kotlin.externalreport.ExternalReportTestUtils
import java.nio.file.Paths

private val PROJECT_DIR = Paths.get("src", "test", "resources", "externalreport", "ktlint")

@EnableRuleMigrationSupport
class KtlintSensorTest {

    val logTester = ThreadLocalLogTester()
        @Rule get

    private val analysisWarnings = mutableListOf<String>()

    @BeforeEach
    fun setup() {
        analysisWarnings.clear()
    }

    @Test
    fun `test descriptor`() {
        val sensorDescriptor = DefaultSensorDescriptor()
        val ktlintSensor = KtlintSensor { e: String ->
            analysisWarnings.add(e)
        }
        ktlintSensor.describe(sensorDescriptor)
        assertThat(sensorDescriptor.name()).isEqualTo("Import of ktlint issues")
        assertThat(sensorDescriptor.languages()).containsOnly("kotlin")
        ExternalReportTestUtils.assertNoErrorWarnDebugLogs(logTester)
    }

    @ParameterizedTest
    @CsvSource("foo-report.xml", "foo-report.json")
    fun `issues with sonarqube`(reportFile: String) {
        val externalIssues = executeSensorImporting(reportFile)
        assertThat(externalIssues).hasSize(4)

        val first = externalIssues[0]
        assertThat(first.primaryLocation().inputComponent().key()).isEqualTo("ktlint-project:Foo.kt")
        assertThat(first.ruleKey().rule()).isEqualTo("no-wildcard-imports")
        assertThat(first.type()).isEqualTo(RuleType.CODE_SMELL)
        assertThat(first.severity()).isEqualTo(Severity.MAJOR)
        assertThat(first.primaryLocation().message()).isEqualTo("Wildcard import (cannot be auto-corrected)")
        assertThat(first.primaryLocation().textRange()!!.start().line()).isEqualTo(3)

        val second = externalIssues[1]
        assertThat(second.primaryLocation().inputComponent().key()).isEqualTo("ktlint-project:Foo.kt")
        assertThat(second.ruleKey().rule()).isEqualTo("experimental:no-empty-first-line-in-method-block")
        assertThat(second.type()).isEqualTo(RuleType.CODE_SMELL)
        assertThat(second.severity()).isEqualTo(Severity.MAJOR)
        assertThat(second.remediationEffort()!!.toLong()).isEqualTo(5L)
        assertThat(second.primaryLocation().message()).isEqualTo("First line in a method block should not be empty")
        assertThat(second.primaryLocation().textRange()!!.start().line()).isEqualTo(7)

        val third = externalIssues[2]
        assertThat(third.primaryLocation().inputComponent().key()).isEqualTo("ktlint-project:Foo.kt")
        assertThat(third.ruleKey().rule()).isEqualTo("no-semi")
        assertThat(third.type()).isEqualTo(RuleType.CODE_SMELL)
        assertThat(third.severity()).isEqualTo(Severity.MAJOR)
        assertThat(third.primaryLocation().message()).isEqualTo("Unnecessary semicolon")
        assertThat(third.primaryLocation().textRange()!!.start().line()).isEqualTo(8)

        val fourth = externalIssues[3]
        assertThat(fourth.primaryLocation().inputComponent().key()).isEqualTo("ktlint-project:Foo.kt")
        assertThat(fourth.ruleKey().rule()).isEqualTo("no-blank-line-before-rbrace")
        assertThat(fourth.type()).isEqualTo(RuleType.CODE_SMELL)
        assertThat(fourth.severity()).isEqualTo(Severity.MAJOR)
        assertThat(fourth.primaryLocation().message()).isEqualTo("""Unexpected blank line(s) before "}"""")
        assertThat(fourth.primaryLocation().textRange()!!.start().line()).isEqualTo(10)

        assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty()
    }

    @Test
    fun `no issues without report paths property`() {
        val externalIssues = executeSensorImporting(null)
        assertThat(externalIssues).isEmpty()
        ExternalReportTestUtils.assertNoErrorWarnDebugLogs(logTester)
    }

    @Test
    fun `invalid report path triggers warnings in SQ UI and error log`() {
        val externalIssues = executeSensorImporting("invalid-path.txt")
        assertThat(externalIssues).isEmpty()
        val warnings = logTester.logs(LoggerLevel.WARN)
        assertThat(warnings)
            .hasSize(1)
            .hasSameSizeAs(analysisWarnings)
        assertThat(warnings[0])
            .startsWith("Unable to import ktlint report file(s):")
            .contains("invalid-path.txt")
            .endsWith("The report file(s) can not be found. Check that the property 'sonar.kotlin.ktlint.reportPaths' is correctly configured.")
        assertThat(analysisWarnings[0])
            .startsWith("Unable to import 1 ktlint report file(s).")
            .endsWith("Please check that property 'sonar.kotlin.ktlint.reportPaths' is correctly configured and the analysis logs for more details.")
    }

    @Test
    fun `multiple missing files are reported in SQ UI`() {
        val context = ExternalReportTestUtils.createContext(PROJECT_DIR)
        val validFile = PROJECT_DIR.resolve("foo-report.xml").toAbsolutePath().toString()
        context.settings().setProperty("sonar.kotlin.ktlint.reportPaths", "invalid1.xml,$validFile,invalid2.txt")
        val ktlintSensor = KtlintSensor { e: String ->
            analysisWarnings.add(e)
        }
        ktlintSensor.execute(context)
        val externalIssues = context.allExternalIssues()
        assertThat(externalIssues).hasSize(4)
        assertThat(logTester.logs(LoggerLevel.INFO))
            .hasSize(1)
            .allMatch { info: String -> info.startsWith("Importing") && info.endsWith("foo-report.xml") }
        val warnings = logTester.logs(LoggerLevel.WARN)
        assertThat(warnings)
            .hasSize(1)
            .hasSameSizeAs(analysisWarnings)
        assertThat(warnings[0])
            .startsWith("Unable to import ktlint report file(s):")
            .contains("invalid1.xml")
            .contains("invalid2.txt")
            .endsWith("The report file(s) can not be found. Check that the property 'sonar.kotlin.ktlint.reportPaths' is correctly configured.")
        assertThat(analysisWarnings[0])
            .startsWith("Unable to import 2 ktlint report file(s).")
            .endsWith("Please check that property 'sonar.kotlin.ktlint.reportPaths' is correctly configured and the analysis logs for more details.")
    }

    @Test
    fun `no issues with invalid checkstyle file`() {
        val externalIssues = executeSensorImporting("invalid-checkstyle-file.xml")
        assertThat(externalIssues).isEmpty()
        assertThat(ExternalReportTestUtils.onlyOneLogElement(logTester.logs(LoggerLevel.ERROR)))
            .startsWith("No issue information will be saved as the report file '")
            .endsWith("invalid-checkstyle-file.xml' can't be read.")
    }

    @Test
    fun `no issues with invalid JSON file`() {
        val externalIssues = executeSensorImporting("invalid-json-file.json")
        assertThat(externalIssues).isEmpty()
        assertThat(ExternalReportTestUtils.onlyOneLogElement(logTester.logs(LoggerLevel.ERROR)))
            .startsWith("No issue information will be saved as the report file '")
            .endsWith("invalid-json-file.json' cannot be read. Could not parse list of files with reported errors. Expected JSON array.")
    }

    @Test
    fun `no issues with invalid JSON format`() {
        val externalIssues = executeSensorImporting("invalid-json-format.json")
        assertThat(externalIssues).isEmpty()
        assertThat(ExternalReportTestUtils.onlyOneLogElement(logTester.logs(LoggerLevel.ERROR)))
            .startsWith("No issue information will be saved as the report file '")
            .endsWith("invalid-json-format.json' cannot be read. JSON parsing failed: ParseException (Expected 'a' at 1:2)")
    }

    @Test
    fun `no issues with invalid file extension`() {
        val externalIssues = executeSensorImporting("invalid-report-format.bin")
        assertThat(externalIssues).isEmpty()
        assertThat(ExternalReportTestUtils.onlyOneLogElement(logTester.logs(LoggerLevel.ERROR)))
            .startsWith("The ktlint report file '")
            .endsWith("invalid-report-format.bin' has an unsupported extension/format. Expected 'json' or 'xml', got 'bin'.")
    }

    @Test
    fun `no issues with invalid xml report`() {
        val externalIssues = executeSensorImporting("invalid-file.xml")
        assertThat(externalIssues).isEmpty()
        assertThat(ExternalReportTestUtils.onlyOneLogElement(logTester.logs(LoggerLevel.ERROR)))
            .startsWith("No issue information will be saved as the report file '")
            .endsWith("invalid-file.xml' can't be read.")
    }

    @Test
    fun `issues when checkstyle report file has errors`() {
        val externalIssues = executeSensorImporting("foo-report-with-errors.xml")
        assertThat(externalIssues).hasSize(4)
        assertThat(logTester.logs(LoggerLevel.WARN)).containsExactlyInAnyOrder(
            "No input file found for non-existent-file.kt. No ktlint issues will be imported on this file.")
        assertThat(logTester.logs(LoggerLevel.DEBUG)).containsExactlyInAnyOrder(
            "Unexpected error without any message for rule: ''",
            "Unexpected error without any message for rule: 'some-rule-key'"
        )
    }

    @Test
    fun `issues when JSON report file has errors`() {
        val externalIssues = executeSensorImporting("foo-report-with-errors.json")
        assertThat(externalIssues).hasSize(4)
        assertThat(logTester.logs(LoggerLevel.WARN)).containsExactlyInAnyOrder(
            "Exception while trying to parse ktlint JSON report: Invalid entry for file name 0.",
            "Exception while trying to parse ktlint JSON report: Could not parse valid list of errors for entry 1 (file Foo.kt)",
            "Exception while trying to parse ktlint JSON report: Not all ktlint errors were parsed correctly for file 'Foo.kt'.",
            "Invalid input file non-existent-file.kt",
            "Exception while trying to parse ktlint JSON report: Could not parse entry 4. Expected JSON object."
        )
        assertThat(logTester.logs(LoggerLevel.DEBUG)).containsExactlyInAnyOrder(
            "Could not parse error 4 of the entry of file 'Foo.kt'."
        )
    }

    private fun executeSensorImporting(fileName: String?): List<ExternalIssue> {
        val context = ExternalReportTestUtils.createContext(PROJECT_DIR).apply {
            if (fileName != null) {
                val path = PROJECT_DIR.resolve(fileName).toAbsolutePath().toString()
                settings().setProperty("sonar.kotlin.ktlint.reportPaths", path)
            }
        }

        KtlintSensor { e: String -> analysisWarnings.add(e) }
            .apply { execute(context) }

        return ArrayList(context.allExternalIssues())
    }
}
