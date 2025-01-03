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

import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.psi.KtCallExpression
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.sonar.api.batch.rule.CheckFactory
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor
import org.sonar.api.config.internal.MapSettings
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.gradle.checks.MissingSettingsCheck
import org.sonarsource.kotlin.testapi.AbstractSensorTest
import kotlin.io.path.createFile

internal class KotlinGraldeSensorTest : AbstractSensorTest() {

    @AfterEach
    fun cleanupMocks() {
        unmockkAll()
    }

    @Test
    fun testDescribe() {
        val checkFactory = checkFactory("GradleKotlinCheck")

        val descriptor = DefaultSensorDescriptor()
        sensor(checkFactory).describe(descriptor)
        assertThat(descriptor.name()).isEqualTo("Gradle Sensor")
        assertThat(descriptor.languages()).isEqualTo(listOf("kotlin"))
    }

    @Test
    fun test_one_rule() {
        mockkStatic("org.sonarsource.kotlin.gradle.KotlinGradleCheckListKt")
        every { KOTLIN_GRADLE_CHECKS } returns listOf(GradleKotlinCheck::class.java)

        val settings = MapSettings()
        settings.setProperty(GRADLE_PROJECT_ROOT_PROPERTY, baseDir.toRealPath().toString())
        context.setSettings(settings)

        addSettingsKtsFile()
        addBuildFile()

        val checkFactory = checkFactory("GradleKotlinCheck")
        sensor(checkFactory).execute(context)
        val issues = context.allIssues()

        assertThat(issues).hasSize(10)
    }

    @Test
    fun test_missing_settings_rule() {
        mockkStatic("org.sonarsource.kotlin.gradle.KotlinGradleCheckListKt")
        every { KOTLIN_GRADLE_CHECKS } returns listOf(MissingSettingsCheck::class.java)

        val settings = MapSettings()
        settings.setProperty(GRADLE_PROJECT_ROOT_PROPERTY, baseDir.toRealPath().toString())
        context.setSettings(settings)

        addBuildFile()

        val checkFactory = checkFactory("S6631")
        sensor(checkFactory).execute(context)
        val issues = context.allIssues()

        assertThat(issues).hasSize(1)
        val issue = issues.iterator().next()
        assertThat(issue.primaryLocation().inputComponent().key()).isEqualTo("projectKey")
        assertThat(issue.primaryLocation().message()).isEqualTo("""Add a missing "settings.gradle" or "settings.gradle.kts" file.""")

    }

    @Test
    fun test_missing_settings_rule_is_not_triggered_when_Groovy_is_used() {
        mockkStatic("org.sonarsource.kotlin.gradle.KotlinGradleCheckListKt")
        every { KOTLIN_GRADLE_CHECKS } returns listOf(MissingSettingsCheck::class.java)

        val settings = MapSettings()
        settings.setProperty(GRADLE_PROJECT_ROOT_PROPERTY, baseDir.toRealPath().toString())
        context.setSettings(settings)

        addSettingsFile()
        addBuildFile()

        val checkFactory = checkFactory("S6631")
        sensor(checkFactory).execute(context)
        val issues = context.allIssues()

        assertThat(issues).isEmpty()
    }

    @Test
    fun test_missing_settings_rule_is_not_triggered_when_Kotlin_is_used() {
        mockkStatic("org.sonarsource.kotlin.gradle.KotlinGradleCheckListKt")
        every { KOTLIN_GRADLE_CHECKS } returns listOf(MissingSettingsCheck::class.java)

        val settings = MapSettings()
        settings.setProperty(GRADLE_PROJECT_ROOT_PROPERTY, baseDir.toRealPath().toString())
        context.setSettings(settings)

        addSettingsKtsFile()
        addBuildFile()

        val checkFactory = checkFactory("S6631")
        sensor(checkFactory).execute(context)
        val issues = context.allIssues()

        assertThat(issues).isEmpty()
    }

    @Test
    fun test_missing_settings_rule_is_not_triggered_when_rule_is_not_active() {
        mockkStatic("org.sonarsource.kotlin.gradle.KotlinGradleCheckListKt")
        every { KOTLIN_GRADLE_CHECKS } returns listOf(MissingSettingsCheck::class.java)

        val settings = MapSettings()
        settings.setProperty(GRADLE_PROJECT_ROOT_PROPERTY, baseDir.toRealPath().toString())
        context.setSettings(settings)

        addBuildFile()

        val checkFactory = checkFactory("GradleKotlinCheck")
        sensor(checkFactory).execute(context)
        val issues = context.allIssues()

        assertThat(issues).isEmpty()
    }

    private fun addBuildFile() {
        val buildFile = createInputFile(
            "build.gradle.kts", """
                plugins {
                    application 
                }
                
                repositories {
                    mavenCentral() 
                }
                
                dependencies {
                    testImplementation("org.junit.jupiter:junit-jupiter:5.9.1") 
                
                    implementation("com.google.guava:guava:31.1-jre") 
                }
                
                application {
                    mainClass.set("demo.App") 
                }
                
                tasks.named<Test>("test") {
                    useJUnitPlatform() 
                }
                """.trimIndent()
        )

        baseDir.resolve("build.gradle.kts").createFile()

        context.fileSystem().add(buildFile)
    }

    private fun addSettingsFile() {
        val settingsFile = createInputFile("settings.gradle", "rootProject.name = 'kotlin'")
        baseDir.resolve("settings.gradle").createFile()
        context.fileSystem().add(settingsFile)
    }

    private fun addSettingsKtsFile() {
        val settingsFile = createInputFile(
            "settings.gradle.kts", """
                rootProject.name = "kotlin"
                """.trimIndent()
        )
        baseDir.resolve("settings.gradle.kts").createFile()
        context.fileSystem().add(settingsFile)
    }

    private fun sensor(checkFactory: CheckFactory): KotlinGradleSensor {
        return KotlinGradleSensor(checkFactory, language())
    }
}

@Rule(key = "GradleKotlinCheck")
internal class GradleKotlinCheck : AbstractCheck() {
    override fun visitCallExpression(expression: KtCallExpression, kfc: KotlinFileContext) {
        kfc.reportIssue(expression, "Boom!")
    }
}
