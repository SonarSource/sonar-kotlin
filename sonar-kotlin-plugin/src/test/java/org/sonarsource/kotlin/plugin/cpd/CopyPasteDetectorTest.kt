/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2026 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1.0.1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonarsource.kotlin.plugin.cpd

import com.intellij.openapi.util.Disposer
import org.assertj.core.api.Assertions
import org.assertj.core.api.ObjectAssert
import org.jetbrains.kotlin.config.LanguageVersion
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.api.io.TempDir
import org.slf4j.event.Level
import org.sonar.api.batch.fs.internal.TestInputFileBuilder
import org.sonar.api.batch.sensor.cpd.internal.TokensLine
import org.sonar.api.batch.sensor.internal.SensorContextTester
import org.sonar.api.testfixtures.log.LogTesterJUnit5
import org.sonarsource.kotlin.plugin.DummyReadCache
import org.sonarsource.kotlin.plugin.DummyWriteCache
import org.sonarsource.kotlin.api.checks.InputFileContextImpl
import org.sonarsource.kotlin.api.frontend.Environment
import org.sonarsource.kotlin.testapi.kotlinTreeOf
import java.nio.file.Path
import kotlin.io.path.createFile
import kotlin.io.path.name

private val content = """
        /*
         * some licence header for example
         */
        package foo.bar
        
        import java.nio.file.Path
        
        class Bar {
            fun foo() {
                println("something")
            }
        }
        """.trimIndent()

class CopyPasteDetectorTest {

    private val disposable = Disposer.newDisposable()

    @AfterEach
    fun dispose() {
        Disposer.dispose(disposable)
    }

    @JvmField
    @TempDir
    var tmpFolder: Path? = null

    @JvmField
    @RegisterExtension
    var logTester = LogTesterJUnit5()

    @Test
    fun test() {
        val tmpFile = tmpFolder!!.resolve("dummy.kt").createFile()

        val sensorContext: SensorContextTester = SensorContextTester.create(tmpFolder!!.root)
        val inputFile = TestInputFileBuilder("moduleKey", tmpFile.name)
            .setModuleBaseDir(tmpFile.parent)
            .setContents(content)
            .build()

        val root = kotlinTreeOf(content, Environment(disposable, emptyList(), LanguageVersion.LATEST_STABLE), inputFile)
        val ctx = InputFileContextImpl(sensorContext, inputFile, false)
        CopyPasteDetector().scan(ctx, root)

        val cpdTokenLines = sensorContext.cpdTokens(inputFile.key())!!

        Assertions.assertThat(cpdTokenLines).hasSize(5)

        assertThat(cpdTokenLines[0])
            .hasValue("classBar{")
            .hasStartLine(8)
            .hasStartUnit(1)
            .hasEndUnit(3)

        assertThat(cpdTokenLines[1])
            .hasValue("funfoo(){")
            .hasStartLine(9)
            .hasStartUnit(4)
            .hasEndUnit(8)

        assertThat(cpdTokenLines[2])
            .hasValue("""println(LITERAL)""")
            .hasStartLine(10)
            .hasStartUnit(9)
            .hasEndUnit(12)

        assertThat(cpdTokenLines[3])
            .hasValue("}")
            .hasStartLine(11)
            .hasStartUnit(13)
            .hasEndUnit(13)

        assertThat(cpdTokenLines[4])
            .hasValue("}")
            .hasStartLine(12)
            .hasStartUnit(14)
            .hasEndUnit(14)
    }

    @Test
    fun `cpd tokens are saved for the next analysis when the cache is enabled`() {
        logTester.setLevel(Level.TRACE)

        val tmpFile = tmpFolder!!.resolve("dummy.kt").createFile()

        val sensorContext: SensorContextTester = SensorContextTester.create(tmpFolder!!.root)
        val inputFile = TestInputFileBuilder("moduleKey", tmpFile.name)
            .setModuleBaseDir(tmpFile.parent)
            .setContents(content)
            .build()

        val root = kotlinTreeOf(content, Environment(disposable, emptyList(), LanguageVersion.LATEST_STABLE), inputFile)
        val ctx = InputFileContextImpl(sensorContext, inputFile, false)

        val readCache = DummyReadCache(emptyMap())
        val writeCache = DummyWriteCache(readCache = readCache)
        sensorContext.isCacheEnabled = true
        sensorContext.setPreviousCache(readCache)
        sensorContext.setNextCache(writeCache)

        Assertions.assertThat(writeCache.cache).isEmpty()
        CopyPasteDetector().scan(ctx, root)
        Assertions.assertThat(writeCache.cache)
            .hasSize(1)

        val logs = logTester.getLogs(Level.TRACE).map { it.rawMsg }

        Assertions.assertThat(logs)
            .hasSize(1)
            .containsExactly("Caching 14 CPD tokens for next analysis of input file moduleKey:dummy.kt.")
    }

    @Test
    fun `cpd tokens are not saved for the next analysis when the cache is disabled`() {
        logTester.setLevel(Level.TRACE)

        val tmpFile = tmpFolder!!.resolve("dummy.kt").createFile()

        val sensorContext: SensorContextTester = SensorContextTester.create(tmpFolder!!.root)
        val inputFile = TestInputFileBuilder("moduleKey", tmpFile.name)
            .setModuleBaseDir(tmpFile.parent)
            .setContents(content)
            .build()

        val root = kotlinTreeOf(content, Environment(disposable, emptyList(), LanguageVersion.LATEST_STABLE), inputFile)
        val ctx = InputFileContextImpl(sensorContext, inputFile, false)

        val readCache = DummyReadCache(emptyMap())
        val writeCache = DummyWriteCache(readCache = readCache)
        sensorContext.isCacheEnabled = false
        sensorContext.setPreviousCache(readCache)
        sensorContext.setNextCache(writeCache)

        Assertions.assertThat(writeCache.cache).isEmpty()
        CopyPasteDetector().scan(ctx, root)
        Assertions.assertThat(writeCache.cache).isEmpty()

        val logs = logTester.getLogs(Level.TRACE).map { it.rawMsg }

        Assertions.assertThat(logs)
            .hasSize(1)
            .containsExactly("No CPD tokens cached for next analysis of input file moduleKey:dummy.kt.")
    }

    private val d = "$"
    private val tq = "\"\"\""

    @TestFactory
    fun `cpd tokens`() = listOf(
        Triple("int literal", """ val x = 42 """, "valx=42"),
        Triple("long literal", """ val x = 42L """, "valx=42L"),
        Triple("float literal", """ val x = 42.0f """, "valx=42.0f"),
        Triple("double literal", """ val x = 42.0 """, "valx=42.0"),
        Triple("char literal", """ val x = 'a' """, "valx='a'"),
        Triple("null literal", """ val x = null """, "valx=null"),
        Triple("double-quote string literal", """ val x = "a" """, "valx=LITERAL"),
        Triple("double-quote string literal concatenation", """ val x = "a" + "b" """, "valx=LITERAL+LITERAL"),
        Triple("double-quote string template", """ val x = "a $d{1}" """, "valx=LITERAL"),
        Triple("triple-quote string literal", """ val x = ${tq}a${tq} """, "valx=LITERAL"),
        Triple("triple-quote string literal concatenation", """ val x = ${tq}a${tq} + ${tq}b${tq} """, "valx=LITERAL+LITERAL"),
        Triple("triple-quote string template", """ val x = ${tq}a $d{1}${tq} """, "valx=LITERAL"),
        Triple("mixed-quote string literal concatenation", """ val x = "a" + ${tq}b${tq} """, "valx=LITERAL+LITERAL"),
        Triple(
            "triple-quote string template with interpolated vars",
            """
            val noInterpolations = "a literal"
            val doubleQuoteTwoInterpolations = "$d{x} $d{x}"
            val tripleQuoteTwoInterpolations = $tq$d{noInterpolations} $d{doubleQuoteTwoInterpolations}${tq}
            var nestedInterpolations = $tq$d{ "$d{1 + 1}" }${tq}
            """.trimIndent(),
            """
            valnoInterpolations=LITERAL
            valdoubleQuoteTwoInterpolations=LITERAL
            valtripleQuoteTwoInterpolations=LITERAL
            varnestedInterpolations=LITERAL
            """.trimIndent()
            )
    ).map { (title, input, expected) ->
        DynamicTest.dynamicTest("with $title") {
            val sensorContext: SensorContextTester = SensorContextTester.create(tmpFolder!!.root)
            val inputFile = TestInputFileBuilder("moduleKey", "test.kt").setModuleBaseDir(Path.of(".")).setContents(input).build()
            val root = kotlinTreeOf(input, Environment(disposable, emptyList(), LanguageVersion.LATEST_STABLE), inputFile)
            val ctx = InputFileContextImpl(sensorContext, inputFile, false)
            CopyPasteDetector().scan(ctx, root)

            val cpdTokenLines = sensorContext.cpdTokens(inputFile.key())!!
            val tokensStringified = cpdTokenLines.joinToString(separator = "\n", transform = { it.value })
            Assertions.assertThat(tokensStringified).isEqualTo(expected)
        }
    }
}


private class TokensLineAssert(actual: TokensLine) : ObjectAssert<TokensLine>(actual) {
    fun hasValue(expected: String) = also {
        Assertions.assertThat(actual.value).isEqualTo(expected)
    }

    fun hasStartLine(expected: Int) = also {
        Assertions.assertThat(actual.startLine).isEqualTo(expected)
    }

    fun hasStartUnit(expected: Int) = also {
        Assertions.assertThat(actual.startUnit).isEqualTo(expected)
    }

    fun hasEndUnit(expected: Int) = also {
        Assertions.assertThat(actual.endUnit).isEqualTo(expected)
    }
}

private fun assertThat(actual: TokensLine): TokensLineAssert {
    return TokensLineAssert(actual)
}
