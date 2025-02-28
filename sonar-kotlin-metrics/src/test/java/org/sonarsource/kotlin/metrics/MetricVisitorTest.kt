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
package org.sonarsource.kotlin.metrics

import com.intellij.openapi.util.Disposer
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.config.LanguageVersion
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.fs.internal.DefaultInputFile
import org.sonar.api.batch.fs.internal.TestInputFileBuilder
import org.sonar.api.batch.sensor.internal.SensorContextTester
import org.sonar.api.issue.NoSonarFilter
import org.sonar.api.measures.FileLinesContext
import org.sonar.api.measures.FileLinesContextFactory
import org.sonarsource.kotlin.api.checks.InputFileContextImpl
import org.sonarsource.kotlin.api.frontend.Environment
import org.sonarsource.kotlin.testapi.kotlinTreeOf
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import kotlin.io.path.createTempFile
import kotlin.io.path.name

internal class MetricVisitorTest {
    private val disposable = Disposer.newDisposable()

    @AfterEach
    fun dispose() {
        Disposer.dispose(disposable)
    }

    private val environment = Environment(disposable, emptyList(), LanguageVersion.LATEST_STABLE)
    private lateinit var mockNoSonarFilter: NoSonarFilter
    private lateinit var visitor: MetricVisitor
    private lateinit var sensorContext: SensorContextTester
    private lateinit var inputFile: DefaultInputFile

    @JvmField
    @TempDir
    var tempFolder: Path? = null

    @BeforeEach
    fun setUp() {
        sensorContext = SensorContextTester.create(tempFolder!!.root)
        val mockFileLinesContext = Mockito.mock(FileLinesContext::class.java)
        val mockFileLinesContextFactory = Mockito.mock(
            FileLinesContextFactory::class.java
        )
        mockNoSonarFilter = Mockito.mock(NoSonarFilter::class.java)
        Mockito.`when`(
            mockFileLinesContextFactory.createFor(
                ArgumentMatchers.any(
                    InputFile::class.java
                )
            )
        ).thenReturn(mockFileLinesContext)
        visitor = MetricVisitor(mockFileLinesContextFactory, mockNoSonarFilter, TelemetryData())
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
        assertThat(visitor.linesOfCode()).containsExactly(1, 3, 4, 5)
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
        assertThat(visitor.commentLines()).containsExactly(2, 3)
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
        assertThat(visitor.commentLines()).containsExactly(1, 2, 3)
        assertThat(visitor.linesOfCode()).isEmpty()
    }

    @Test
    fun multiLineCommentInFunction() {
        scan(
            """
    fun function1(x: Int) { 
        /**
         * Multiline comment
         * inside a function
         */
        x + 1 
    }
    """.trimIndent()
        )
        assertThat(visitor.commentLines()).containsExactly(2, 3, 4, 5)
        assertThat(visitor.linesOfCode()).containsExactly(1, 6, 7 )
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
        assertThat(visitor.nosonarLines()).containsExactly(2)
        assertThat(visitor.commentLines()).containsExactly(3)
        Mockito.verify(mockNoSonarFilter).noSonarInFile(inputFile, setOf(2))
    }

    @Test
    fun `kDocs are counted`() {
        scan(
            """
    fun function1(x: Int) { x + 1 }
    /**
     * KDoc comment
     */
    fun function2() {
        val x = true || false
    }
    """.trimIndent()
        )
        assertThat(visitor.commentLines()).containsExactly(2, 3, 4)
        assertThat(visitor.nosonarLines()).isEmpty()
    }

    @Test
    fun `header comments are not counted`() {
        scan(
            """
    // Header comment
    //
    package a
    
    fun function1(x: Int) { x + 1 }
    """.trimIndent()
        )
        assertThat(visitor.commentLines()).isEmpty()
    }


    @Test
    fun `header block comments are not counted`() {
        scan(
            """
            
    /*
     * Header comment
     */
    package b
      
    val my_c = 2
     
    fun function1(x: Int) { x + 1 }
    """.trimIndent()
        )
        assertThat(visitor.commentLines()).isEmpty()
    }

    @Test
    fun `double header comments are counted correctly`() {
        scan(
            """
            
    /*
     * Header comment
     */
     
    // Another header comment
     
    package b
      
    val my_c = 2
     
    fun function1(x: Int) { x + 1 }
    """.trimIndent()
        )
        assertThat(visitor.commentLines()).isEmpty()
    }

    @Test
    fun `a mixture of comments is counted correctly`() {
        scan(
            """
            
    /*
     * Header comment
     */
    package b
      
    // This is a comment
    // for the variable my_c
    val my_c = 2
     
    // NOSONAR comment
    
    /**
     * A KDoc comment
     */
    fun function1(x: Int) { x + 1 } // A comment

    // NOSONAR comment
    """.trimIndent()
        )
        assertThat(visitor.commentLines()).containsExactly(7, 8, 16, 13, 14, 15)
        assertThat(visitor.nosonarLines()).containsExactly(11, 18)
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
        assertThat(visitor.numberOfFunctions()).isZero

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
        assertThat(visitor.numberOfFunctions()).isEqualTo(1)
    }

    @Test
    fun classes() {
        scan(
            """
             fun foo(x: Int) { x + 1 }
             fun bar() = true || false;
    """.trimIndent()
        )
        assertThat(visitor.numberOfClasses()).isZero

        scan(
            """class C {}
                fun function() {}
                class D { val x = 0 }
                class E {
                  fun doSomething(x: Int) {}
                }"""
        )
        assertThat(visitor.numberOfClasses()).isEqualTo(3)
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
                }"""
        )
        assertThat(visitor.cognitiveComplexity()).isEqualTo(7)
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
               }"""
        )
        assertThat(visitor.executableLines()).containsExactly(5, 10)
    }

    @Test
    fun hasAndroidImports() {
        fun assert(expected: Boolean, code: String) {
            scan(code)
            assertThat(visitor.hasAndroidImports()).isEqualTo(expected)
        }

        assert(true, "import android.content.Context")
        assert(true, "import android.content.Context;")
        assert(true, "import android.net.Uri")
        assert(true, "import androidx.core.view.WindowCompat")
        assert(true, "import android.graphics.*")
        assert(true, "import android.content.Context as AndroidContext")

        assert(false, "import java.io.ByteArrayOutputStream")
        assert(false, "import kotlin.properties.ReadWriteProperty")
        assert(false, "/* import android.content.Context */")
        assert(false, """
            // import android.content.Context
        """.trimIndent())
        assert(false, "import mylibrary.android.MyClass")
        assert(false, "import androidy.core.view.WindowCompat")
        assert(false, "package android")
        assert(false, "class android {}")
        assert(false, "fun android() = 42")

        assert(true, """
            import java.io.*
            import android.content.SharedPreferences
        """.trimIndent())
        assert(true, """
            import android.util.Log
            import com.facebook.react.PackageList
        """.trimIndent())
    }

    private fun scan(code: String) {
        val file = createTempFile(tempFolder, suffix = ".kt")
        inputFile = TestInputFileBuilder("moduleKey", file.name)
            .setModuleBaseDir(file.parent)
            .setCharset(StandardCharsets.UTF_8)
            .initMetadata(code).build()
        val ctx = InputFileContextImpl(sensorContext, inputFile, false)
        visitor.scan(ctx, kotlinTreeOf(code, environment, inputFile))
    }
}
