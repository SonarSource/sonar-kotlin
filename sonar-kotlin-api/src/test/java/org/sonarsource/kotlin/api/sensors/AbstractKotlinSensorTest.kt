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
package org.sonarsource.kotlin.api.sensors

import com.intellij.openapi.util.Disposer
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.*
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.rule.CheckFactory
import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.batch.sensor.SensorDescriptor
import org.sonar.api.config.internal.ConfigurationBridge
import org.sonar.api.config.internal.MapSettings
import org.sonar.check.Rule
import org.sonarsource.analyzer.commons.ProgressReport
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.checks.KotlinCheck
import org.sonarsource.kotlin.api.common.KOTLIN_LANGUAGE_VERSION
import org.sonarsource.kotlin.api.common.KotlinLanguage
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.KtChecksVisitor
import org.sonarsource.kotlin.testapi.AbstractSensorTest

class AbstractKotlinSensorTest : AbstractSensorTest() {

    private val disposable = Disposer.newDisposable()

    @AfterEach
    fun dispose() {
        Disposer.dispose(disposable)
    }

    @Test
    fun test_one_file() {
        val inputFile = createInputFile(
            "file1.kt", """
     fun main() {
     print (1 == 1);}
     """.trimIndent()
        )
        context.fileSystem().add(inputFile)

        sensor().execute(context)

        val issues = context.allIssues()
        assertThat(issues).hasSize(1)

    }

    @Test
    fun test_no_files() {
        sensor().execute(context)

        val issues = context.allIssues()
        assertThat(issues)
    }

    @Test
    fun test_fail_parsing() {
        val inputFile = createInputFile("file1.kt", "enum class A { <!REDECLARATION!>FOO<!>,<!REDECLARATION!>FOO<!> }")
        context.fileSystem().add(inputFile)

        sensor().execute(context)
        val analysisErrors = context.allAnalysisErrors()
        assertThat(analysisErrors).hasSize(1)
        val analysisError = analysisErrors.iterator().next()
        assertThat(analysisError.inputFile()).isEqualTo(inputFile)
        assertThat(analysisError.message()).isEqualTo("Unable to parse file: file1.kt")
        val textPointer = analysisError.location()
        assertThat(textPointer).isNotNull
        assertThat(textPointer!!.line()).isEqualTo(1)
        assertThat(textPointer.lineOffset()).isEqualTo(14)
        assertThat(logTester.logs())
            .contains(String.format("Unable to parse file: %s. Parse error at position 1:14", inputFile.uri()))
    }

    @Test
    fun `not setting the kotlin version analyzer property results in Environment with the default Kotlin version`() {
        logTester.setLevel(Level.DEBUG)

        val sensorContext = mockk<SensorContext> {
            every { config() } returns ConfigurationBridge(MapSettings())
        }

        val environment = environment(disposable, sensorContext, LOG)

        val expectedKotlinVersion = LanguageVersion.LATEST_STABLE

        assertThat(environment.configuration.languageVersionSettings.languageVersion).isSameAs(expectedKotlinVersion)
        assertThat(logTester.logs(Level.WARN)).isEmpty()
        assertThat(logTester.logs(Level.DEBUG))
            .contains("Using Kotlin ${expectedKotlinVersion.versionString} to parse source code")
    }

    @Test
    fun `setting the kotlin version analyzer property to a valid value is reflected in the Environment`() {
        logTester.setLevel(Level.DEBUG)

        val sensorContext = mockk<SensorContext> {
            every { config() } returns ConfigurationBridge(MapSettings().apply {
                setProperty(KOTLIN_LANGUAGE_VERSION, "1.3")
            })
        }

        val environment = environment(disposable, sensorContext, LOG)

        val expectedKotlinVersion = LanguageVersion.KOTLIN_1_3

        assertThat(environment.configuration.languageVersionSettings.languageVersion).isSameAs(expectedKotlinVersion)
        assertThat(logTester.logs(Level.WARN)).isEmpty()
        assertThat(logTester.logs(Level.DEBUG))
            .contains("Using Kotlin ${expectedKotlinVersion.versionString} to parse source code")
    }

    @Test
    fun `setting the kotlin version analyzer property to an invalid value results in log message and the default version to be used`() {
        logTester.setLevel(Level.DEBUG)

        val sensorContext = mockk<SensorContext> {
            every { config() } returns ConfigurationBridge(MapSettings().apply {
                setProperty(KOTLIN_LANGUAGE_VERSION, "foo")
            })
        }

        val environment = environment(disposable, sensorContext, LOG)

        val expectedKotlinVersion = LanguageVersion.LATEST_STABLE

        assertThat(environment.configuration.languageVersionSettings.languageVersion).isSameAs(expectedKotlinVersion)
        assertThat(logTester.logs(Level.WARN))
            .containsExactly("Failed to find Kotlin version 'foo'. Defaulting to ${expectedKotlinVersion.versionString}")
        assertThat(logTester.logs(Level.DEBUG))
            .contains("Using Kotlin ${expectedKotlinVersion.versionString} to parse source code")
    }

    @Test
    fun `setting the kotlin version analyzer property to whitespaces only results in the default version to be used`() {
        logTester.setLevel(Level.DEBUG)

        val sensorContext = mockk<SensorContext> {
            every { config() } returns ConfigurationBridge(MapSettings().apply {
                setProperty(KOTLIN_LANGUAGE_VERSION, "  ")
            })
        }

        val environment = environment(disposable, sensorContext, LOG)

        val expectedKotlinVersion = LanguageVersion.LATEST_STABLE

        assertThat(environment.configuration.languageVersionSettings.languageVersion).isSameAs(expectedKotlinVersion)
        assertThat(logTester.logs(Level.WARN)).isEmpty()
        assertThat(logTester.logs(Level.DEBUG))
            .contains("Using Kotlin ${expectedKotlinVersion.versionString} to parse source code")
    }

    private fun sensor() = DummyKotlinSensor(checkFactory("DummyKotlinCheck"), language(), listOf(DummyKotlinCheck::class.java))
}

private val LOG = LoggerFactory.getLogger(DummyKotlinSensor::class.java)
class DummyKotlinSensor(checkFactory: CheckFactory, language: KotlinLanguage, checks: List<Class<out KotlinCheck>>) :
    AbstractKotlinSensor(
        checkFactory,
        language,
        checks,
    ) {
    override fun getExecuteContext(
        sensorContext: SensorContext,
        filesToAnalyze: Iterable<InputFile>,
        progressReport: ProgressReport,
        filenames: List<String>
    ): AbstractKotlinSensorExecuteContext = object : AbstractKotlinSensorExecuteContext(
        sensorContext, filesToAnalyze, progressReport, listOf(KtChecksVisitor(checks)), filenames, LOG
    ) {
        override val bindingContext: BindingContext = BindingContext.EMPTY
        override val doResolve: Boolean = false
    }

    override fun getFilesToAnalyse(sensorContext: SensorContext): Iterable<InputFile> =
        sensorContext.fileSystem().inputFiles(sensorContext.fileSystem().predicates().all())

    override fun describe(descriptor: SensorDescriptor) {}

}

@Rule(key = "DummyKotlinCheck")
internal class DummyKotlinCheck : AbstractCheck() {
    override fun visitCallExpression(expression: KtCallExpression, kfc: KotlinFileContext) {
        kfc.reportIssue(expression, "Boom!")
    }
}
