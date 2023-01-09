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
package org.sonarsource.kotlin.api

import io.mockk.Called
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.util.getCall
import org.jetbrains.kotlin.types.typeUtil.TypeNullability
import org.junit.jupiter.api.Test
import org.sonar.api.batch.fs.internal.TestInputFileBuilder
import org.sonarsource.kotlin.converter.Environment
import org.sonarsource.kotlin.utils.kotlinTreeOf
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

class FunMatcherTest {

    val environment = Environment(listOf("../kotlin-checks-test-sources/build/classes/kotlin/main"), LanguageVersion.LATEST_STABLE)
    val path = Paths.get("../kotlin-checks-test-sources/src/main/kotlin/sample/functions.kt")
    val content = String(Files.readAllBytes(path))
    val inputFile = TestInputFileBuilder("moduleKey",  "src/org/foo/kotlin.kt")
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

    private val testFunCalls = allCallExpressions.subList(1, 7)

    @Test
    fun `match method by type and name`() {
        val funMatcher = FunMatcher {
            qualifier = "sample.SampleClass"
            name = "sayHello"
        }

        assertThat(funMatcher.matches(ktCallExpression1, tree.bindingContext)).isTrue
        assertThat(funMatcher.matches(ktCallExpression2, tree.bindingContext)).isTrue
    }

    @Test
    fun `match method by type and name and multiple names`() {
        val funMatcher = FunMatcher {
            qualifier = "sample.SampleClass"
            withNames("sayHello", "sayHelloTo")
        }

        assertThat(funMatcher.matches(ktCallExpression1, tree.bindingContext)).isTrue
        assertThat(funMatcher.matches(ktCallExpression2, tree.bindingContext)).isTrue
    }

    @Test
    fun `Don't match method by type or name`() {
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

        assertThat(wrongTypeMethodMatcher.matches(ktCallExpression1, tree.bindingContext)).isFalse
        assertThat(wrongTypeMethodMatcher.matches(ktCallExpression2, tree.bindingContext)).isFalse

        assertThat(wrongNameMethodMatcher.matches(ktCallExpression1, tree.bindingContext)).isFalse
        assertThat(wrongNameMethodMatcher.matches(ktCallExpression2, tree.bindingContext)).isFalse

        assertThat(wrongNameMethodMatcherModified.matches(ktCallExpression1, tree.bindingContext)).isFalse
        assertThat(wrongNameMethodMatcherModified.matches(ktCallExpression2, tree.bindingContext)).isFalse

        assertThat(wrongNameMethodMatcherModified2.matches(ktCallExpression1, tree.bindingContext)).isTrue
        assertThat(wrongNameMethodMatcherModified2.matches(ktCallExpression2, tree.bindingContext)).isTrue
    }

    @Test
    fun `Don't match method without binding context`() {
        val funMatcher = FunMatcher {
            qualifier = "sample.SampleClass"
            name = "sayHello"
        }

        assertThat(funMatcher.matches(ktCallExpression1, BindingContext.EMPTY)).isFalse
        assertThat(funMatcher.matches(ktCallExpression2, BindingContext.EMPTY)).isFalse
    }

    @Test
    fun `Match method by name regex`() {
        val funMatcher = FunMatcher(nameRegex = """^say.*+""".toRegex())

        assertThat(funMatcher.matches(ktCallExpression1, tree.bindingContext)).isTrue
        assertThat(funMatcher.matches(ktCallExpression2, tree.bindingContext)).isTrue
    }

    @Test
    fun `Match method by type and name regex`() {
        val funMatcher = FunMatcher {
            qualifier = "sample.SampleClass"
            nameRegex = """^say.*+""".toRegex()
        }

        assertThat(funMatcher.matches(ktCallExpression1, tree.bindingContext)).isTrue
        assertThat(funMatcher.matches(ktCallExpression2, tree.bindingContext)).isTrue
    }

    @Test
    fun `Match method by type, name and name regex`() {
        val funMatcher = FunMatcher {
            qualifier = "sample.SampleClass"
            name = "sayHello"
            nameRegex = """^say.*+""".toRegex()
        }

        assertThat(funMatcher.matches(ktCallExpression1, tree.bindingContext)).isTrue
        assertThat(funMatcher.matches(ktCallExpression2, tree.bindingContext)).isTrue
    }

    @Test
    fun `Match method by type, incorrect name and name regex`() {
        val funMatcher = FunMatcher {
            qualifier = "sample.SampleClass"
            name = "anything"
            nameRegex = """^say.*+""".toRegex()
        }

        assertThat(funMatcher.matches(ktCallExpression1, tree.bindingContext)).isTrue
        assertThat(funMatcher.matches(ktCallExpression2, tree.bindingContext)).isTrue
    }

    @Test
    fun `Match method by type, name and incorrect name regex`() {
        val funMatcher = FunMatcher {
            qualifier = "sample.SampleClass"
            name = "sayHello"
            nameRegex = """^lala.*+""".toRegex()
        }

        assertThat(funMatcher.matches(ktCallExpression1, tree.bindingContext)).isTrue
        assertThat(funMatcher.matches(ktCallExpression2, tree.bindingContext)).isTrue
    }

    @Test
    fun `Don't method by type, name and any name regex`() {
        val funMatcher = FunMatcher {
            qualifier = "sample.SampleClass"
            name = "anything"
            nameRegex = "lala".toRegex()
        }

        assertThat(funMatcher.matches(ktCallExpression1, tree.bindingContext)).isFalse
        assertThat(funMatcher.matches(ktCallExpression2, tree.bindingContext)).isFalse
    }

    @Test
    fun `Match method with parameters`() {
        val funMatcher = FunMatcher {
            qualifier = "sample.SampleClass"
            name = "sayHello"
            withArguments("kotlin.String")
        }

        assertThat(funMatcher.matches(ktCallExpression1, tree.bindingContext)).isTrue
        assertThat(funMatcher.matches(ktCallExpression2, tree.bindingContext)).isTrue
    }

    @Test
    fun `Match method with unqualified parameters`() {
        val funMatcher = FunMatcher {
            qualifier = "sample.SampleClass"
            name = "sayHello"
            withArguments(ArgumentMatcher("String", qualified = false))
        }

        assertThat(funMatcher.matches(ktCallExpression1, tree.bindingContext)).isTrue
        assertThat(funMatcher.matches(ktCallExpression2, tree.bindingContext)).isTrue
    }

    @Test
    fun `Match method without type`() {
        val funMatcher = FunMatcher {
            name = "sayHello"
            withArguments("kotlin.String")
        }

        assertThat(funMatcher.matches(ktCallExpression1, tree.bindingContext)).isTrue
        assertThat(funMatcher.matches(ktCallExpression2, tree.bindingContext)).isTrue
    }

    @Test
    fun `Don't match method with wrong number of parameters`() {
        val funMatcher = FunMatcher {
            qualifier = "sample.SampleClass"
            name = "sayHello"
            withArguments("kotlin.String", "kotlin.String")
        }

        assertThat(funMatcher.matches(ktCallExpression1, tree.bindingContext)).isFalse
        assertThat(funMatcher.matches(ktCallExpression2, tree.bindingContext)).isFalse
    }

    @Test
    fun `Don't match method with wrong parameters`() {
        val funMatcher = FunMatcher {
            qualifier = "sample.SampleClass"
            name = "sayHello"
            withArguments("Int")
        }

        assertThat(funMatcher.matches(ktCallExpression1, tree.bindingContext)).isFalse
        assertThat(funMatcher.matches(ktCallExpression2, tree.bindingContext)).isFalse
    }

    @Test
    fun `Don't match method with no parameters matcher`() {
        val funMatcher = FunMatcher {
            qualifier = "sample.SampleClass"
            name = "sayHello"
            withArguments("Int")
            withNoArguments()
        }

        assertThat(funMatcher.matches(ktCallExpression1, tree.bindingContext)).isFalse
        assertThat(funMatcher.matches(ktCallExpression2, tree.bindingContext)).isFalse
    }

    @Test
    fun `Don't request the bindingContext for calls with too many arguments`() {
        val funMatcher1 = FunMatcher { withNoArguments() }
        val funMatcher2 = FunMatcher { withArguments("kotlin.String") }

        val callWithOneArgument = ktCallExpression1.getCall(tree.bindingContext)!!
        val callWithTwoArguments = ktCallExpressionIntAndVararg1.getCall(tree.bindingContext)!!
        val spiedBindingContext = spyk(tree.bindingContext)
        assertThat(funMatcher1.matches(callWithOneArgument, spiedBindingContext)).isFalse
        assertThat(funMatcher2.matches(callWithTwoArguments, spiedBindingContext)).isFalse
        verify { spiedBindingContext wasNot Called }
        unmockkAll()
    }

    @Test
    fun `Match vararg arguments`() {
        var matcher = FunMatcher { withArguments("kotlin.Int", "vararg kotlin.String") }
        assertThat(matcher.matches(ktCallExpressionIntAndVararg1, tree.bindingContext)).isTrue
        assertThat(matcher.matches(ktCallExpressionIntAndVararg2, tree.bindingContext)).isTrue

        matcher= FunMatcher { withArguments("kotlin.Int", "kotlin.String") }
        assertThat(matcher.matches(ktCallExpressionIntAndVararg1, tree.bindingContext)).isFalse
        assertThat(matcher.matches(ktCallExpressionIntAndVararg2, tree.bindingContext)).isFalse

        matcher = FunMatcher { withArguments("kotlin.Int", "kotlin.Array") }
        assertThat(matcher.matches(ktCallExpressionIntAndVararg1, tree.bindingContext)).isFalse
        assertThat(matcher.matches(ktCallExpressionIntAndVararg2, tree.bindingContext)).isFalse

        matcher = FunMatcher { withArguments(ArgumentMatcher(isVararg = false), ArgumentMatcher(isVararg = true)) }
        assertThat(matcher.matches(ktCallExpressionIntAndVararg1, tree.bindingContext)).isTrue
        assertThat(matcher.matches(ktCallExpressionIntAndVararg2, tree.bindingContext)).isTrue

        matcher = FunMatcher { withArguments(ArgumentMatcher(isVararg = false), ArgumentMatcher(typeName = "kotlin.Int", isVararg = true)) }
        assertThat(matcher.matches(ktCallExpressionIntAndVararg1, tree.bindingContext)).isFalse
        assertThat(matcher.matches(ktCallExpressionIntAndVararg2, tree.bindingContext)).isFalse

        matcher = FunMatcher { withArguments(ArgumentMatcher(isVararg = false), ArgumentMatcher(isVararg = false)) }
        assertThat(matcher.matches(ktCallExpressionIntAndVararg1, tree.bindingContext)).isFalse
        assertThat(matcher.matches(ktCallExpressionIntAndVararg2, tree.bindingContext)).isFalse

        matcher = FunMatcher { withArguments("kotlin.String") }
        assertThat(matcher.matches(ktCallExpression1, tree.bindingContext)).isTrue

        matcher = FunMatcher { withArguments("vararg kotlin.String") }
        assertThat(matcher.matches(ktCallExpression1, tree.bindingContext)).isFalse

        matcher = FunMatcher { withArguments(ArgumentMatcher(isVararg = false)) }
        assertThat(matcher.matches(ktCallExpression1, tree.bindingContext)).isTrue

        matcher = FunMatcher { withArguments(ArgumentMatcher(isVararg = true)) }
        assertThat(matcher.matches(ktCallExpression1, tree.bindingContext)).isFalse

        matcher = FunMatcher { withArguments(ArgumentMatcher(typeName = "String", qualified = false, isVararg = false)) }
        assertThat(matcher.matches(ktCallExpression1, tree.bindingContext)).isTrue

        matcher = FunMatcher { withArguments(ArgumentMatcher(typeName = "Int", qualified = false, isVararg = false)) }
        assertThat(matcher.matches(ktCallExpression1, tree.bindingContext)).isFalse

        matcher = FunMatcher { withArguments(ArgumentMatcher(typeName = "String", qualified = false, isVararg = true)) }
        assertThat(matcher.matches(ktCallExpression1, tree.bindingContext)).isFalse
    }

    @Test
    fun `Match method with no parameters matcher`() {
        val funMatcher = FunMatcher {
            qualifier = "sample.SampleClass"
            name = "sayHello"
            withArguments("kotlin.String")
            withNoArguments()
        }

        assertThat(funMatcher.matches(ktCallExpression1, tree.bindingContext)).isTrue
        assertThat(funMatcher.matches(ktCallExpression2, tree.bindingContext)).isTrue
    }

    @Test
    fun `Match method declaration`() {
        val funMatcher = FunMatcher {
            qualifier = "sample.MySampleClass"
            name = "sayHello"
            withArguments("kotlin.String")
            withNoArguments()
        }
        val ktNamedFunction = tree.psiFile.children[5].children[1].children[0] as KtNamedFunction

        assertThat(funMatcher.matches(ktNamedFunction, tree.bindingContext)).isTrue
    }

    @Test
    fun `Don't match method declaration without binding context`() {
        val funMatcher = FunMatcher {
            qualifier = "sample.MySampleClass"
            name = "sayHello"
            withArguments("kotlin.String")
            withNoArguments()
        }
        val ktNamedFunction = tree.psiFile.children[5].children[1].children[0] as KtNamedFunction

        assertThat(funMatcher.matches(ktNamedFunction, BindingContext.EMPTY)).isFalse
    }

    @Test
    fun `Don't match method declaration with wrong parameters`() {
        val funMatcher = FunMatcher {
            qualifier = "sample.MySampleClass"
            name = "sayHello"
            withNoArguments()
        }
        val ktNamedFunction = tree.psiFile.children[5].children[1].children[0] as KtNamedFunction

        assertThat(funMatcher.matches(ktNamedFunction, tree.bindingContext)).isFalse
    }

    @Test
    fun `Match method declaration with type of supertype`() {
        val funMatcher = FunMatcher {
            definingSupertype = "sample.MySampleClass"
            name = "sayHello"
            withArguments("kotlin.String")
        }
        val ktNamedFunction = tree.psiFile.children[5].children[1].children[0] as KtNamedFunction

        assertThat(funMatcher.matches(ktNamedFunction, tree.bindingContext)).isTrue
    }

    @Test
    fun `Don't match method declaration with wrong supertype`() {
        val funMatcher = FunMatcher {
            definingSupertype = "NonexistentClass"
            name = "sayHello"
            withArguments("kotlin.String")
        }
        val ktNamedFunction = tree.psiFile.children[5].children[1].children[0] as KtNamedFunction

        assertThat(funMatcher.matches(ktNamedFunction, tree.bindingContext)).isFalse
    }

    @Test
    fun `Match method declaration by supertype`() {
        val funMatcher = FunMatcher {
            definingSupertype = "sample.MyInterface"
            name = "sayHello"
            withArguments("kotlin.String")
        }
        val ktNamedFunction = tree.psiFile.children[5].children[1].children[0] as KtNamedFunction

        assertThat(funMatcher.matches(ktNamedFunction, tree.bindingContext)).isTrue
    }

    @Test
    fun `Don't match method declaration by supertype without binding context`() {
        val funMatcher = FunMatcher {
            definingSupertype = "sample.MyInterface"
            name = "sayHello"
            withArguments("kotlin.String")
        }
        val ktNamedFunction = tree.psiFile.children[5].children[1].children[0] as KtNamedFunction

        assertThat(funMatcher.matches(ktNamedFunction, BindingContext.EMPTY)).isFalse
    }

    @Test
    fun `Don't match method call without binding context`() {
        val funMatcher = FunMatcher {
            qualifier = "sample.MySampleClass"
            name = "sayHello"
            withArguments("kotlin.String")
        }
        val call = ktCallExpression1.getCall(tree.bindingContext)
        assertThat(funMatcher.matches(call!!, BindingContext.EMPTY)).isFalse
    }

    @Test
    fun `Don't match method call by supertype without binding context`() {
        val funMatcher = FunMatcher {
            definingSupertype = "sample.MyInterface"
            name = "sayHello"
            withArguments("kotlin.String")
        }
        val call = ktCallExpression1.getCall(tree.bindingContext)
        assertThat(funMatcher.matches(call!!, BindingContext.EMPTY)).isFalse
    }

    @Test
    fun `Match constructor`() {
        val funMatcher = ConstructorMatcher {
            qualifier = "sample.SampleClass"
            withNoArguments()
        }

        val callExpression = allCallExpressions[0]

        assertThat(funMatcher.matches(callExpression, tree.bindingContext)).isTrue
    }

    @Test
    fun `Don't match constructor without binding context`() {
        val funMatcher = ConstructorMatcher {
            qualifier = "sample.SampleClass"
            withNoArguments()
        }

        val callExpression = allCallExpressions[0]

        assertThat(funMatcher.matches(callExpression, BindingContext.EMPTY)).isFalse
    }


    @Test
    fun `Don't match constructor`() {
        val funMatcher = ConstructorMatcher {
            qualifier = "sample.SampleClass"
            withArguments("kotlin.String")
        }

        val callExpression = allCallExpressions[0]

        assertThat(funMatcher.matches(callExpression, tree.bindingContext)).isFalse
    }

    @Test
    fun `Don't match the constructor by supertype`() {
        val funMatcher = ConstructorMatcher {
            definingSupertype = "sample.MyInterface"
            withNoArguments()
        }

        val callExpression = allCallExpressions[5]

        assertThat(funMatcher.matches(callExpression, tree.bindingContext)).isFalse
    }

    @Test
    fun `Match only methods with non-nullable paramter`() {
        check(FunMatcher {
            withArguments(ArgumentMatcher(nullability = TypeNullability.NOT_NULL))
        }, true, true, false, false, false, false)
    }

    @Test
    fun `Match only methods with nullable parameter`() {
        check(FunMatcher {
            withArguments(ArgumentMatcher(nullability = TypeNullability.NULLABLE))
        }, false, false, true, false, false, false)
    }

    @Test
    fun `Match methods with a parameter with any nullability`() {
        check(FunMatcher {
            withArguments(ArgumentMatcher(nullability = null))
        }, true, true, true, false, false, false)
    }

    @Test
    fun `Don't match methods without flexible nullability parameter`() {
        check(FunMatcher {
            withArguments(ArgumentMatcher(nullability = TypeNullability.FLEXIBLE))
        }, false, false, false, false, false, false)
    }

    @Test
    fun `Match only suspending methods`() {
        check(FunMatcher(suspending = true), false, false, false, true, false, false)
    }

    @Test
    fun `Match only non-suspending methods`() {
        check(FunMatcher(suspending = false), true, true, true, false, true, true)
    }

    @Test
    fun `Match only extension methods`() {
        check(FunMatcher(extensionFunction = true), false, false, false, true, false, false)
    }

    @Test
    fun `Match only non-extension methods`() {
        check(FunMatcher(extensionFunction = false), true, true, true, false, true, true)
    }

    @Test
    fun `Match only methods returning String`() {
        check(FunMatcher(returnType = "kotlin.String"), false, false, false, true, false, false)
    }

    @Test
    fun `Match only methods returning int`() {
        check(FunMatcher(returnType = "kotlin.Int"), false, false, true, false, false, false)
    }

    @Test
    fun `Match only methods returning Unit`() {
        check(FunMatcher(returnType = "kotlin.Unit"), true, true, false, false, true, true)
    }

    private fun check(funMatcher: FunMatcherImpl, vararg expected: Boolean?) {
        assertThat(expected).hasSameSizeAs(testFunCalls)
        testFunCalls.forEachIndexed { index, callExpression ->
            if (expected[index] != null) {
                assertThat(funMatcher.matches(callExpression, tree.bindingContext))
                    .withFailMessage("Unexpected (mis)match on call expression #$index (expected ${expected[index]})")
                    .isEqualTo(expected[index])
            }
        }
    }
}
