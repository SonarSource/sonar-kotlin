/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonarsource.kotlin.gradle

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
import java.nio.file.Files

const val GRADLE_PROJECT_ROOT_PROPERTY = "sonar.kotlin.gradleProjectRoot"
const val MISSING_SETTINGS_RULE_KEY = "S6631"
const val MISSING_VERIFICATION_METADATA_RULE_KEY = "S6474"

private val LOG = LoggerFactory.getLogger(KotlinGradleSensor::class.java)

class KotlinGradleSensor(
    checkFactory: CheckFactory,
    language: KotlinLanguage,
) : AbstractKotlinSensor(
    checkFactory, emptyList(), language, KOTLIN_GRADLE_CHECKS
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
        override val classpath: List<String> = listOf()
    }

    override fun getFilesToAnalyse(sensorContext: SensorContext): Iterable<InputFile> {
        val fileSystem: FileSystem = sensorContext.fileSystem()

        val mainFilePredicate = fileSystem.predicates().and(
            fileSystem.predicates().or(
                fileSystem.predicates().hasFilename("build.gradle.kts"),
                fileSystem.predicates().hasFilename("settings.gradle.kts")
            )
        )

        sensorContext.config()[GRADLE_PROJECT_ROOT_PROPERTY]
            .map { File(it) }
            // Only run checks on the root module, where Gradle project root == baseDir
            .filter { Files.isSameFile(it.toPath(), sensorContext.fileSystem().baseDir().toPath()) }
            .ifPresent {
                checkForMissingGradleSettings(it, sensorContext)
                checkForMissingVerificationMetadata(it, sensorContext)
            }

        return fileSystem.inputFiles(mainFilePredicate)
    }

    private fun checkForMissingGradleSettings(rootDirFile: File, sensorContext: SensorContext) {
        val missingSettingsRuleKey = RuleKey.of(KOTLIN_REPOSITORY_KEY, MISSING_SETTINGS_RULE_KEY)
        if (sensorContext.activeRules().find(missingSettingsRuleKey) == null) return

        if (!rootDirFile.resolve("settings.gradle").exists() && !rootDirFile.resolve("settings.gradle.kts").exists()) {
            raiseProjectLevelIssue(
                sensorContext,
                missingSettingsRuleKey,
                """Add a missing "settings.gradle" or "settings.gradle.kts" file.""",
            )
        }
    }

    private fun checkForMissingVerificationMetadata(rootDirFile: File, sensorContext: SensorContext) {
        val missingVerificationMetadataRuleKey = RuleKey.of(KOTLIN_REPOSITORY_KEY, MISSING_VERIFICATION_METADATA_RULE_KEY)
        if (sensorContext.activeRules().find(missingVerificationMetadataRuleKey) == null) return

        if (!rootDirFile.resolve("gradle/verification-metadata.xml").exists()) {
            raiseProjectLevelIssue(
                sensorContext,
                missingVerificationMetadataRuleKey,
                """Dependencies are not verified because the "verification-metadata.xml" file is missing. Make sure it is safe here.""",
            )
        }
    }

    private fun raiseProjectLevelIssue(sensorContext: SensorContext, ruleKey: RuleKey, message: String) =
        with(sensorContext) {
            newIssue()
                .forRule(ruleKey)
                .at(
                    newIssue()
                        .newLocation()
                        .on(project())
                        .message(message)
                )
                .save()
        }
}
