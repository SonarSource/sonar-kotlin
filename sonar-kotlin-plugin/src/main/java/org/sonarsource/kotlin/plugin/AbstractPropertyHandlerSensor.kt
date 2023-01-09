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
package org.sonarsource.kotlin.plugin

import org.sonar.api.batch.sensor.Sensor
import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.batch.sensor.SensorDescriptor
import org.sonar.api.config.Configuration
import org.sonar.api.notifications.AnalysisWarnings
import org.sonar.api.utils.log.Loggers
import org.sonarsource.analyzer.commons.ExternalReportProvider
import java.io.File

private val LOG = Loggers.get(AbstractPropertyHandlerSensor::class.java)

abstract class AbstractPropertyHandlerSensor protected constructor(
    private val analysisWarnings: AnalysisWarnings,
    private val propertyKey: String,
    private val propertyName: String,
    private val configurationKey: String,
    private val languageKey: String,
) : Sensor {
    override fun describe(descriptor: SensorDescriptor) {
        descriptor
            .onlyOnLanguage(languageKey)
            .onlyWhenConfiguration { conf: Configuration -> conf.hasKey(configurationKey) }
            .name("Import of $propertyName issues")
    }

    override fun execute(context: SensorContext) {
        executeOnFiles(reportFiles(context), reportConsumer(context))
    }

    abstract fun reportConsumer(context: SensorContext): (File) -> Unit

    private fun executeOnFiles(reportFiles: List<File>, action: (File) -> Unit) {
        reportFiles
            .filter { obj: File -> obj.exists() }
            .forEach { file: File ->
                LOG.info("Importing {}", file)
                action(file)
            }
        reportMissingFiles(reportFiles)
    }

    private fun reportFiles(context: SensorContext): List<File> {
        return ExternalReportProvider.getReportFiles(context, configurationKey)
    }

    private fun reportMissingFiles(reportFiles: List<File>) {
        val missingFiles = reportFiles
            .filter { file: File -> !file.exists() }
            .map { obj: File -> obj.path }

        if (missingFiles.isNotEmpty()) {
            val missingFilesAsString = missingFiles.joinToString(separator = "\n- ", prefix = "\n- ")

            val logWarning = """Unable to import $propertyName report file(s):$missingFilesAsString
                |The report file(s) can not be found. Check that the property '$configurationKey' is correctly configured.""".trimMargin()
            LOG.warn(logWarning)

            val uiWarning = """Unable to import ${missingFiles.size} $propertyName report file(s).
                |Please check that property '$configurationKey' is correctly configured and the analysis logs for more details."""
                .trimMargin()
            analysisWarnings.addUnique(uiWarning)
        }
    }
}
