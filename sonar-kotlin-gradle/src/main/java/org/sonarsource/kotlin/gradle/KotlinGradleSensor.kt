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
package org.sonarsource.kotlin.gradle

import org.jetbrains.kotlin.resolve.BindingContext
import org.slf4j.LoggerFactory
import org.sonar.api.batch.fs.FileSystem
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.batch.sensor.SensorDescriptor
import org.sonar.api.batch.rule.CheckFactory
import org.sonar.api.rule.RuleKey
import org.sonarsource.analyzer.commons.ProgressReport
import org.sonarsource.kotlin.api.common.KOTLIN_REPOSITORY_KEY
import org.sonarsource.kotlin.api.common.KotlinLanguage
import org.sonarsource.kotlin.api.sensors.AbstractKotlinSensor
import org.sonarsource.kotlin.api.sensors.AbstractKotlinSensorExecuteContext
import org.sonarsource.kotlin.api.visiting.KtChecksVisitor
import java.io.File

const val GRADLE_PROJECT_ROOT_PROPERTY = "sonar.kotlin.gradleProjectRoot"
const val MISSING_SETTINGS_RULE_KEY = "S6631"

private val LOG = LoggerFactory.getLogger(KotlinGradleSensor::class.java)

class KotlinGradleSensor(
    checkFactory: CheckFactory,
    language: KotlinLanguage,
) : AbstractKotlinSensor(
    checkFactory, language, KOTLIN_GRADLE_CHECKS
) {

    override fun describe(descriptor: SensorDescriptor) {
        descriptor
            .onlyOnLanguage(language.key)
            .name("Gradle Sensor")
    }

    override fun getExecuteContext(
        sensorContext: SensorContext,
        filesToAnalyze: Iterable<InputFile>,
        progressReport: ProgressReport,
        filenames: List<String>
    ) = object : AbstractKotlinSensorExecuteContext(
        sensorContext, filesToAnalyze, progressReport, listOf(KtChecksVisitor(checks)), filenames, LOG
    ) {
        override val bindingContext: BindingContext = BindingContext.EMPTY
    }

    override fun getFilesToAnalyse(sensorContext: SensorContext): Iterable<InputFile> {
        val fileSystem: FileSystem = sensorContext.fileSystem()

        val mainFilePredicate = fileSystem.predicates().and(
            fileSystem.predicates().or(
                fileSystem.predicates().hasFilename("build.gradle.kts"),
                fileSystem.predicates().hasFilename("settings.gradle.kts")
            )
        )

        sensorContext.config()[GRADLE_PROJECT_ROOT_PROPERTY].ifPresent {
            checkForMissingGradleSettings(File(it), sensorContext)
        }

        return fileSystem.inputFiles(mainFilePredicate)
    }

    private fun checkForMissingGradleSettings(rootDirFile: File, sensorContext: SensorContext) {
        val missingSettingsRuleKey = RuleKey.of(KOTLIN_REPOSITORY_KEY, MISSING_SETTINGS_RULE_KEY)
        if (sensorContext.activeRules().find(missingSettingsRuleKey) == null) return

        if (!rootDirFile.resolve("settings.gradle").exists() && !rootDirFile.resolve("settings.gradle.kts").exists()) {
            val project = sensorContext.project()

            with(sensorContext) {
                newIssue()
                    .forRule(missingSettingsRuleKey)
                    .at(
                        newIssue()
                            .newLocation()
                            .on(project)
                            .message("""Add a missing "settings.gradle" or "settings.gradle.kts" file.""")
                    )
                    .save()
            }
        }
    }
}
