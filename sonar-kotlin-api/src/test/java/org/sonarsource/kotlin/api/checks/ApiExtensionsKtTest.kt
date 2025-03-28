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
package org.sonarsource.kotlin.api.checks

import com.intellij.openapi.util.Disposer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ObjectAssert
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtThisExpression
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.sonar.api.SonarEdition
import org.sonar.api.SonarQubeSide
import org.sonar.api.batch.fs.internal.TestInputFileBuilder
import org.sonar.api.batch.sensor.internal.SensorContextTester
import org.sonar.api.internal.SonarRuntimeImpl
import org.sonar.api.utils.Version
import org.sonarsource.kotlin.api.frontend.Environment
import org.sonarsource.kotlin.api.visiting.kaSession
import org.sonarsource.kotlin.testapi.kotlinTreeOf
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.TreeMap

private class ApiExtensionsKtTest : AbstractApiExtensionsKtTest() {

    @Test
    fun `test isLocalVariable`() {
        val tree = parse(
            """
            const val a = "abc"
            val b = 1
            var c = true
            class A {
              const val d = ""
              var e = 7
              var f = 1.2
              fun foo(g: Int): List<Any?> {
                const val h = ""
                val i = 3
                var j = 4
                val k :(Int) -> Int = {
                  l -> {
                    var m = l
                    m + 1
                  }
                }
                val n :(Int) -> Int = { it + 1 }
                // reference all variables and fields
                return listOf(a, b, c, d, e, f, g, h, i, j, k, n)
              }
            }
            fun bar(): Int {
              val o = 0
              return o
            }
            """.trimIndent()
        )
        val referencesMap: MutableMap<String, KtReferenceExpression> = TreeMap()
        walker(tree.psiFile) {
            if (it is KtNameReferenceExpression) {
                referencesMap[it.getReferencedName()] = it
            }
        }

        assertThat(referencesMap.keys).containsExactlyInAnyOrder(
            "Any", "Int", "List",
            "a", "b", "c", "d", "e", "f", "g", "h", "i", "it", "j", "k", "l", "listOf", "m", "n", "o"
        )
        kaSession(tree.psiFile) {
            assertThat((null as KtExpression?).isLocalVariable()).isFalse
            assertThat(referencesMap.filter { it.value.isLocalVariable() }.map { it.key })
                .containsExactlyInAnyOrder("h", "i", "j", "k", "m", "n", "o")
        }
    }

    @Test
    fun `test KtExpression findClosestAncestorOfType`() {
        val tree = parse(
            """
            fun foo(list: List<String>, prefix: String, length: Int): Boolean = 
                list.filter { it.startsWith(prefix) }.find { it.length > length } != null
            """.trimIndent()
        )

        val textToExpression: MutableMap<String, KtExpression> = TreeMap()
        walker(tree.psiFile) {
            if (it is KtExpression)
                textToExpression[it.text] = it
        }

        assertThat(textToExpression["find { it.length > length }"]!!.findClosestAncestorOfType<KtDotQualifiedExpression>())
            .isEqualTo(textToExpression["list.filter { it.startsWith(prefix) }.find { it.length > length }"])

        assertThat(textToExpression["find { it.length > length }"]!!.findClosestAncestorOfType<KtBinaryExpression>())
            .isEqualTo(textToExpression["list.filter { it.startsWith(prefix) }.find { it.length > length } != null"])

        assertThat(textToExpression["filter { it.startsWith(prefix) }"]!!.findClosestAncestorOfType<KtBinaryExpression>())
            .isEqualTo(textToExpression["list.filter { it.startsWith(prefix) }.find { it.length > length } != null"])

        assertThat(textToExpression["find"]!!.findClosestAncestorOfType<KtDotQualifiedExpression>())
            .isEqualTo(textToExpression["list.filter { it.startsWith(prefix) }.find { it.length > length }"])

        assertThat(textToExpression["find"]!!.findClosestAncestorOfType<KtDotQualifiedExpression> { it is KtDotQualifiedExpression })
            .isEqualTo(textToExpression["list.filter { it.startsWith(prefix) }.find { it.length > length }"])

        assertThat(textToExpression["find"]!!.findClosestAncestorOfType<KtBinaryExpression> { it is KtDotQualifiedExpression }).isNull()

        assertThat(textToExpression["find"]!!.findClosestAncestorOfType<KtClassLiteralExpression>()).isNull()

        assertThat(textToExpression["find"]!!.findClosestAncestorOfType<KtClassLiteralExpression> { it is KtBinaryExpression }).isNull()
    }

    @Test
    fun `test findClosestAncestor`() {
        val tree = parse(
            """
            fun foo(list: List<String>, prefix: String, length: Int): Boolean = 
                list.filter { it.startsWith(prefix) }.find { it.length > length } != null
            """.trimIndent()
        )

        val textToExpression: MutableMap<String, KtExpression> = TreeMap()
        walker(tree.psiFile) {
            if (it is KtExpression)
                textToExpression[it.text] = it
        }

        val findCall = textToExpression["find { it.length > length }"]!!

        assertThat(findCall.findClosestAncestor { it is KtDotQualifiedExpression })
            .isEqualTo(textToExpression["list.filter { it.startsWith(prefix) }.find { it.length > length }"])

        assertThat(findCall.findClosestAncestor { it is KtFunction && it.name == "foo" })
            .isEqualTo(textToExpression["""
                fun foo(list: List<String>, prefix: String, length: Int): Boolean = 
                    list.filter { it.startsWith(prefix) }.find { it.length > length } != null
            """.trimIndent()])

        // Null, as the paramater itself is not a parent of the `find` call
        assertThat(findCall.findClosestAncestor { it is KtParameter })
            .isNull()
    }

    @Test
    fun `test Functions modifiers`(){
        val tree = parse(
            """
            abstract fun abstractFun():Unit{}
            open fun openFun():Unit{}
            override fun overrideFun():Unit{}
            actual fun actualFun():Unit{}
            expect fun expectFun():Unit{}
            fun defaultFun():Unit{}
            """.trimIndent()
        )
        val textToExpression: MutableMap<String, KtNamedFunction> = TreeMap()
        walker(tree.psiFile) {
            if (it is KtNamedFunction)
                textToExpression[it.name!!] = it
        }
        assertThat(textToExpression["abstractFun"]!!.isAbstract()).isTrue()
        assertThat(textToExpression["defaultFun"]!!.isAbstract()).isFalse()

        assertThat(textToExpression["openFun"]!!.isOpen()).isTrue()
        assertThat(textToExpression["defaultFun"]!!.isOpen()).isFalse()

        assertThat(textToExpression["overrideFun"]!!.overrides()).isTrue()
        assertThat(textToExpression["defaultFun"]!!.overrides()).isFalse()

        assertThat(textToExpression["actualFun"]!!.isActual()).isTrue()
        assertThat(textToExpression["defaultFun"]!!.isActual()).isFalse()

        assertThat(textToExpression["expectFun"]!!.isExpect()).isTrue()
        assertThat(textToExpression["defaultFun"]!!.isExpect()).isFalse()
    }

    private fun walker(node: PsiElement, action: (PsiElement) -> Unit) {
        action(node)
        node.allChildren.forEach { walker(it, action) }
    }
}

private class ApiExtensionsScopeFunctionResolutionTest : AbstractApiExtensionsKtTest() {
    private fun generateAst(funContent: String) = """
            package bar

            class Foo {
                val prop: Int = 42

                fun aFun() {
                    $funContent
                }
            }
        """.let { parse(it) }.psiFile

    @Test
    fun `resolve this as arg in with`() {
        val tree = generateAst(
            """
                with(prop) {
                    println(this)
                }
            """.trimIndent()
        )

        kaSession(tree) {
            assertThatExpr(tree.findDescendantOfType<KtThisExpression>()!!.predictRuntimeValueExpression())
                .isConstantExpr(42)
        }
    }

    @Test
    fun `resolve this as explicit target in with`() {
        val tree = generateAst(
            """
                with(prop) {
                    this.toString()
                }
            """.trimIndent()
        )

        kaSession(tree) {
            assertThatExpr(tree.findDescendantOfType<KtThisExpression>()!!.predictRuntimeValueExpression())
                .isConstantExpr(42)
        }
    }

    @Test
    fun `resolve this as arg in apply`() {
        val tree = generateAst(
            """
                prop.apply {
                    println(this)
                }
            """.trimIndent()
        )

        kaSession(tree) {
            assertThatExpr(tree.findDescendantOfType<KtThisExpression>()!!.predictRuntimeValueExpression())
                .isConstantExpr(42)
        }
    }

    @Test
    fun `resolve this as explicit target in apply`() {
        val tree = generateAst(
            """
                prop.apply {
                    this.toString()
                }
            """.trimIndent()
        )

        kaSession(tree) {
            assertThatExpr(tree.findDescendantOfType<KtThisExpression>()!!.predictRuntimeValueExpression())
                .isConstantExpr(42)
        }
    }

    @Test
    fun `resolve this as arg in run`() {
        val tree = generateAst(
            """
                prop.run {
                    println(this)
                }
            """.trimIndent()
        )

        kaSession(tree) {
            assertThatExpr(tree.findDescendantOfType<KtThisExpression>()!!.predictRuntimeValueExpression())
                .isConstantExpr(42)
        }
    }

    @Test
    fun `resolve this as explicit target in run`() {
        val tree = generateAst(
            """
                prop.run {
                    this.toString()
                }
            """.trimIndent()
        )

        kaSession(tree) {
            assertThatExpr(tree.findDescendantOfType<KtThisExpression>()!!.predictRuntimeValueExpression())
                .isConstantExpr(42)
        }
    }

    @Test
    fun `resolve this to current object`() {
        val tree = generateAst(
            """
                prop.let {
                    println(this)
                }
            """.trimIndent()
        )

        kaSession(tree) {
            assertThatExpr(tree.findDescendantOfType<KtThisExpression>()!!.predictRuntimeValueExpression())
                .isInstanceOf(KtThisExpression::class.java)
        }
    }

    @JvmField
    @TempDir
    var tempFolder: Path? = null

    @Test
    fun `SensorContext hasCacheEnabled() returns false when the product is sonarlint`() {
        val sensorContext = SensorContextTester.create(tempFolder!!)
        val sonarLintRuntime = SonarRuntimeImpl.forSonarLint(Version.create(9, 4))
        sensorContext.setRuntime(sonarLintRuntime)
        sensorContext.isCacheEnabled = true
        assertThat(sensorContext.hasCacheEnabled()).isFalse()
    }

    @Test
    fun `SensorContext hasCacheEnabled() returns false when the product has an API version below 9_4`() {
        val sensorContext = SensorContextTester.create(tempFolder!!)
        val incompatibleRuntime = SonarRuntimeImpl.forSonarQube(Version.create(9, 3), SonarQubeSide.SERVER, SonarEdition.DEVELOPER)
        sensorContext.setRuntime(incompatibleRuntime)
        sensorContext.isCacheEnabled = true
        assertThat(sensorContext.hasCacheEnabled()).isFalse()
    }

    @Test
    fun `SensorContext hasCacheEnabled() returns false when the product has an API version is 9_4 or greater but the cache is disabled`() {
        val sensorContext = SensorContextTester.create(tempFolder!!)
        val minimumCompatibleRuntime = SonarRuntimeImpl.forSonarQube(Version.create(9, 4), SonarQubeSide.SERVER, SonarEdition.DEVELOPER)
        sensorContext.setRuntime(minimumCompatibleRuntime)
        sensorContext.isCacheEnabled = false
        assertThat(sensorContext.hasCacheEnabled()).isFalse()
    }

    @Test
    fun `SensorContext hasCacheEnabled() returns true when the product has an API version is 9_4 or greater but the cache is enabled`() {
        val sensorContext = SensorContextTester.create(tempFolder!!)
        val minimumCompatibleRuntime = SonarRuntimeImpl.forSonarQube(Version.create(9, 4), SonarQubeSide.SERVER, SonarEdition.DEVELOPER)
        sensorContext.setRuntime(minimumCompatibleRuntime)
        sensorContext.isCacheEnabled = true
        assertThat(sensorContext.hasCacheEnabled()).isTrue()
    }
}

private abstract class AbstractApiExtensionsKtTest {
    private val disposable = Disposer.newDisposable()

    @AfterEach
    fun dispose() {
        Disposer.dispose(disposable)
    }

    fun parse(code: String) = kotlinTreeOf(
        code,
        Environment(
            disposable,
            listOf("build/classes/kotlin/main") + System.getProperty("java.class.path").split(File.pathSeparatorChar),
            LanguageVersion.LATEST_STABLE
        ),
        TestInputFileBuilder("moduleKey", "src/org/foo/kotlin.kt")
            .setModuleBaseDir(Path.of("."))
            .setCharset(StandardCharsets.UTF_8)
            .initMetadata(code)
            .build()
    )

    fun parseWithoutParsingExceptions(code: String): KtFile {
        val environment = Environment(
            disposable,
            listOf("build/classes/kotlin/main") + System.getProperty("java.class.path").split(File.pathSeparatorChar),
            LanguageVersion.LATEST_STABLE
        )
        val inputFile = TestInputFileBuilder("moduleKey", "src/org/foo/kotlin.kt")
            .setModuleBaseDir(Path.of("."))
            .setCharset(StandardCharsets.UTF_8)
            .initMetadata(code)
            .build()
        return environment.ktPsiFactory.createFile(inputFile.uri().path, code.replace("""\r\n?""".toRegex(), "\n"))
    }

}

private class KtExpressionAssert(expression: KtExpression?) : ObjectAssert<KtExpression>(expression) {
    fun isConstantExpr(value: Int): KtExpressionAssert {
        isNotNull
        actual!!.let { actualNonNull ->
            assertThat(actualNonNull)
                .withFailMessage {
                    "Expecting KtExpression of type ${KtConstantExpression::class.simpleName}, got ${actualNonNull::class.simpleName}"
                }
                .isInstanceOf(KtConstantExpression::class.java)

            assertThat((actualNonNull as KtConstantExpression).elementType.debugName)
                .withFailMessage {
                    "Expecting KtConstant with element type INTEGER_CONSTANT, got ${actualNonNull.elementType.debugName}"
                }
                .isEqualTo("INTEGER_CONSTANT")

            assertThat(actualNonNull.text)
                .withFailMessage {
                    "Expecting constant expression with value $value, got ${actualNonNull.text}"
                }
                .isEqualTo(value.toString())
        }
        return this
    }
}

private fun assertThatExpr(expression: KtExpression?) = KtExpressionAssert(expression)
