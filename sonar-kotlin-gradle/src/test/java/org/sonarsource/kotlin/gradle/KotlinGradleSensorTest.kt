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
import org.sonarsource.kotlin.gradle.checks.MissingVerificationMetadataCheck
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

        val checkFactory = checkFactory(MISSING_SETTINGS_RULE_KEY)
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

        val checkFactory = checkFactory(MISSING_SETTINGS_RULE_KEY)
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

        val checkFactory = checkFactory(MISSING_SETTINGS_RULE_KEY)
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

    @Test
    fun test_missing_verification_metadata_rule_is_triggered_when_verification_metadata_is_not_present() {
        mockkStatic("org.sonarsource.kotlin.gradle.KotlinGradleCheckListKt")
        every { KOTLIN_GRADLE_CHECKS } returns listOf(MissingVerificationMetadataCheck::class.java)

        val settings = MapSettings()
        settings.setProperty(GRADLE_PROJECT_ROOT_PROPERTY, baseDir.toRealPath().toString())
        context.setSettings(settings)

        val checkFactory = checkFactory(MISSING_VERIFICATION_METADATA_RULE_KEY)
        sensor(checkFactory).execute(context)
        val issues = context.allIssues()

        assertThat(issues).hasSize(1)
        val issue = issues.iterator().next()
        assertThat(issue.primaryLocation().inputComponent().key()).isEqualTo("projectKey")
        val expectedMessage = """Create a "verification-metadata.xml" file to verify these dependencies against a known checksum or signature."""
        assertThat(issue.primaryLocation().message()).isEqualTo(expectedMessage)
    }

    @Test
    fun test_missing_verification_metadata_rule_is_not_triggered_when_verification_metadata_is_present() {
        mockkStatic("org.sonarsource.kotlin.gradle.KotlinGradleCheckListKt")
        every { KOTLIN_GRADLE_CHECKS } returns listOf(MissingVerificationMetadataCheck::class.java)

        val settings = MapSettings()
        settings.setProperty(GRADLE_PROJECT_ROOT_PROPERTY, baseDir.toRealPath().toString())
        context.setSettings(settings)

        addVerificationMetadataFile()

        val checkFactory = checkFactory(MISSING_VERIFICATION_METADATA_RULE_KEY)
        sensor(checkFactory).execute(context)

        assertThat(context.allIssues()).isEmpty()
    }

    @Test
    fun test_missing_verification_metadata_rule_is_not_triggered_when_rule_is_not_active() {
        mockkStatic("org.sonarsource.kotlin.gradle.KotlinGradleCheckListKt")
        every { KOTLIN_GRADLE_CHECKS } returns listOf(MissingVerificationMetadataCheck::class.java)

        val settings = MapSettings()
        settings.setProperty(GRADLE_PROJECT_ROOT_PROPERTY, baseDir.toRealPath().toString())
        context.setSettings(settings)

        addSettingsKtsFile()
        addBuildFile()

        val checkFactory = checkFactory() // No rule key
        sensor(checkFactory).execute(context)

        assertThat(context.allIssues()).isEmpty()
    }

    @Test
    fun test_missing_verification_metadata_rule_is_not_triggereD_when_gradle_project_root_property_is_not_set() {
        mockkStatic("org.sonarsource.kotlin.gradle.KotlinGradleCheckListKt")
        every { KOTLIN_GRADLE_CHECKS } returns listOf(MissingVerificationMetadataCheck::class.java)

        val settings = MapSettings()
        context.setSettings(settings)

        val checkFactory = checkFactory(MISSING_VERIFICATION_METADATA_RULE_KEY)
        sensor(checkFactory).execute(context)

        assertThat(context.allIssues()).isEmpty()
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

    private fun addVerificationMetadataFile() {
        val verificationMetadataFile = createInputFile(
            "verification-metadata.xml", """
                <?xml version="1.0" encoding="UTF-8"?>
                <verification-metadata xmlns="https://schema.gradle.org/dependency-verification" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="https://schema.gradle.org/dependency-verification https://schema.gradle.org/dependency-verification/dependency-verification-1.3.xsd">
                   <configuration>
                      <verify-metadata>false</verify-metadata>
                      <verify-signatures>false</verify-signatures>
                   </configuration>
                   <components>
                      <component group="ch.qos.logback" name="logback-classic" version="1.2.9">
                         <artifact name="logback-classic-1.2.9.jar">
                            <sha256 value="ad745cc243805800d1ebbf5b7deba03b37c95885e6bce71335a73f7d6d0f14ee" origin="Verified"/>
                         </artifact>
                      </component>
                   </components>
                </verification-metadata>
                """.trimIndent())
        baseDir.resolve("gradle").toFile().mkdir()
        baseDir.resolve("gradle/verification-metadata.xml").createFile()
        context.fileSystem().add(verificationMetadataFile)
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
