/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2021 SonarSource SA
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
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.fs.internal.DefaultInputFile
import org.sonar.api.batch.fs.internal.TestInputFileBuilder
import org.sonar.api.batch.sensor.internal.SensorContextTester
import org.sonar.api.issue.NoSonarFilter
import org.sonar.api.measures.CoreMetrics
import org.sonar.api.measures.FileLinesContext
import org.sonar.api.measures.FileLinesContextFactory
import org.sonar.api.measures.Metric
import org.sonarsource.kotlin.converter.Environment
import org.sonarsource.kotlin.utils.kotlinTreeOf
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import kotlin.io.path.createTempFile
import kotlin.io.path.name

internal class MetricVisitorTest {
    private val environment = Environment(emptyList())
    private lateinit var mockNoSonarFilter: NoSonarFilter
    private lateinit var mockFileLinesContext: FileLinesContext
    private lateinit var visitor: MetricVisitor
    private lateinit var sensorContext: SensorContextTester
    private lateinit var inputFile: DefaultInputFile
    private val fileLines = mutableMapOf<String, MutableSet<Int>>()

    @JvmField
    @TempDir
    var tempFolder: Path? = null

    @BeforeEach
    fun setUp() {
        fileLines.clear()

        sensorContext = SensorContextTester.create(tempFolder!!.root)
        mockFileLinesContext = spyk()
        every { mockFileLinesContext.setIntValue(any(), any(), any()) } answers {
            fileLines.computeIfAbsent(args[0] as String) { mutableSetOf() }.add(args[1] as Int)
            callOriginal()
        }

        val mockFileLinesContextFactory = mockk<FileLinesContextFactory>()
        mockNoSonarFilter = spyk()
        every { mockFileLinesContextFactory.createFor(any()) } returns mockFileLinesContext

        visitor = MetricVisitor(mockFileLinesContextFactory, mockNoSonarFilter)
    }

    @AfterEach
    fun cleanup() {
        unmockkAll()
    }

    @Test
    fun linesOfCode() {
        scan(
            """
            fun function1(x: Int) { x + 1 }
            // comment
            fun function1() { // comment
                val x = true || false 
            }
            """.trimIndent()
        )
        Assertions.assertThat(fileLines).containsKey(CoreMetrics.NCLOC_DATA_KEY)
        Assertions.assertThat(fileLines[CoreMetrics.NCLOC_DATA_KEY]).containsExactly(1, 3, 4, 5)
        Assertions.assertThat(getMeasure(CoreMetrics.NCLOC)).isEqualTo(4)
    }

    @Test
    fun commentLines() {
        scan(
            """
            fun function1(x: Int) { x + 1 }
            // comment
            fun function1() { // comment
                val x = true || false
            }
            """.trimIndent()
        )
        Assertions.assertThat(getMeasure(CoreMetrics.COMMENT_LINES)).isEqualTo(2)
    }

    @Test
    fun multiLineComment() {
        scan(
            """
            /*start
            x + 1
            end*/
            """.trimIndent()
        )
        Assertions.assertThat(getMeasure(CoreMetrics.COMMENT_LINES)).isEqualTo(3)
        Assertions.assertThat(getMeasure(CoreMetrics.NCLOC)).isZero
    }

    @Test
    fun nosonarLines() {
        scan(
            """
            fun function1(x: Int) { x + 1 }
            // NOSONAR comment
            fun function2() { // comment
                val x = true || false
            }
            """.trimIndent()
        )
        verify { mockNoSonarFilter invoke "noSonarInFile" withArguments listOf(any<InputFile>(), setOf(2)) }
    }

    @Test
    fun functions() {
        scan(
            """
            class C {
                init {
                    val y = 1 + 1;
                    val x = true || false;
                }
            }
            """.trimIndent()
        )
        Assertions.assertThat(getMeasure(CoreMetrics.FUNCTIONS)).isZero

        scan(
            """
            abstract class C {
                fun noBodyFunction()
                fun function1() { // comment
                    val x = true || false
                }
            }
            """.trimIndent()
        )
        Assertions.assertThat(getMeasure(CoreMetrics.FUNCTIONS)).isEqualTo(1)
    }

    @Test
    fun classes() {
        scan(
            """
            fun foo(x: Int) { x + 1 }
            fun bar() = true || false;
            """.trimIndent()
        )
        Assertions.assertThat(getMeasure(CoreMetrics.CLASSES)).isZero

        scan(
            """
            class C {}
            fun function() {}
            class D { val x = 0 }
            class E {
              fun doSomething(x: Int) {}
            }
            """.trimIndent()
        )
        Assertions.assertThat(getMeasure(CoreMetrics.CLASSES)).isEqualTo(3)
    }

    @Test
    fun cognitiveComplexity() {
        scan(
            """
            class A { fun foo() { if(1 != 1) return 1 } } // +1
            fun function() {
                if (1 != 1) { // +1
                    if (1 != 1) { // +2
                        1
                    }
                }
                class B {
                    fun bar(a: Int) {
                        when(a) { // +1
                            1 -> doSomething()
                            2 -> doSomething()
                            else -> if (1 != 1) doSomething(); // +2
                        }
                    }
                }
            }
            """.trimIndent()
        )
        Assertions.assertThat(getMeasure(CoreMetrics.COGNITIVE_COMPLEXITY)).isEqualTo(7)
    }

    @Test
    fun executable_lines() {
        scan(
            """
            package abc
            import x
            class A {
                fun foo(a: Int, b: Int) {
                    statementOnSeveralLines(a,
                        b)
                }
            }
            fun bar() {
                val x = 42
            }
            """.trimIndent()
        )

        Assertions.assertThat(fileLines).containsKey(CoreMetrics.EXECUTABLE_LINES_DATA_KEY)
        Assertions.assertThat(fileLines[CoreMetrics.EXECUTABLE_LINES_DATA_KEY]).containsExactly(5, 10)
    }

    private fun scan(code: String) {
        inputFile = TestInputFileBuilder("moduleKey", createTempFile(tempFolder).name)
            .setCharset(StandardCharsets.UTF_8)
            .initMetadata(code).build()
        val ctx = InputFileContextImpl(sensorContext, inputFile, false)
        visitor.scan(ctx, kotlinTreeOf(code, environment, inputFile))
    }

    private fun getMeasure(metric: Metric<Int>) = sensorContext.measure("moduleKey:${inputFile.filename()}", metric).value()
}
