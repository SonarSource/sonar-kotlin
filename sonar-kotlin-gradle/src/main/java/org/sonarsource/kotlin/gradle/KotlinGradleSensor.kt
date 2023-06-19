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
package org.sonarsource.kotlin.gradle

import org.gradle.tooling.GradleConnector
import org.gradle.tooling.model.kotlin.dsl.KotlinDslScriptsModel
import org.sonar.api.batch.fs.FileSystem
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.sensor.Sensor
import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.batch.sensor.SensorDescriptor
import org.sonar.api.rule.RuleKey
import org.sonar.api.batch.ScannerSide
import org.sonarsource.kotlin.api.checks.InputFileContextImpl
import org.sonarsource.kotlin.api.common.KotlinLanguage
import org.sonarsource.kotlin.api.reporting.Message
import org.sonarsource.performance.measure.PerformanceMeasure

@ScannerSide
class KotlinGradleSensor(
    val language: KotlinLanguage,
) : Sensor {

    override fun describe(descriptor: SensorDescriptor) {
        descriptor
            .onlyOnLanguage(language.key)
            .name("Gradle Sensor")
    }

    override fun execute(sensorContext: SensorContext) {
        val sensorDuration = createPerformanceMeasureReport(sensorContext)
        val fileSystem: FileSystem = sensorContext.fileSystem()

        val projectConnection = GradleConnector.newConnector()
            .forProjectDirectory(fileSystem.baseDir())
            .connect()

        projectConnection.newBuild()
            .forTasks("prepareKotlinBuildScriptModel")
            .run()

        val models = projectConnection.getModel(KotlinDslScriptsModel::class.java).scriptModels
        models.forEach {(file, model) ->
            val predicate = fileSystem.predicates().hasAbsolutePath(file.absolutePath)
            fileSystem.inputFile(predicate)?.let {inputFile: InputFile ->

                val fileContext = InputFileContextImpl(
                    sensorContext,
                    inputFile,
                    false
                )

                fileContext.reportIssue(
                    RuleKey.of("kotlin", "S100"),
                    inputFile.newRange(1, 1, 1, 3),
                    Message("No Implementation for KTS files. Sorry :-("),
                    emptyList(),
                    null
                )

            }
        }
    }
}

private fun createPerformanceMeasureReport(context: SensorContext): PerformanceMeasure.Duration? {
    return PerformanceMeasure.reportBuilder()
        .activate(context.config()[PERFORMANCE_MEASURE_ACTIVATION_PROPERTY].filter { "true" == it }.isPresent)
        .toFile(context.config()[PERFORMANCE_MEASURE_DESTINATION_FILE].orElse("/tmp/sonar.kotlin.performance.measure.json"))
        .appendMeasurementCost()
        .start("KotlinSensor")
}

const val PERFORMANCE_MEASURE_ACTIVATION_PROPERTY = "sonar.kotlin.performance.measure"
const val PERFORMANCE_MEASURE_DESTINATION_FILE = "sonar.kotlin.performance.measure.json"
