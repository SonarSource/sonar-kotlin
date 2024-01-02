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
package org.sonarsource.kotlin.api.sensors

import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.rule.CheckFactory
import org.sonar.api.batch.rule.Checks
import org.sonar.api.batch.sensor.Sensor
import org.sonar.api.batch.sensor.SensorContext
import org.sonarsource.analyzer.commons.ProgressReport
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.checks.KotlinCheck
import org.sonarsource.kotlin.api.common.KOTLIN_REPOSITORY_KEY
import org.sonarsource.kotlin.api.common.KotlinLanguage
import org.sonarsource.performance.measure.PerformanceMeasure
import java.util.concurrent.TimeUnit


private const val PERFORMANCE_MEASURE_ACTIVATION_PROPERTY = "sonar.kotlin.performance.measure"
private const val PERFORMANCE_MEASURE_DESTINATION_FILE = "sonar.kotlin.performance.measure.json"
abstract class AbstractKotlinSensor(
    checkFactory: CheckFactory,
    val language: KotlinLanguage,
    checks: List<Class<out KotlinCheck>>
) : Sensor {

    val checks: Checks<AbstractCheck> = checkFactory.create<AbstractCheck>(KOTLIN_REPOSITORY_KEY).apply {
        addAnnotatedChecks(checks)
        all().forEach { it.initialize(ruleKey(it)!!) }
    }

    abstract fun getExecuteContext(
        sensorContext: SensorContext,
        filesToAnalyze: Iterable<InputFile>,
        progressReport: ProgressReport,
        filenames: List<String>
    ): AbstractKotlinSensorExecuteContext

    abstract fun getFilesToAnalyse(sensorContext: SensorContext): Iterable<InputFile>

    override fun execute(sensorContext: SensorContext) {
        val sensorDuration = createPerformanceMeasureReport(sensorContext)
        val filesToAnalyze = getFilesToAnalyse(sensorContext)

        val filenames = filesToAnalyze.map { it.toString() }
        if (filenames.isEmpty()) return
        val progressReport = ProgressReport("Progress of the ${language.name} analysis", TimeUnit.SECONDS.toMillis(10))

        var success = false
        try {
            success = getExecuteContext(sensorContext, filesToAnalyze, progressReport, filenames).analyzeFiles()
        } finally {
            if (success) {
                progressReport.stop()
            } else {
                progressReport.cancel()
            }
        }
        sensorDuration?.stop()
    }
}
private fun createPerformanceMeasureReport(context: SensorContext): PerformanceMeasure.Duration? =
    PerformanceMeasure.reportBuilder()
        .activate(context.config()[PERFORMANCE_MEASURE_ACTIVATION_PROPERTY].filter { "true" == it }.isPresent)
        .toFile(context.config()[PERFORMANCE_MEASURE_DESTINATION_FILE].orElse("/tmp/sonar.kotlin.performance.measure.json"))
        .appendMeasurementCost()
        .start("KotlinSensor")
