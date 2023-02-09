/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2023 SonarSource SA
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

import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.notifications.AnalysisWarnings
import org.sonar.api.utils.log.Loggers
import org.sonarsource.kotlin.externalreport.ExternalReporting
import org.sonarsource.kotlin.externalreport.ktlint.KtlintRulesDefinition.Companion.EXPERIMENTAL_RULE_PREFIX
import java.io.File

internal val LOG = Loggers.get(ReportImporter::class.java)

internal class ReportImporter(val analysisWarnings: AnalysisWarnings, val context: SensorContext) {
    fun importFile(reportFile: File) {
        when (reportFile.extension) {
            "json" -> importJsonFile(reportFile)
            "xml" -> CheckstyleReportParser(context).importFile(reportFile)
            else -> ("The ktlint report file '$reportFile' has an unsupported extension/format. " +
                "Expected 'json' or 'xml', got '${reportFile.extension}'.").let {
                LOG.error(it)
                analysisWarnings.addUnique(it)
            }
        }
    }

    fun importJsonFile(reportFile: File) {
        val parser = try {
            JsonReportParser(reportFile.toPath()).apply { parse() }
        } catch (e: InvalidReportFormatException) {
            LOG.error("No issue information will be saved as the report file '$reportFile' cannot be read. ${e.message}")
            return
        }
        parser.report.forEach { (filePath, linterFindings) -> importExternalIssues(filePath, linterFindings) }
    }

    private fun importExternalIssues(filePath: String, linterFindings: List<Finding>) {
        val predicates = context.fileSystem().predicates()
        val inputFile = context.fileSystem().inputFile(predicates.or(
            predicates.hasAbsolutePath(filePath),
            predicates.hasRelativePath(filePath)))
            ?: run {
                LOG.warn("Invalid input file $filePath")
                return
            }

        val ruleLoader = KtlintRulesDefinition.RULE_LOADER
        linterFindings.forEach { (line, _, message, preliminaryRuleKey) ->

            val truncatedPrelimRuleKey =
                if (preliminaryRuleKey.startsWith(EXPERIMENTAL_RULE_PREFIX)) preliminaryRuleKey.substring(EXPERIMENTAL_RULE_PREFIX.length)
                else preliminaryRuleKey

            val ruleKey =
                if (KtlintRulesDefinition.RULE_LOADER.ruleKeys().contains(truncatedPrelimRuleKey)) truncatedPrelimRuleKey
                else ExternalReporting.FALLBACK_RULE_KEY

            context.newExternalIssue().apply {
                type(ruleLoader.ruleType(ruleKey))
                severity(ruleLoader.ruleSeverity(ruleKey))
                remediationEffortMinutes(ruleLoader.ruleConstantDebtMinutes(ruleKey))
                at(
                    newLocation().message(message)
                        .on(inputFile)
                        .at(inputFile.selectLine(line))
                )
                engineId(KtlintSensor.LINTER_KEY)
                ruleId(ruleKey)
                save()
            }
        }
    }
}
