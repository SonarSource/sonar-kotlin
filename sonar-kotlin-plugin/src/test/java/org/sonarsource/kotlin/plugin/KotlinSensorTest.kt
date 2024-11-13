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
package org.sonarsource.kotlin.plugin

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.rule.CheckFactory
import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.batch.sensor.cache.WriteCache
import org.sonar.api.batch.sensor.highlighting.TypeOfText
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor
import org.sonar.api.batch.sensor.internal.SensorContextTester
import org.sonar.api.batch.sensor.issue.internal.DefaultNoSonarFilter
import org.sonar.api.config.internal.ConfigurationBridge
import org.sonar.api.config.internal.MapSettings
import org.sonar.api.internal.SonarRuntimeImpl
import org.sonar.api.measures.CoreMetrics
import org.sonar.api.utils.Version
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.common.FAIL_FAST_PROPERTY_NAME
import org.sonarsource.kotlin.api.common.KOTLIN_LANGUAGE_VERSION
import org.sonarsource.kotlin.api.common.SONAR_ANDROID_DETECTED
import org.sonarsource.kotlin.api.common.SONAR_JAVA_BINARIES
import org.sonarsource.kotlin.api.frontend.Environment
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.frontend.analyzeAndGetBindingContext
import org.sonarsource.kotlin.api.sensors.environment
import org.sonarsource.kotlin.plugin.caching.contentHashKey
import org.sonarsource.kotlin.plugin.cpd.computeCPDTokensCacheKey
import org.sonarsource.kotlin.testapi.AbstractSensorTest
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import kotlin.time.ExperimentalTime

private val LOG = LoggerFactory.getLogger(KotlinSensor::class.java)

@ExperimentalTime
internal class KotlinSensorTest : AbstractSensorTest() {

    @AfterEach
    fun cleanupMocks() {
        unmockkAll()
    }

    @Test
    fun testDescribe() {
        val checkFactory = checkFactory("S1764")

        val descriptor = DefaultSensorDescriptor()
        sensor(checkFactory).describe(descriptor)
        assertThat(descriptor.name()).isEqualTo("Kotlin Sensor")
        assertThat(descriptor.languages()).isEqualTo(listOf("kotlin"))
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
        assertThat(issues).hasSize(1)
        val issue = issues.iterator().next()
        assertThat(issue.ruleKey().rule()).isEqualTo("S1764")
        val location = issue.primaryLocation()
        assertThat(location.inputComponent()).isEqualTo(inputFile)
        assertThat(location.message())
            .isEqualTo("Correct one of the identical sub-expressions on both sides this operator.")
        assertTextRange(location.textRange()).hasRange(2, 12, 2, 13)
    }

    @Test
    fun test_no_rules_executed_for_Kotlin_scripts() {
        val inputFile = createInputFile(
            "file1.kts", """
            print (1 == 1)
            """.trimIndent()
        )
        context.fileSystem().add(inputFile)
        val checkFactory = checkFactory("S1764")
        sensor(checkFactory).execute(context)
        val issues = context.allIssues()
        assertThat(issues).isEmpty()
    }

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
        assertThat(issues).hasSize(1)
        val issue = issues.iterator().next()
        assertThat(issue.ruleKey().rule()).isEqualTo("S125")
        val location = issue.primaryLocation()
        assertThat(location.inputComponent()).isEqualTo(inputFile)
        assertThat(location.message()).isEqualTo("Remove this commented out code.")
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
        assertThat(context.highlightingTypeAt(inputFile.key(), 1, 0)).containsExactly(TypeOfText.KEYWORD)
        assertThat(context.highlightingTypeAt(inputFile.key(), 1, 3)).isEmpty()
        assertThat(context.measure(inputFile.key(), CoreMetrics.NCLOC).value()).isEqualTo(3)
        assertThat(context.measure(inputFile.key(), CoreMetrics.COMMENT_LINES).value()).isZero
        assertThat(context.measure(inputFile.key(), CoreMetrics.FUNCTIONS).value()).isEqualTo(1)
        assertThat(context.measure(inputFile.key(), CoreMetrics.CLASSES).value()).isEqualTo(1)
        assertThat(context.cpdTokens(inputFile.key())!![1].value)
            .isEqualTo("print(1==1);print(\"LITERAL\");}")
        assertThat(context.measure(inputFile.key(), CoreMetrics.COMPLEXITY).value()).isEqualTo(1)
        assertThat(context.measure(inputFile.key(), CoreMetrics.STATEMENTS).value()).isEqualTo(2)

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
        assertThat(issues).isEmpty()
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
        assertThat(issues).hasSize(2)
    }

    @Test
    fun `Ensure compiler crashes during BindingContext generation don't crash engine`() {
        context.setCanSkipUnchangedFiles(false)
        executeAnalysisWithInvalidBindingContext()
        assertThat(logTester.logs(Level.ERROR)).containsExactly("Could not generate binding context. Proceeding without semantics.")
    }

    @Test
    fun `BindingContext generation does not crash when there are no files to analyze`() {
        context.setCanSkipUnchangedFiles(true)
        executeAnalysisWithInvalidBindingContext()
        assertThat(logTester.logs(Level.ERROR)).isEmpty()
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
        populateCacheWithExpectedEntries(listOf(inputFile), context)
        mockkStatic("org.sonarsource.kotlin.api.sensors.AbstractKotlinSensorExecuteContextKt")
        every { environment(any(), any()) } returns Environment(listOf("file1.kt"), LanguageVersion.LATEST_STABLE)
        mockkStatic("org.sonarsource.kotlin.api.frontend.KotlinCoreEnvironmentToolsKt")
        every { analyzeAndGetBindingContext(any(), any()) } throws IOException("Boom!")

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
    fun test_fail_reading() {
        val inputFile = spyk(createInputFile("file1.kt", "class A { fun f() = TODO() }"))
        context.fileSystem().add(inputFile)
        every { inputFile.contents() } throws IOException("Can't read")
        every { inputFile.toString() } returns "file1.kt"

        val checkFactory = checkFactory("S1764")
        sensor(checkFactory).execute(context)
        val analysisErrors = context.allAnalysisErrors()
        assertThat(analysisErrors).hasSize(1)
        val analysisError = analysisErrors.iterator().next()
        assertThat(analysisError.inputFile()).isEqualTo(inputFile)
        assertThat(analysisError.message()).isEqualTo("Unable to parse file: file1.kt")
        val textPointer = analysisError.location()
        assertThat(textPointer).isNull()

        assertThat(logTester.logs(Level.ERROR)).contains("Cannot read 'file1.kt': Can't read")
    }

    @Test
    fun test_with_classpath() {
        val settings = MapSettings()
        settings.setProperty(SONAR_JAVA_BINARIES, "classes/")
        context.setSettings(settings)
        val inputFile = createInputFile("file1.kt", "class A { fun f() = TODO() }")
        context.fileSystem().add(inputFile)
        val checkFactory = checkFactory("S1764")
        sensor(checkFactory).execute(context)
        val analysisErrors = context.allAnalysisErrors()
        assertThat(analysisErrors).isEmpty()
    }

    @Test
    fun test_with_blank_classpath() {
        val settings = MapSettings()
        settings.setProperty(SONAR_JAVA_BINARIES, " ")
        context.setSettings(settings)
        val inputFile = createInputFile("file1.kt", "class A { fun f() = TODO() }")
        context.fileSystem().add(inputFile)
        val checkFactory = checkFactory("S1764")
        sensor(checkFactory).execute(context)
        val analysisErrors = context.allAnalysisErrors()
        assertThat(analysisErrors).isEmpty()
    }

    private fun failFastSensorWithEnvironmentSetup(failFast: Boolean?): KotlinSensor {
        mockkStatic("org.sonarsource.kotlin.plugin.KotlinCheckListKt")
        every { KOTLIN_CHECKS } returns listOf(ExceptionThrowingCheck::class.java)

        context.apply {
            setSettings(MapSettings().apply {
                failFast?.let { setProperty(FAIL_FAST_PROPERTY_NAME, it) }
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

        assertThat(logTester.logs(Level.ERROR))
            .containsExactly("Cannot analyse 'file1.kt' with 'KtChecksVisitor': This is a test message")
    }

    @Test
    fun `ensure failFast is not triggering when set to false`() {
        val sensor = failFastSensorWithEnvironmentSetup(false)
        assertDoesNotThrow { sensor.execute(context) }
        assertThat(logTester.logs(Level.ERROR))
            .containsExactly("Cannot analyse 'file1.kt' with 'KtChecksVisitor': This is a test message")
    }

    @Test
    fun `ensure failFast is not triggered when not set at all`() {
        val sensor = failFastSensorWithEnvironmentSetup(null)
        assertDoesNotThrow { sensor.execute(context) }
        assertThat(logTester.logs(Level.ERROR))
            .containsExactly("Cannot analyse 'file1.kt' with 'KtChecksVisitor': This is a test message")
    }

    @Test
    fun `not setting the kotlin version analyzer property results in Environment with the default Kotlin version`() {
        logTester.setLevel(Level.DEBUG)

        val sensorContext = mockk<SensorContext> {
            every { config() } returns ConfigurationBridge(MapSettings())
        }

        val environment = environment(sensorContext, LOG)

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

        val environment = environment(sensorContext, LOG)

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

        val environment = environment(sensorContext, LOG)

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

        val environment = environment(sensorContext, LOG)

        val expectedKotlinVersion = LanguageVersion.LATEST_STABLE

        assertThat(environment.configuration.languageVersionSettings.languageVersion).isSameAs(expectedKotlinVersion)
        assertThat(logTester.logs(Level.WARN)).isEmpty()
        assertThat(logTester.logs(Level.DEBUG))
            .contains("Using Kotlin ${expectedKotlinVersion.versionString} to parse source code")
    }

    @Test
    fun `setting android_detected property to true triggers AndroidOnly check`() {
        val sensor = prepareSensorForAndroid(androidDetected = "true")

        sensor.execute(context)

        assertThat(context.allIssues()).hasSize(1)
    }

    @Test
    fun `setting android_detected property to false doesn't trigger AndroidOnly check`() {
        val sensor = prepareSensorForAndroid(androidDetected = "false")

        sensor.execute(context)

        assertThat(context.allIssues()).isEmpty()
    }

    @Test
    fun `setting android_detected property to other value doesn't trigger AndroidOnly check`() {
        val sensor = prepareSensorForAndroid(androidDetected = "other")

        sensor.execute(context)

        assertThat(context.allIssues()).isEmpty()
    }

    @Test
    fun `setting no android_detected property doesn't trigger AndroidOnly check`() {
        val sensor = prepareSensorForAndroid()

        sensor.execute(context)

        assertThat(context.allIssues()).isEmpty()
    }

    private fun prepareSensorForAndroid(androidDetected: String? = null): KotlinSensor {
        mockkStatic("org.sonarsource.kotlin.plugin.KotlinCheckListKt")
        every { KOTLIN_CHECKS } returns listOf(AndroidOnlyCheck::class.java)

        context.apply {
            setSettings(MapSettings().apply {
                androidDetected?.let { setProperty(SONAR_ANDROID_DETECTED, it) }
            })

            fileSystem().add(createInputFile("file1.kt", "class A { fun f() = TODO() }"))
        }
        return sensor(checkFactory("AndroidOnlyRule"))
    }


    @Test
    fun `the kotlin sensor optimizes analyses in contexts where sonar-kotlin-skipUnchanged is true`() {
        logTester.setLevel(Level.DEBUG)
        val files = incrementalAnalysisFileSet()
        // Enable analysis property to override skipUnchanged setting
        context.settings().setProperty("sonar.kotlin.skipUnchanged", "true")
        context.setCanSkipUnchangedFiles(false)
        assertAnalysisIsIncremental(files)
    }

    @Test
    fun `the kotlin sensor does not optimize analysis when the cpd tokens of a file are missing because the cache is disabled`() {
        val files = incrementalAnalysisFileSet()

        // Disable the cache to make the analysis non-incremental
        context.isCacheEnabled = false
        logTester.setLevel(Level.DEBUG)

        assertAnalysisIsNotIncremental(files)
    }

    @Test
    fun `the kotlin sensor does not optimize analysis when the cpd tokens of a file are missing from the previous analysis cache`() {
        val files = incrementalAnalysisFileSet()

        // Clear the cache from the expected CPD tokens
        val emptyReadCache = DummyReadCache(emptyMap())
        val writeCache = DummyWriteCache(readCache = emptyReadCache)
        context.setPreviousCache(emptyReadCache)
        context.setNextCache(writeCache)
        logTester.setLevel(Level.DEBUG)

        assertAnalysisIsNotIncremental(files)
    }

    @Test
    fun `the kotlin sensor does not optimize analysis when the cpd tokens cannot be copied from the previous cache to the next one`() {
        val files = incrementalAnalysisFileSet()

        // Clear the cache of CPD tokens
        val emptyReadCache = DummyReadCache(emptyMap())
        val writeCache = DummyWriteCache(readCache = emptyReadCache)
        context.setPreviousCache(emptyReadCache)
        context.setNextCache(writeCache)
        logTester.setLevel(Level.DEBUG)

        // Mock exception throwing when copying the tokens from the cache of the previous analysis to the next one
        val unchangedFile = files[InputFile.Status.SAME]!!
        val cacheKey = computeCPDTokensCacheKey(unchangedFile)
        val mockedWriteCache = mockk<WriteCache>()
        every { mockedWriteCache.copyFromPrevious(cacheKey) } throws IllegalArgumentException()
        every { mockedWriteCache.write(any(), any<InputStream>()) } returns Unit
        every { mockedWriteCache.write(any(), any<ByteArray>()) } returns Unit
        context.setNextCache(mockedWriteCache)

        assertAnalysisIsNotIncremental(files)
    }

    @Test
    fun `the kotlin sensor optimizes analysis when the cpd tokens cannot be loaded from the previous cache but cannot be copied to the next cache`() {
        logTester.setLevel(Level.TRACE)

        val files = incrementalAnalysisFileSet()

        // Mock exception throwing when copying the tokens from the cache of the previous analysis to the next one
        val unchangedFile = files[InputFile.Status.SAME]!!
        val cpdCacheKey = computeCPDTokensCacheKey(unchangedFile)
        val mockedWriteCache = mockk<WriteCache>()
        every { mockedWriteCache.copyFromPrevious(any()) } throws IllegalArgumentException()
        every { mockedWriteCache.copyFromPrevious(cpdCacheKey) } throws IllegalArgumentException()
        every { mockedWriteCache.write(any(), any<ByteArray>()) } returns Unit
        context.setNextCache(mockedWriteCache)

        assertAnalysisIsIncremental(files)
        // Check that the exception has been logged
        assertThat(logTester.logs(Level.TRACE)).contains("Unable to save the CPD tokens of file unchanged.kt for the next analysis.")
    }

    @Test
    fun `the kotlin sensor does not optimize analyses in contexts when the sonar-kotlin-skipUnchanged is false`() {
        val files = incrementalAnalysisFileSet()
        // Explicitly prevent the skipping of unchanged files
        context.settings().setProperty("sonar.kotlin.skipUnchanged", "false")
        assertAnalysisIsNotIncremental(files)
    }

    @Test
    fun `the kotlin sensor does not optimize analyses in contexts where it is not appropriate`() {
        val files = incrementalAnalysisFileSet()
        context.setCanSkipUnchangedFiles(false)
        assertAnalysisIsNotIncremental(files)
    }

    @Test
    fun `the kotlin sensor optimizes analyses in contexts where this is appropriate`() {
        logTester.setLevel(Level.DEBUG)
        val files = incrementalAnalysisFileSet()
        assertAnalysisIsIncremental(files)
    }

    @Test
    fun `the kotlin sensor defaults to not optimizing in case the API implementation is out of date (AbstractMethodError)`() {
        context = spyk(context) {
            every { canSkipUnchangedFiles() } throws AbstractMethodError()
        }

        val files = incrementalAnalysisFileSet()
        assertAnalysisIsNotIncremental(files)

        verify(exactly = 1) { context.canSkipUnchangedFiles() }
    }

    @Test
    fun `the kotlin sensor defaults to not optimizing in case the API implementation is out of date (NoSuchMethodError)`() {
        context = spyk(context) {
            every { canSkipUnchangedFiles() } throws NoSuchMethodError()
        }

        val files = incrementalAnalysisFileSet()
        assertAnalysisIsNotIncremental(files)

        verify(exactly = 1) { context.canSkipUnchangedFiles() }
    }

    @Test
    fun `test sensor skips cached files`() {
        logTester.setLevel(Level.DEBUG)

        val files = incrementalAnalysisFileSet()
        assertAnalysisIsIncremental(files)

        assertThat(logTester.logs(Level.DEBUG))
            .contains("Content hash cache was initialized")
        assertThat(logTester.logs(Level.INFO))
            .contains("Only analyzing 2 changed Kotlin files out of 3.")

    }

    @Test
    fun `hasFileChanged falls back on the InputFile status when cache is disabled`() {
        logTester.setLevel(Level.DEBUG)

        val files = incrementalAnalysisFileSet()

        context.isCacheEnabled = false
        context.setCanSkipUnchangedFiles(true)

        val unchangedFile = spyk(files[InputFile.Status.SAME]!!)
        context.fileSystem().add(unchangedFile)
        context.fileSystem().add(files[InputFile.Status.CHANGED]!!)
        val checkFactory = checkFactory("S1764")
        sensor(checkFactory).execute(context)

        // The analysis is not incremental because a disabled cache prevents the reuse of CPD tokens
        assertThat(logTester.logs(Level.DEBUG)).contains("Content hash cache is disabled")
        assertThat(logTester.logs(Level.INFO)).contains("Only analyzing 2 changed Kotlin files out of 2.")
        verify { unchangedFile.status() }
    }

    @Test
    fun `test same file passed twice to content hash cache`() {
        val files = incrementalAnalysisFileSet()
        val key = contentHashKey(files[InputFile.Status.CHANGED]!!)
        val messageDigest = MessageDigest.getInstance("MD5")
        val readCache = files[InputFile.Status.CHANGED]!!.contents().byteInputStream().use {
            DummyReadCache(mapOf(key to messageDigest.digest(it.readAllBytes())))
        }
        val writeCache = DummyWriteCache(readCache = readCache)
        context.setNextCache(writeCache)
        context.setPreviousCache(readCache)
        val changedFile = files[InputFile.Status.CHANGED]!!
        context.fileSystem().add(changedFile)
        val sameKeyFile = spyk(files[InputFile.Status.SAME]!!)
        every { sameKeyFile.key() } returns changedFile.key()
        every { sameKeyFile.contents() } returns changedFile.contents()
        context.fileSystem().add(sameKeyFile)
        val checkFactory = checkFactory("S1764")
        sensor(checkFactory).execute(context)
        assertThat(logTester.logs(Level.WARN)).contains("Cannot copy key $key from cache as it has already been written")
    }

    @Test
    fun `the kotlin sensor does not optimize analysis when running in SonarLint`() {
        val files = incrementalAnalysisFileSet()
        context.setCanSkipUnchangedFiles(true)
        val sonarLintRuntime = SonarRuntimeImpl.forSonarLint(Version.create(7, 0))
        context.setRuntime(sonarLintRuntime)
        assertAnalysisIsNotIncremental(files)
    }

    private fun assertAnalysisIsIncremental(files: Map<InputFile.Status, InputFile>) {
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

        assertThat(logTester.logs(Level.DEBUG))
            .contains("The Kotlin analyzer is running in a context where it can skip unchanged files.")
        assertThat(logTester.logs(Level.INFO))
            .contains("Only analyzing 2 changed Kotlin files out of 3.")
    }

    private fun assertAnalysisIsNotIncremental(files: Map<InputFile.Status, InputFile>) {
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
        context.setCanSkipUnchangedFiles(true)
        context.isCacheEnabled = false
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
        val files = mapOf(
            InputFile.Status.ADDED to addedFile,
            InputFile.Status.CHANGED to changedFile,
            InputFile.Status.SAME to unchangedFile,
        )
        populateCacheWithExpectedEntries(files.values, context)
        return files
    }

    private fun populateCacheWithExpectedEntries(files: Iterable<InputFile>, context: SensorContextTester) {
        val cacheContentBeforeAnalysis = mutableMapOf<String, ByteArray>()
        val messageDigest = MessageDigest.getInstance("MD5")
        files
            .filter { it.status() != InputFile.Status.ADDED }
            .forEach {
                // Add content hashes
                val contentHashKey = contentHashKey(it)
                if (it.status() == InputFile.Status.SAME) {
                    val digest = messageDigest.digest(it.contents().byteInputStream().readAllBytes())
                    cacheContentBeforeAnalysis[contentHashKey] = digest
                } else if (it.status() == InputFile.Status.CHANGED) {
                    cacheContentBeforeAnalysis[contentHashKey] = ByteArray(0)
                }
                // Add CPD tokens
                cacheContentBeforeAnalysis["kotlin:cpdTokens:${it.key()}"] = ByteArray(0)
            }

        context.isCacheEnabled = true
        if (context.previousCache() != null) {
            val readCache = context.previousCache() as DummyReadCache
            readCache.cache.entries.forEach { cacheContentBeforeAnalysis[it.key] = it.value }
        }

        val previousCache = DummyReadCache(cacheContentBeforeAnalysis)
        val nextCache = DummyWriteCache(readCache = previousCache)
        context.setPreviousCache(previousCache)
        context.setNextCache(nextCache)
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

@Rule(key = "AndroidOnlyRule")
internal class AndroidOnlyCheck : AbstractCheck() {
    override fun visitNamedFunction(function: KtNamedFunction, kfc: KotlinFileContext) {
        if (kfc.isInAndroid()) {
            kfc.reportIssue(function.nameIdentifier!!, "Boom!")
        }
    }
}

internal class TestException(msg: String) : Exception(msg)
