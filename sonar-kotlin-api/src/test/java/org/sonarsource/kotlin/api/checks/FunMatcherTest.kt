/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.sonar.api.batch.fs.internal.TestInputFileBuilder
import org.sonarsource.kotlin.api.frontend.Environment
import org.sonarsource.kotlin.api.visiting.kaSession
import org.sonarsource.kotlin.testapi.DEFAULT_KOTLIN_CLASSPATH
import org.sonarsource.kotlin.testapi.kotlinTreeOf
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.name

class FunMatcherTest {
    private val disposable = Disposer.newDisposable()

    @AfterEach
    fun dispose() {
        Disposer.dispose(disposable)
    }

    val classpath = System.getProperty("java.class.path").split(File.pathSeparatorChar) + DEFAULT_KOTLIN_CLASSPATH
    val environment = Environment(disposable, classpath, LanguageVersion.LATEST_STABLE)
    val path = Paths.get("../kotlin-checks-test-sources/src/main/kotlin/sample/functions.kt")
    val content = String(Files.readAllBytes(path))
    val inputFile = TestInputFileBuilder("moduleKey",  path.name)
        .setModuleBaseDir(path.parent)
        .setCharset(StandardCharsets.UTF_8)
        .initMetadata(content)
        .build()

    val tree = kotlinTreeOf(content, environment, inputFile)
    private val allCallExpressions = tree.psiFile.collectDescendantsOfType<KtCallExpression>()

    // sampleClass.sayHello("Kotlin")
    val ktCallExpression1 = allCallExpressions[1]

    // sampleClass.sayHello("Java")
    val ktCallExpression2 = allCallExpressions[2]

    // sampleClass.sayHelloNullable("nothingness")
    val ktCallExpression3 = allCallExpressions[3]

    // "".suspendExtFun()
    val ktCallExpression4 = allCallExpressions[4]

    // sampleClass.intAndVararg(42, "one")
    val ktCallExpressionIntAndVararg1 = allCallExpressions[5]

    // sampleClass.intAndVararg(42, "one", "two")
    val ktCallExpressionIntAndVararg2 = allCallExpressions[6]

    // sampleClass.get(42)
    val ktCallExpressionGet = allCallExpressions[7]

    private val testFunCalls = allCallExpressions.subList(1, 8)

    @Test
    fun `match method by type and name`() = kaSession(tree.psiFile) {
        val funMatcher = FunMatcher {
            qualifier = "sample.SampleClass"
            name = "sayHello"
        }

        assertThat(funMatcher.matches(ktCallExpression1)).isTrue
        assertThat(funMatcher.matches(ktCallExpression2)).isTrue
    }

    @Test
    fun `match method by type and name and multiple names`() = kaSession(tree.psiFile) {
        val funMatcher = FunMatcher {
            qualifier = "sample.SampleClass"
            withNames("sayHello", "sayHelloTo")
        }

        assertThat(funMatcher.matches(ktCallExpression1)).isTrue
        assertThat(funMatcher.matches(ktCallExpression2)).isTrue
    }

    @Test
    fun `Don't match method by type or name`() = kaSession(tree.psiFile) {
        val wrongTypeMethodMatcher = FunMatcher {
            qualifier = "sample.MySampleClass"
            name = "sayHello"
        }

        val wrongNameMethodMatcher = FunMatcher {
            qualifier = "sample.SampleClass"
            name = "sayBye"
        }

        val wrongNameMethodMatcherModified = FunMatcher {
            qualifier = "sample.SampleClass"
            name = "sayBye"
        }

        val wrongNameMethodMatcherModified2 = FunMatcher {
            qualifier = "sample.SampleClass"
            name = "sayBye"
            name = null
        }

        assertThat(wrongTypeMethodMatcher.matches(ktCallExpression1)).isFalse
        assertThat(wrongTypeMethodMatcher.matches(ktCallExpression2)).isFalse

        assertThat(wrongNameMethodMatcher.matches(ktCallExpression1)).isFalse
        assertThat(wrongNameMethodMatcher.matches(ktCallExpression2)).isFalse

        assertThat(wrongNameMethodMatcherModified.matches(ktCallExpression1)).isFalse
        assertThat(wrongNameMethodMatcherModified.matches(ktCallExpression2)).isFalse

        assertThat(wrongNameMethodMatcherModified2.matches(ktCallExpression1)).isTrue
        assertThat(wrongNameMethodMatcherModified2.matches(ktCallExpression2)).isTrue
    }

    @Test
    fun `Match method by name regex`() = kaSession(tree.psiFile) {
        val funMatcher = FunMatcher(nameRegex = """^say.*+""".toRegex())

        assertThat(funMatcher.matches(ktCallExpression1)).isTrue
        assertThat(funMatcher.matches(ktCallExpression2)).isTrue
    }

    @Test
    fun `Match method by type and name regex`() = kaSession(tree.psiFile) {
        val funMatcher = FunMatcher {
            qualifier = "sample.SampleClass"
            nameRegex = """^say.*+""".toRegex()
        }

        assertThat(funMatcher.matches(ktCallExpression1)).isTrue
        assertThat(funMatcher.matches(ktCallExpression2)).isTrue
    }

    @Test
    fun `Match method by type, name and name regex`() = kaSession(tree.psiFile) {
        val funMatcher = FunMatcher {
            qualifier = "sample.SampleClass"
            name = "sayHello"
            nameRegex = """^say.*+""".toRegex()
        }

        assertThat(funMatcher.matches(ktCallExpression1)).isTrue
        assertThat(funMatcher.matches(ktCallExpression2)).isTrue
    }

    @Test
    fun `Match method by type, incorrect name and name regex`() = kaSession(tree.psiFile) {
        val funMatcher = FunMatcher {
            qualifier = "sample.SampleClass"
            name = "anything"
            nameRegex = """^say.*+""".toRegex()
        }

        assertThat(funMatcher.matches(ktCallExpression1)).isTrue
        assertThat(funMatcher.matches(ktCallExpression2)).isTrue
    }

    @Test
    fun `Match method by type, name and incorrect name regex`() = kaSession(tree.psiFile) {
        val funMatcher = FunMatcher {
            qualifier = "sample.SampleClass"
            name = "sayHello"
            nameRegex = """^lala.*+""".toRegex()
        }

        assertThat(funMatcher.matches(ktCallExpression1)).isTrue
        assertThat(funMatcher.matches(ktCallExpression2)).isTrue
    }

    @Test
    fun `Don't method by type, name and any name regex`() = kaSession(tree.psiFile) {
        val funMatcher = FunMatcher {
            qualifier = "sample.SampleClass"
            name = "anything"
            nameRegex = "lala".toRegex()
        }

        assertThat(funMatcher.matches(ktCallExpression1)).isFalse
        assertThat(funMatcher.matches(ktCallExpression2)).isFalse
    }

    @Test
    fun `Match method with parameters`() = kaSession(tree.psiFile) {
        val funMatcher = FunMatcher {
            qualifier = "sample.SampleClass"
            name = "sayHello"
            withArguments("kotlin.String")
        }

        assertThat(funMatcher.matches(ktCallExpression1)).isTrue
        assertThat(funMatcher.matches(ktCallExpression2)).isTrue
    }

    @Test
    fun `Match method without type`() = kaSession(tree.psiFile) {
        val funMatcher = FunMatcher {
            name = "sayHello"
            withArguments("kotlin.String")
        }

        assertThat(funMatcher.matches(ktCallExpression1)).isTrue
        assertThat(funMatcher.matches(ktCallExpression2)).isTrue
    }

    @Test
    fun `Don't match method with wrong number of parameters`() = kaSession(tree.psiFile) {
        val funMatcher = FunMatcher {
            qualifier = "sample.SampleClass"
            name = "sayHello"
            withArguments("kotlin.String", "kotlin.String")
        }

        assertThat(funMatcher.matches(ktCallExpression1)).isFalse
        assertThat(funMatcher.matches(ktCallExpression2)).isFalse
    }

    @Test
    fun `Don't match method with wrong parameters`() = kaSession(tree.psiFile) {
        val funMatcher = FunMatcher {
            qualifier = "sample.SampleClass"
            name = "sayHello"
            withArguments("Int")
        }

        assertThat(funMatcher.matches(ktCallExpression1)).isFalse
        assertThat(funMatcher.matches(ktCallExpression2)).isFalse
    }

    @Test
    fun `Don't match method with no parameters matcher`() = kaSession(tree.psiFile) {
        val funMatcher = FunMatcher {
            qualifier = "sample.SampleClass"
            name = "sayHello"
            withArguments("Int")
            withNoArguments()
        }

        assertThat(funMatcher.matches(ktCallExpression1)).isFalse
        assertThat(funMatcher.matches(ktCallExpression2)).isFalse
    }

    @Test
    fun `Match method with no parameters matcher`() = kaSession(tree.psiFile) {
        val funMatcher = FunMatcher {
            qualifier = "sample.SampleClass"
            name = "sayHello"
            withArguments("kotlin.String")
            withNoArguments()
        }

        assertThat(funMatcher.matches(ktCallExpression1)).isTrue
        assertThat(funMatcher.matches(ktCallExpression2)).isTrue
    }

    @Test
    fun `Match method declaration`() = kaSession(tree.psiFile) {
        val funMatcher = FunMatcher {
            qualifier = "sample.MySampleClass"
            name = "sayHello"
            withArguments("kotlin.String")
            withNoArguments()
        }
        val ktNamedFunction = tree.psiFile.children[5].children[1].children[0] as KtNamedFunction

        assertThat(funMatcher.matches(ktNamedFunction)).isTrue
    }

    @Test
    fun `Don't match method declaration with wrong parameters`() = kaSession(tree.psiFile) {
        val funMatcher = FunMatcher {
            qualifier = "sample.MySampleClass"
            name = "sayHello"
            withNoArguments()
        }
        val ktNamedFunction = tree.psiFile.children[5].children[1].children[0] as KtNamedFunction

        assertThat(funMatcher.matches(ktNamedFunction)).isFalse
    }

    @Test
    fun `Match method declaration with type of supertype`() = kaSession(tree.psiFile) {
        val funMatcher = FunMatcher {
            definingSupertype = "sample.MySampleClass"
            name = "sayHello"
            withArguments("kotlin.String")
        }
        val ktNamedFunction = tree.psiFile.children[5].children[1].children[0] as KtNamedFunction

        assertThat(funMatcher.matches(ktNamedFunction)).isTrue
    }

    @Test
    fun `Don't match method declaration with wrong supertype`() = kaSession(tree.psiFile) {
        val funMatcher = FunMatcher {
            definingSupertype = "NonexistentClass"
            name = "sayHello"
            withArguments("kotlin.String")
        }
        val ktNamedFunction = tree.psiFile.children[5].children[1].children[0] as KtNamedFunction

        assertThat(funMatcher.matches(ktNamedFunction)).isFalse
    }

    @Test
    fun `Match method declaration by supertype`() = kaSession(tree.psiFile) {
        val funMatcher = FunMatcher {
            definingSupertype = "sample.MyInterface"
            name = "sayHello"
            withArguments("kotlin.String")
        }
        val ktNamedFunction = tree.psiFile.children[5].children[1].children[0] as KtNamedFunction

        assertThat(funMatcher.matches(ktNamedFunction)).isTrue
    }

    @Test
    fun `Match constructor`() = kaSession(tree.psiFile) {
        val funMatcher = ConstructorMatcher {
            qualifier = "sample.SampleClass"
            withNoArguments()
        }

        val callExpression = allCallExpressions[0]

        assertThat(funMatcher.matches(callExpression)).isTrue
    }

    @Test
    fun `Don't match constructor`() = kaSession(tree.psiFile) {
        val funMatcher = ConstructorMatcher {
            qualifier = "sample.SampleClass"
            withArguments("kotlin.String")
        }

        val callExpression = allCallExpressions[0]

        assertThat(funMatcher.matches(callExpression)).isFalse
    }

    @Test
    fun `Don't match the constructor by supertype`() = kaSession(tree.psiFile) {
        val funMatcher = ConstructorMatcher {
            definingSupertype = "sample.MyInterface"
            withNoArguments()
        }

        val callExpression = allCallExpressions[5]

        assertThat(funMatcher.matches(callExpression)).isFalse
    }

    @Test
    fun `Match multiple qualifiers`() {
        check(FunMatcher {
            withQualifiers("sample.SampleClass", "sample")
        },true, true, true, true, true, true, true)
    }

    @Test
    fun `Match multiple defining supertypes`() {
        check(FunMatcher {
            withDefiningSupertypes("sample.SampleClass", "sample")
        }, true, true, true, true, true, true, true)
    }

    @Test
    fun `Match only suspending methods`() {
        check(FunMatcher(isSuspending = true), false, false, false, true, false, false, false)
    }

    @Test
    fun `Match only non-suspending methods`() {
        check(FunMatcher(isSuspending = false), true, true, true, false, true, true, true)
    }

    @Test
    fun `Match only operator functions`() {
        check(FunMatcher(isOperator = true), false, false, false, false, false, false, true)
    }

    @Test
    fun `Match only nun-operator functions`() {
        check(FunMatcher(isOperator = false), true, true, true, true, true, true, false)
    }

    @Test
    fun `Match only extension methods`() {
        check(FunMatcher(isExtensionFunction = true), false, false, false, true, false, false, false)
    }

    @Test
    fun `Match only non-extension methods`() {
        check(FunMatcher(isExtensionFunction = false), true, true, true, false, true, true, true)
    }

    @Test
    fun `Match only methods returning String`() {
        check(FunMatcher(returnType = "kotlin.String"), false, false, false, true, false, false, false)
    }

    @Test
    fun `Match only methods returning int`() {
        check(FunMatcher(returnType = "kotlin.Int"), false, false, true, false, false, false, true)
    }

    @Test
    fun `Match only methods returning Unit`() {
        check(FunMatcher(returnType = "kotlin.Unit"), true, true, false, false, true, true, false)
    }

    @Test
    fun `Match aliased and non-aliased constructor`() = kaSession(tree.psiFile) {
        val constructorExpressions = tree.psiFile
            .findDescendantOfType<KtNamedFunction> { it.name == "Match aliased and non-aliased constructor" }!!
            .collectDescendantsOfType<KtCallExpression> {
                it.calleeExpression?.text?.contains("IllegalStateException") ?: false
            }
        val assert = { typeName: String, expected: Boolean ->
            val matcher = ConstructorMatcher(typeName = typeName)
            constructorExpressions.forEach { assertThat(matcher.matches(it)).withFailMessage(it.text).isEqualTo(expected) }
        }

        assert("java.lang.IllegalStateException", true)
        assert("kotlin.IllegalStateException", false)
        assert("JavaLangIllegalStateExceptionAlias", false)
        assert("KotlinIllegalStateExceptionAlias", false)
        assert("KotlinIllegalStateExceptionAliasOfAlias", false)
        assert("IllegalStateException", false)
    }

    private fun check(funMatcher: FunMatcherImpl, vararg expected: Boolean?) = kaSession(tree.psiFile) {
        assertThat(expected).hasSameSizeAs(testFunCalls)
        testFunCalls.forEachIndexed { index, callExpression ->
            if (expected[index] != null) {
                assertThat(funMatcher.matches(callExpression))
                    .withFailMessage("Unexpected (mis)match on call expression #$index (expected ${expected[index]})")
                    .isEqualTo(expected[index])
            }
        }
    }
}
