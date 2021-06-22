/*
 * SonarSource SLang
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

import org.assertj.core.api.Assertions
import org.junit.Rule
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport
import org.junit.rules.TemporaryFolder
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.fs.internal.DefaultInputFile
import org.sonar.api.batch.fs.internal.TestInputFileBuilder
import org.sonar.api.batch.sensor.internal.SensorContextTester
import org.sonar.api.issue.NoSonarFilter
import org.sonar.api.measures.FileLinesContext
import org.sonar.api.measures.FileLinesContextFactory
import org.sonarsource.kotlin.converter.KotlinConverter
import org.sonarsource.slang.plugin.InputFileContext
import java.nio.charset.StandardCharsets

@EnableRuleMigrationSupport
internal class MetricVisitorTest {
    private val parser = KotlinConverter(emptyList())
    private lateinit var mockNoSonarFilter: NoSonarFilter
    private lateinit var visitor: MetricVisitor
    private lateinit var sensorContext: SensorContextTester
    private lateinit var inputFile: DefaultInputFile


    val tempFolder = TemporaryFolder()
        @Rule get

    @BeforeEach
    fun setUp() {
        sensorContext = SensorContextTester.create(tempFolder.root)
        val mockFileLinesContext = Mockito.mock(FileLinesContext::class.java)
        val mockFileLinesContextFactory = Mockito.mock(
            FileLinesContextFactory::class.java)
        mockNoSonarFilter = Mockito.mock(NoSonarFilter::class.java)
        Mockito.`when`(mockFileLinesContextFactory.createFor(ArgumentMatchers.any(
            InputFile::class.java))).thenReturn(mockFileLinesContext)
        visitor = MetricVisitor(mockFileLinesContextFactory, mockNoSonarFilter)
    }

    @Test
    fun linesOfCode() {
        scan("""
    fun function1(x: Int) { x + 1 }
    // comment
    fun function1() { // comment
        val x = true || false 
    }
    """.trimIndent())
        Assertions.assertThat(visitor.linesOfCode()).containsExactly(1, 3, 4, 5)
    }

    @Test
    fun commentLines() {
        scan("""
    fun function1(x: Int) { x + 1 }
    // comment
    fun function1() { // comment
        val x = true || false
    }
    """.trimIndent())
        Assertions.assertThat(visitor.commentLines()).containsExactly(2, 3)
    }

    @Test
    fun multiLineComment() {
        scan("""
    /*start
    x + 1
    end*/
    """.trimIndent())
        Assertions.assertThat(visitor.commentLines()).containsExactly(1, 2, 3)
        Assertions.assertThat(visitor.linesOfCode()).isEmpty()
    }

    @Test
    fun nosonarLines() {
        scan("""
    fun function1(x: Int) { x + 1 }
    // NOSONAR comment
    fun function2() { // comment
        val x = true || false
    }
    """.trimIndent())
        Assertions.assertThat(visitor.nosonarLines()).containsExactly(2)
        Mockito.verify(mockNoSonarFilter).noSonarInFile(inputFile, setOf(2))
    }

    @Test
    fun functions() {
        scan("""
            class C {
                init {
                    val y = 1 + 1;
                    val x = true || false;
                }
            }
    """.trimIndent())
        Assertions.assertThat(visitor.numberOfFunctions()).isZero

        scan("""
            abstract class C {
                fun noBodyFunction()
                fun function1() { // comment
                    val x = true || false
                }
            }
    """.trimIndent())
        Assertions.assertThat(visitor.numberOfFunctions()).isEqualTo(1)
    }

    @Test
    fun classes() {
        scan("""
             fun foo(x: Int) { x + 1 }
             fun bar() = true || false;
    """.trimIndent())
        Assertions.assertThat(visitor.numberOfClasses()).isZero

        scan("""class C {}
                fun function() {}
                class D { val x = 0 }
                class E {
                  fun doSomething(x: Int) {}
                }""")
        Assertions.assertThat(visitor.numberOfClasses()).isEqualTo(3)
    }

    @Test
    fun cognitiveComplexity() {
        scan("""
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
                }""")
        Assertions.assertThat(visitor.cognitiveComplexity()).isEqualTo(7)
    }

    @Test
    fun executable_lines() {
        scan(
            """package abc
               import x
               class A {
                 fun foo(a: Int, b: Int) {
                   statementOnSeveralLines(a,
                     b)
                 }
               }
               fun bar() {
                 val x = 42
               }""")
        Assertions.assertThat(visitor.executableLines()).containsExactly(5, 10)
    }

    private fun scan(code: String) {
        inputFile = TestInputFileBuilder("moduleKey", tempFolder.newFile().name)
            .setCharset(StandardCharsets.UTF_8)
            .initMetadata(code).build()
        val ctx = InputFileContext(sensorContext, inputFile)
        visitor.scan(ctx, parser.parse(code))
    }
}
