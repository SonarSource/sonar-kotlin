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

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.rule.CheckFactory
import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.batch.sensor.highlighting.TypeOfText
import org.sonar.api.batch.sensor.issue.internal.DefaultNoSonarFilter
import org.sonar.api.config.internal.ConfigurationBridge
import org.sonar.api.config.internal.MapSettings
import org.sonar.api.measures.CoreMetrics
import org.sonar.api.utils.log.LoggerLevel
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.converter.Environment
import org.sonarsource.kotlin.testing.AbstractSensorTest
import org.sonarsource.kotlin.testing.assertTextRange
import java.io.IOException
import kotlin.time.ExperimentalTime

@ExperimentalTime
internal class KotlinSensorTest : AbstractSensorTest() {

    @AfterEach
    fun cleanupMocks() {
        unmockkAll()
    }

    @Test
    fun test_one_rule() {
        val inputFile = createInputFile(
            "file1.kt", """
     fun main(args: Array<String>) {
     print (1 == 1);}
     """.trimIndent()
        )
        context.fileSystem().add(inputFile)
        val checkFactory = checkFactory("S1764")
        sensor(checkFactory).execute(context)
        val issues = context.allIssues()
        Assertions.assertThat(issues).hasSize(1)
        val issue = issues.iterator().next()
        Assertions.assertThat(issue.ruleKey().rule()).isEqualTo("S1764")
        val location = issue.primaryLocation()
        Assertions.assertThat(location.inputComponent()).isEqualTo(inputFile)
        Assertions.assertThat(location.message())
            .isEqualTo("Correct one of the identical sub-expressions on both sides this operator.")
        assertTextRange(location.textRange()).hasRange(2, 12, 2, 13)
    }

    @org.junit.jupiter.api.Disabled
    @Test
    fun test_commented_code() {
        val inputFile = createInputFile(
            "file1.kt", """
     fun main(args: Array<String>) {
     //fun foo () { if (true) {print("string literal");}}
     print (1 == 1);
     print(b);
     //a b c ...
     foo();
     // Coefficients of polynomial
     val b = DoubleArray(n) // linear
     val c = DoubleArray(n + 1) // quadratic
     val d = DoubleArray(n) // cubic
     }
     """.trimIndent()
        )
        context.fileSystem().add(inputFile)
        val checkFactory = checkFactory("S125")
        sensor(checkFactory).execute(context)
        val issues = context.allIssues()
        Assertions.assertThat(issues).hasSize(1)
        val issue = issues.iterator().next()
        Assertions.assertThat(issue.ruleKey().rule()).isEqualTo("S125")
        val location = issue.primaryLocation()
        Assertions.assertThat(location.inputComponent()).isEqualTo(inputFile)
        Assertions.assertThat(location.message()).isEqualTo("Remove this commented out code.")
    }

    @Test
    fun simple_file() {
        val inputFile = createInputFile(
            "file1.kt", """
     fun main(args: Array<String>) {
     print (1 == 1); print("abc"); }
     data class A(val a: Int)
     """.trimIndent()
        )
        context.fileSystem().add(inputFile)
        sensor(checkFactory()).execute(context)
        Assertions.assertThat(context.highlightingTypeAt(inputFile.key(), 1, 0)).containsExactly(TypeOfText.KEYWORD)
        Assertions.assertThat(context.highlightingTypeAt(inputFile.key(), 1, 3)).isEmpty()
        Assertions.assertThat(context.measure(inputFile.key(), CoreMetrics.NCLOC).value()).isEqualTo(3)
        Assertions.assertThat(context.measure(inputFile.key(), CoreMetrics.COMMENT_LINES).value()).isZero
        Assertions.assertThat(context.measure(inputFile.key(), CoreMetrics.FUNCTIONS).value()).isEqualTo(1)
        Assertions.assertThat(context.measure(inputFile.key(), CoreMetrics.CLASSES).value()).isEqualTo(1)
        Assertions.assertThat(context.cpdTokens(inputFile.key())!![1].value)
            .isEqualTo("print(1==1);print(\"LITERAL\");}")
        Assertions.assertThat(context.measure(inputFile.key(), CoreMetrics.COMPLEXITY).value()).isEqualTo(1)
        Assertions.assertThat(context.measure(inputFile.key(), CoreMetrics.STATEMENTS).value()).isEqualTo(2)

        // FIXME
        //assertThat(logTester.logs()).contains("1 source files to be analyzed");
    }

    @Test
    fun test_issue_suppression() {
        val inputFile = createInputFile(
            "file1.kt", """
     @SuppressWarnings("kotlin:S1764")
     fun main() {
     print (1 == 1);}
     @SuppressWarnings(value=["kotlin:S1764"])
     fun main2() {
     print (1 == 1);}
     """.trimIndent()
        )
        context.fileSystem().add(inputFile)
        val checkFactory = checkFactory("S1764")
        sensor(checkFactory).execute(context)
        val issues = context.allIssues()
        Assertions.assertThat(issues).isEmpty()
    }

    @Test
    fun test_issue_not_suppressed() {
        val inputFile = createInputFile(
            "file1.kt", """
     @SuppressWarnings("S1764")
     fun main() {
     print (1 == 1);}
     @SuppressWarnings(value=["scala:S1764"])
     fun main2() {
     print (1 == 1);}
     """.trimIndent()
        )
        context.fileSystem().add(inputFile)
        val checkFactory = checkFactory("S1764")
        sensor(checkFactory).execute(context)
        val issues = context.allIssues()
        Assertions.assertThat(issues).hasSize(2)
    }

    @Test
    fun `Ensure compiler crashes during BindingContext generation don't crash engine`() {
        context.setCanSkipUnchangedFiles(false)
        executeAnalysisWithInvalidBindingContext()
        assertThat(logTester.logs(LoggerLevel.ERROR)).containsExactly("Could not generate binding context. Proceeding without semantics.")
    }

    @Test
    fun `BindingContext generation does not crash when there are no files to analyze`() {
        context.setCanSkipUnchangedFiles(true)
        executeAnalysisWithInvalidBindingContext()
        assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty()
    }

    private fun executeAnalysisWithInvalidBindingContext() {
        val inputFile = createInputFile(
            "file1.kt", """
        abstract class MyClass {
            abstract fun <P1> foo(): (P1) -> Unknown<String>
        
            private fun callTryConvertConstant() {
                println(foo<String>())
            }
        }
        """.trimIndent(),
            InputFile.Status.SAME
        )
        context.fileSystem().add(inputFile)

        mockkStatic("org.sonarsource.kotlin.plugin.KotlinSensorKt")
        every { environment(any()) } returns Environment(listOf("file1.kt"), LanguageVersion.LATEST_STABLE)

        val checkFactory = checkFactory("S1764")
        assertDoesNotThrow { sensor(checkFactory).execute(context) }

        unmockkAll()
    }

    @Test
    fun test_fail_parsing() {
        val inputFile = createInputFile("file1.kt", "enum class A { <!REDECLARATION!>FOO<!>,<!REDECLARATION!>FOO<!> }")
        context.fileSystem().add(inputFile)
        val checkFactory = checkFactory("S1764")
        sensor(checkFactory).execute(context)
        val analysisErrors = context.allAnalysisErrors()
        Assertions.assertThat(analysisErrors).hasSize(1)
        val analysisError = analysisErrors.iterator().next()
        Assertions.assertThat(analysisError.inputFile()).isEqualTo(inputFile)
        Assertions.assertThat(analysisError.message()).isEqualTo("Unable to parse file: file1.kt")
        val textPointer = analysisError.location()
        Assertions.assertThat(textPointer).isNotNull
        Assertions.assertThat(textPointer!!.line()).isEqualTo(1)
        Assertions.assertThat(textPointer.lineOffset()).isEqualTo(14)
        Assertions.assertThat(logTester.logs())
            .contains(String.format("Unable to parse file: %s. Parse error at position 1:14", inputFile.uri()))
    }

    @Test
    fun test_fail_reading() {
        val inputFile = spyk(createInputFile("file1.kt", "class A { fun f() = TODO() }"))
        context.fileSystem().add(inputFile)
        every { inputFile.contents() } throws IOException("Can't read")
        every { inputFile.toString() } returns "file1.kt"

        val checkFactory = checkFactory("S1764")
        sensor(checkFactory).execute(context)
        val analysisErrors = context.allAnalysisErrors()
        Assertions.assertThat(analysisErrors).hasSize(1)
        val analysisError = analysisErrors.iterator().next()
        Assertions.assertThat(analysisError.inputFile()).isEqualTo(inputFile)
        Assertions.assertThat(analysisError.message()).isEqualTo("Unable to parse file: file1.kt")
        val textPointer = analysisError.location()
        Assertions.assertThat(textPointer).isNull()

        Assertions.assertThat(logTester.logs(LoggerLevel.ERROR)).contains("Cannot read 'file1.kt': Can't read")
    }

    @Test
    fun test_with_classpath() {
        val settings = MapSettings()
        settings.setProperty(KotlinPlugin.SONAR_JAVA_BINARIES, "classes/")
        context.setSettings(settings)
        val inputFile = createInputFile("file1.kt", "class A { fun f() = TODO() }")
        context.fileSystem().add(inputFile)
        val checkFactory = checkFactory("S1764")
        sensor(checkFactory).execute(context)
        val analysisErrors = context.allAnalysisErrors()
        Assertions.assertThat(analysisErrors).isEmpty()
    }

    @Test
    fun test_with_blank_classpath() {
        val settings = MapSettings()
        settings.setProperty(KotlinPlugin.SONAR_JAVA_BINARIES, " ")
        context.setSettings(settings)
        val inputFile = createInputFile("file1.kt", "class A { fun f() = TODO() }")
        context.fileSystem().add(inputFile)
        val checkFactory = checkFactory("S1764")
        sensor(checkFactory).execute(context)
        val analysisErrors = context.allAnalysisErrors()
        Assertions.assertThat(analysisErrors).isEmpty()
    }

    private fun failFastSensorWithEnvironmentSetup(failFast: Boolean?): KotlinSensor {
        mockkStatic("org.sonarsource.kotlin.plugin.KotlinCheckListKt")
        every { KOTLIN_CHECKS } returns listOf(ExceptionThrowingCheck::class.java)

        context.apply {
            setSettings(MapSettings().apply {
                failFast?.let { setProperty(KotlinPlugin.FAIL_FAST_PROPERTY_NAME, it) }
            })

            fileSystem().add(createInputFile("file1.kt", "class A { fun f() = TODO() }"))
        }
        return sensor(checkFactory("ETRule1"))
    }

    @Test
    fun `ensure failFast is working when set`() {
        val sensor = failFastSensorWithEnvironmentSetup(true)

        assertThrows<IllegalStateException>("Exception in 'KtChecksVisitor' while analyzing 'file1.kt'") {
            sensor.execute(context)
        }.apply {
            assertThat(this).hasCause(TestException("This is a test message"))
        }

        assertThat(logTester.logs(LoggerLevel.ERROR))
            .containsExactly("Cannot analyse 'file1.kt' with 'KtChecksVisitor': This is a test message")
    }

    @Test
    fun `ensure failFast is not triggering when set to false`() {
        val sensor = failFastSensorWithEnvironmentSetup(false)
        assertDoesNotThrow { sensor.execute(context) }
        assertThat(logTester.logs(LoggerLevel.ERROR))
            .containsExactly("Cannot analyse 'file1.kt' with 'KtChecksVisitor': This is a test message")
    }

    @Test
    fun `ensure failFast is not triggered when not set at all`() {
        val sensor = failFastSensorWithEnvironmentSetup(null)
        assertDoesNotThrow { sensor.execute(context) }
        assertThat(logTester.logs(LoggerLevel.ERROR))
            .containsExactly("Cannot analyse 'file1.kt' with 'KtChecksVisitor': This is a test message")
    }

    @Test
    fun `not setting the kotlin version analyzer property results in Environment with the default Kotlin version`() {
        logTester.setLevel(LoggerLevel.DEBUG)

        val sensorContext = mockk<SensorContext> {
            every { config() } returns ConfigurationBridge(MapSettings())
        }

        val environment = environment(sensorContext)

        val expectedKotlinVersion = LanguageVersion.KOTLIN_1_5

        assertThat(environment.configuration.languageVersionSettings.languageVersion).isSameAs(expectedKotlinVersion)
        assertThat(logTester.logs(LoggerLevel.WARN)).isEmpty()
        assertThat(logTester.logs(LoggerLevel.DEBUG))
            .contains("Using Kotlin ${expectedKotlinVersion.versionString} to parse source code")
    }

    @Test
    fun `setting the kotlin version analyzer property to a valid value is reflected in the Environment`() {
        logTester.setLevel(LoggerLevel.DEBUG)

        val sensorContext = mockk<SensorContext> {
            every { config() } returns ConfigurationBridge(MapSettings().apply {
                setProperty(KotlinPlugin.KOTLIN_LANGUAGE_VERSION, "1.3")
            })
        }

        val environment = environment(sensorContext)

        val expectedKotlinVersion = LanguageVersion.KOTLIN_1_3

        assertThat(environment.configuration.languageVersionSettings.languageVersion).isSameAs(expectedKotlinVersion)
        assertThat(logTester.logs(LoggerLevel.WARN)).isEmpty()
        assertThat(logTester.logs(LoggerLevel.DEBUG))
            .contains("Using Kotlin ${expectedKotlinVersion.versionString} to parse source code")
    }

    @Test
    fun `setting the kotlin version analyzer property to an invalid value results in log message and the default version to be used`() {
        logTester.setLevel(LoggerLevel.DEBUG)

        val sensorContext = mockk<SensorContext> {
            every { config() } returns ConfigurationBridge(MapSettings().apply {
                setProperty(KotlinPlugin.KOTLIN_LANGUAGE_VERSION, "foo")
            })
        }

        val environment = environment(sensorContext)

        val expectedKotlinVersion = LanguageVersion.KOTLIN_1_5

        assertThat(environment.configuration.languageVersionSettings.languageVersion).isSameAs(expectedKotlinVersion)
        assertThat(logTester.logs(LoggerLevel.WARN))
            .containsExactly("Failed to find Kotlin version 'foo'. Defaulting to ${expectedKotlinVersion.versionString}")
        assertThat(logTester.logs(LoggerLevel.DEBUG))
            .contains("Using Kotlin ${expectedKotlinVersion.versionString} to parse source code")
    }

    @Test
    fun `setting the kotlin version analyzer property to whitespaces only results in the default version to be used`() {
        logTester.setLevel(LoggerLevel.DEBUG)

        val sensorContext = mockk<SensorContext> {
            every { config() } returns ConfigurationBridge(MapSettings().apply {
                setProperty(KotlinPlugin.KOTLIN_LANGUAGE_VERSION, "  ")
            })
        }

        val environment = environment(sensorContext)

        val expectedKotlinVersion = LanguageVersion.KOTLIN_1_5

        assertThat(environment.configuration.languageVersionSettings.languageVersion).isSameAs(expectedKotlinVersion)
        assertThat(logTester.logs(LoggerLevel.WARN)).isEmpty()
        assertThat(logTester.logs(LoggerLevel.DEBUG))
            .contains("Using Kotlin ${expectedKotlinVersion.versionString} to parse source code")
    }

    @Test
    fun `not setting the amount of threads to use explicitly will not set anything in the environment`() {
        logTester.setLevel(LoggerLevel.DEBUG)

        val sensorContext = mockk<SensorContext> {
            every { config() } returns ConfigurationBridge(MapSettings())
        }

        val environment = environment(sensorContext)

        assertThat(environment.configuration.get(CommonConfigurationKeys.PARALLEL_BACKEND_THREADS)).isNull()
        assertThat(logTester.logs(LoggerLevel.WARN)).isEmpty()
        assertThat(logTester.logs(LoggerLevel.DEBUG))
            .contains("Using the default amount of threads")
    }

    @Test
    fun `setting the amount of threads to use is reflected in the environment`() {
        logTester.setLevel(LoggerLevel.DEBUG)

        val sensorContext = mockk<SensorContext> {
            every { config() } returns ConfigurationBridge(MapSettings().apply {
                setProperty(KotlinPlugin.COMPILER_THREAD_COUNT_PROPERTY, "42")
            })
        }

        val environment = environment(sensorContext)

        assertThat(environment.configuration.get(CommonConfigurationKeys.PARALLEL_BACKEND_THREADS)).isEqualTo(42)
        assertThat(logTester.logs(LoggerLevel.WARN)).isEmpty()
        assertThat(logTester.logs(LoggerLevel.DEBUG))
            .contains("Using 42 threads")
    }

    @Test
    fun `setting the amount of threads to use to an invalid integer value produces warning`() {
        logTester.setLevel(LoggerLevel.DEBUG)

        val sensorContext = mockk<SensorContext> {
            every { config() } returns ConfigurationBridge(MapSettings().apply {
                setProperty(KotlinPlugin.COMPILER_THREAD_COUNT_PROPERTY, "0")
            })
        }

        val environment = environment(sensorContext)

        assertThat(environment.configuration.get(CommonConfigurationKeys.PARALLEL_BACKEND_THREADS)).isNull()
        assertThat(logTester.logs(LoggerLevel.WARN))
            .containsExactly("Invalid amount of threads specified for ${KotlinPlugin.COMPILER_THREAD_COUNT_PROPERTY}: '0'.")
        assertThat(logTester.logs(LoggerLevel.DEBUG))
            .contains("Using the default amount of threads")
    }

    @Test
    fun `setting the amount of threads to use to an invalid non-integer value produces warning`() {
        logTester.setLevel(LoggerLevel.DEBUG)

        val sensorContext = mockk<SensorContext> {
            every { config() } returns ConfigurationBridge(MapSettings().apply {
                setProperty(KotlinPlugin.COMPILER_THREAD_COUNT_PROPERTY, "foo")
            })
        }

        val environment = environment(sensorContext)

        assertThat(environment.configuration.get(CommonConfigurationKeys.PARALLEL_BACKEND_THREADS)).isNull()
        assertThat(logTester.logs(LoggerLevel.WARN))
            .containsExactly("${KotlinPlugin.COMPILER_THREAD_COUNT_PROPERTY} needs to be set to an integer value. Could not interpret 'foo' as integer.")
        assertThat(logTester.logs(LoggerLevel.DEBUG))
            .contains("Using the default amount of threads")
    }


    @Test
    fun `the kotlin sensor optimizes analyses in contexts where sonar-kotlin-skipUnchanged is true`() {
        context.setCanSkipUnchangedFiles(false)

        // Enable analysis property to override skipUnchanged setting
        context.settings().setProperty("sonar.kotlin.skipUnchanged", "true")

        assertAnalysisIsIncremental()
    }

    @Test
    fun `the kotlin sensor does not optimize analyses in contexts when the sonar-kotlin-skipUnchanged is false`() {
        context.setCanSkipUnchangedFiles(true)

        // Explicitly prevent the skipping of unchanged files
        context.settings().setProperty("sonar.kotlin.skipUnchanged", "false")

        assertAnalysisIsNotIncremental()
    }

    @Test
    fun `the kotlin sensor does not optimize analyses in contexts where it is not appropriate`() {
        context.setCanSkipUnchangedFiles(false)
        assertAnalysisIsNotIncremental()
    }

    @Test
    fun `the kotlin sensor optimizes analyses in contexts where this is appropriate`() {
        context.setCanSkipUnchangedFiles(true)
        assertAnalysisIsIncremental()
    }

    @Test
    fun `the kotlin sensor defaults to not optimizing in case the API implementation is out of date (AbstractMethodError)`() {
        context = spyk(context) {
            every { canSkipUnchangedFiles() } throws AbstractMethodError()
        }

        assertAnalysisIsNotIncremental()

        verify(exactly = 1) { context.canSkipUnchangedFiles() }
    }

    @Test
    fun `the kotlin sensor defaults to not optimizing in case the API implementation is out of date (NoSuchMethodError)`() {
        context = spyk(context) {
            every { canSkipUnchangedFiles() } throws NoSuchMethodError()
        }

        assertAnalysisIsNotIncremental()

        verify(exactly = 1) { context.canSkipUnchangedFiles() }
    }

    private fun assertAnalysisIsIncremental() {
        logTester.setLevel(LoggerLevel.DEBUG)
        val files = incrementalAnalysisFileSet()
        val addedFile = files[InputFile.Status.ADDED]
        val changedFile = files[InputFile.Status.CHANGED]
        files.values.forEach { context.fileSystem().add(it) }

        val checkFactory = checkFactory("S1764")
        sensor(checkFactory).execute(context)
        val issueIterator = context.allIssues().iterator()
        val firstIssue = issueIterator.next()
        val secondIssue = issueIterator.next()
        assertThrows<NoSuchElementException>("There should be no more than 2 issues from this analysis") {
            issueIterator.next()
        }
        assertThat(firstIssue.ruleKey().rule()).isEqualTo("S1764")
        assertThat(secondIssue.ruleKey().rule()).isEqualTo("S1764")
        val locationOfFirstIssue = firstIssue.primaryLocation()
        assertThat(locationOfFirstIssue.inputComponent()).isEqualTo(changedFile)
        assertThat(locationOfFirstIssue.message())
            .isEqualTo("Correct one of the identical sub-expressions on both sides this operator.")

        val locationOfSecondIssue = secondIssue.primaryLocation()
        assertThat(locationOfSecondIssue.inputComponent()).isEqualTo(addedFile)
        assertThat(locationOfSecondIssue.message())
            .isEqualTo("Correct one of the identical sub-expressions on both sides this operator.")

        assertThat(logTester.logs(LoggerLevel.DEBUG))
            .contains("The Kotlin analyzer is running in a context where it can skip unchanged files.")
        assertThat(logTester.logs(LoggerLevel.INFO))
            .contains("Only analyzing 2 changed Kotlin files out of 3.")
    }

    private fun assertAnalysisIsNotIncremental() {
        val files = incrementalAnalysisFileSet()
        val addedFile = files[InputFile.Status.ADDED]
        val changedFile = files[InputFile.Status.CHANGED]
        val unchangedFile = files[InputFile.Status.SAME]
        files.values.forEach { context.fileSystem().add(it) }

        val checkFactory = checkFactory("S1764")
        sensor(checkFactory).execute(context)
        val issueIterator = context.allIssues().iterator()
        val firstIssue = issueIterator.next()
        val secondIssue = issueIterator.next()
        val thirdIssue = issueIterator.next()
        assertThrows<NoSuchElementException>("There should be no more than 2 issues from this analysis") {
            issueIterator.next()
        }
        assertThat(firstIssue.ruleKey().rule()).isEqualTo("S1764")
        assertThat(secondIssue.ruleKey().rule()).isEqualTo("S1764")
        assertThat(thirdIssue.ruleKey().rule()).isEqualTo("S1764")

        val locationOfFirstIssue = firstIssue.primaryLocation()
        assertThat(locationOfFirstIssue.inputComponent()).isEqualTo(changedFile)
        assertThat(locationOfFirstIssue.message())
            .isEqualTo("Correct one of the identical sub-expressions on both sides this operator.")

        val locationOfSecondIssue = secondIssue.primaryLocation()
        assertThat(locationOfSecondIssue.inputComponent()).isEqualTo(addedFile)
        assertThat(locationOfSecondIssue.message())
            .isEqualTo("Correct one of the identical sub-expressions on both sides this operator.")

        val locationOfThirdIssue = thirdIssue.primaryLocation()
        assertThat(locationOfThirdIssue.inputComponent()).isEqualTo(unchangedFile)
        assertThat(locationOfThirdIssue.message())
            .isEqualTo("Correct one of the identical sub-expressions on both sides this operator.")

        assertThat(logTester.logs())
            .doesNotContain("The Kotlin analyzer is working in a context where it can skip unchanged files.")
    }

    private fun incrementalAnalysisFileSet(): Map<InputFile.Status, InputFile> {
        val changedFile = createInputFile(
            "changed.kt",
            """
                fun main(args: Array<String>) {
                    print (1 == 1);
                }
                """.trimIndent(),
            status = InputFile.Status.CHANGED
        )
        val addedFile = createInputFile(
            "added.kt",
            """
                fun isAlsoIdentical(input: Int): Boolean = input == input
                """.trimIndent(),
            status = InputFile.Status.ADDED
        )
        val unchangedFile = createInputFile(
            "unchanged.kt",
            """
                fun isIdentical(input: Int): Boolean = input == input
                """.trimIndent(),
            status = InputFile.Status.SAME
        )
        return mapOf(
            InputFile.Status.ADDED to addedFile,
            InputFile.Status.CHANGED to changedFile,
            InputFile.Status.SAME to unchangedFile,
        )
    }


    private fun sensor(checkFactory: CheckFactory): KotlinSensor {
        return KotlinSensor(checkFactory, fileLinesContextFactory, DefaultNoSonarFilter(), language())
    }
}

@Rule(key = "ETRule1")
internal class ExceptionThrowingCheck : AbstractCheck() {
    override fun visitNamedFunction(function: KtNamedFunction, data: KotlinFileContext?) {
        throw TestException("This is a test message")
    }
}

internal class TestException(msg: String) : Exception(msg)
