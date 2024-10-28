/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2024 SonarSource SA
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

import org.slf4j.LoggerFactory
import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.notifications.AnalysisWarnings
import org.sonar.api.rule.RuleKey
import org.sonarsource.kotlin.api.frontend.AbstractPropertyHandlerSensor
import org.sonarsource.kotlin.api.common.RULE_REPOSITORY_LANGUAGE
import org.sonarsource.kotlin.externalreport.common.CheckstyleFormatImporterWithRuleLoader
import org.sonarsource.kotlin.externalreport.common.FALLBACK_RULE_KEY
import java.io.File

class DetektSensor(analysisWarnings: AnalysisWarnings) : AbstractPropertyHandlerSensor(
    analysisWarnings,
    LINTER_KEY,
    LINTER_NAME,
    REPORT_PROPERTY_KEY,
    RULE_REPOSITORY_LANGUAGE,
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(DetektSensor::class.java)
        const val LINTER_KEY = "detekt"
        const val LINTER_NAME = "detekt"
        private const val DETEKT_PREFIX = "detekt."
        const val REPORT_PROPERTY_KEY = "sonar.kotlin.detekt.reportPaths"

        private class ReportImporter(
            context: SensorContext,
        ) : CheckstyleFormatImporterWithRuleLoader(context, LINTER_KEY, DetektRulesDefinition.RULE_LOADER) {

            override fun createRuleKey(source: String): RuleKey? {
                if (!source.startsWith(DETEKT_PREFIX)) {
                    LOG.debug("Unexpected rule key without '{}' suffix: '{}'", DETEKT_PREFIX, source)
                    return null
                }
                return super.createRuleKey(source.substring(DETEKT_PREFIX.length))
            }
        }
    }

    override fun reportConsumer(context: SensorContext) = { reportPath: File -> ReportImporter(context).importFile(reportPath) }
}
