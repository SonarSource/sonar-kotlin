/*
 * SonarSource Kotlin
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonarsource.kotlin.plugin

import org.slf4j.LoggerFactory
import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.batch.sensor.SensorDescriptor
import org.sonar.api.scanner.sensor.ProjectSensor
import org.sonar.api.utils.Version
import org.sonarsource.kotlin.api.common.KOTLIN_LANGUAGE_KEY
import org.sonarsource.kotlin.metrics.TelemetryData
import java.io.IOException
import java.util.Properties

class KotlinProjectSensor(internal val telemetryData: TelemetryData) : ProjectSensor {
    private val LOG = LoggerFactory.getLogger(KotlinProjectSensor::class.java)

    companion object {
        private const val PLUGIN_VERSION_RESOURCE = "org/sonar/plugins/kotlin/pluginVersion.properties"

        fun resolvePluginVersion(): String = resolvePluginVersion(PLUGIN_VERSION_RESOURCE)

        internal fun resolvePluginVersion(resourceName: String): String {
            try {
                KotlinProjectSensor::class.java.classLoader.getResourceAsStream(resourceName)?.use { stream ->
                    val props = Properties()
                    props.load(stream)
                    return props.getProperty("plugin.version", "unknown")
                }
            } catch (e: IOException) {
                // fall through to fallback
            }
            return "unknown"
        }
    }

    override fun describe(descriptor: SensorDescriptor) {
        descriptor.onlyOnLanguage(KOTLIN_LANGUAGE_KEY).name("KotlinProjectSensor")
    }

    fun addAndLogTelemetryProperty(context: SensorContext, propetyName: String, propertyValue: String) {
        context.addTelemetryProperty(propetyName, propertyValue);
        LOG.debug("TELEMETRY: $propetyName=$propertyValue");
    }

    fun fileProcessingTelemetry(context: SensorContext) {
        // files - .kt and .kts counters
        addAndLogTelemetryProperty(context, "kotlin.files.processed", telemetryData.filesProcessed.toString())
        addAndLogTelemetryProperty(context, "kotlin.files.read.failures", telemetryData.readFailures.toString())
        addAndLogTelemetryProperty(context, "kotlin.files.parse.failures", telemetryData.parseFailures.toString())
        // scripts - .kts only
        addAndLogTelemetryProperty(context, "kotlin.scripts.processed", telemetryData.scriptsProcessed.toString())
        addAndLogTelemetryProperty(context, "kotlin.scripts.read.failures", telemetryData.scriptReadFailures.toString())
        addAndLogTelemetryProperty(context, "kotlin.scripts.parse.failures", telemetryData.scriptParseFailures.toString())
    }


    /**
     * Executed once for entire project after all executions of [KotlinSensor.execute] for individual modules.
     */
    override fun execute(context: SensorContext) {
        if (context.runtime().apiVersion.isGreaterThanOrEqual(Version.create(10, 9))) {
            addAndLogTelemetryProperty(context, "kotlin.pluginVersion", resolvePluginVersion())
            addAndLogTelemetryProperty(context, "kotlin.android", if (telemetryData.hasAndroidImports) "1" else "0")
            addAndLogTelemetryProperty(context, "kotlin.reports.surefire.classes.imported", telemetryData.surefireClassesImported.toString())
            addAndLogTelemetryProperty(context, "kotlin.reports.surefire.classes.failed", telemetryData.surefireClassesFailed.toString())

            fileProcessingTelemetry(context)
        }
    }
}
