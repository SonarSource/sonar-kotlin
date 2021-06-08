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
package org.sonarsource.kotlin.externalreport.detekt

import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.notifications.AnalysisWarnings
import org.sonar.api.rule.RuleKey
import org.sonar.api.utils.log.Loggers
import org.sonarsource.kotlin.externalreport.ExternalReporting
import org.sonarsource.kotlin.plugin.KotlinPlugin
import org.sonarsource.slang.externalreport.CheckstyleFormatImporterWithRuleLoader
import org.sonarsource.slang.plugin.AbstractPropertyHandlerSensor
import java.io.File
import java.util.function.Consumer

class DetektSensor(analysisWarnings: AnalysisWarnings) : AbstractPropertyHandlerSensor(
    analysisWarnings,
    LINTER_KEY,
    LINTER_NAME,
    REPORT_PROPERTY_KEY,
    KotlinPlugin.KOTLIN_LANGUAGE_KEY,
) {
    companion object {
        private val LOG = Loggers.get(DetektSensor::class.java)
        const val LINTER_KEY = "detekt"
        const val LINTER_NAME = "detekt"
        private const val DETEKT_PREFIX = "detekt."
        const val REPORT_PROPERTY_KEY = "sonar.kotlin.detekt.reportPaths"

        private class ReportImporter(
            context: SensorContext,
        ) : CheckstyleFormatImporterWithRuleLoader(context, LINTER_KEY, DetektRulesDefinition.RULE_LOADER) {

            override fun createRuleKey(source: String): RuleKey? {
                val preliminaryRuleKey =
                    if (source.startsWith(DETEKT_PREFIX)) {
                        source.substring(DETEKT_PREFIX.length)
                    } else {
                        LOG.debug("Unexpected rule key without '{}' suffix: '{}'", DETEKT_PREFIX, source)
                        return null
                    }

                val ruleKey =
                    if (DetektRulesDefinition.RULE_LOADER.ruleKeys().contains(preliminaryRuleKey)) preliminaryRuleKey
                    else ExternalReporting.FALLBACK_RULE_KEY

                return super.createRuleKey(ruleKey)
            }
        }
    }

    override fun reportConsumer(context: SensorContext): Consumer<File> {
        return Consumer { reportPath: File -> ReportImporter(context).importFile(reportPath) }
    }
}
