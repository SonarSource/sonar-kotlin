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
package org.sonarsource.kotlin.api

import java.nio.file.Files
import java.nio.file.Paths
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.resolve.BindingContext
import org.junit.jupiter.api.Test
import org.sonarsource.kotlin.converter.Environment
import org.sonarsource.kotlin.converter.KotlinTree

class FunMatcherTest {
    val environment = Environment(listOf("../kotlin-checks-test-sources/build/classes/kotlin/main"))
    val path = Paths.get("../kotlin-checks-test-sources/src/main/kotlin/sample/functions.kt")
    val content = String(Files.readAllBytes(path))
    val tree = KotlinTree.of(content, environment)
    val ktCallExpression1 = tree.psiFile.children[3].children[1].children[1].children[1] as KtCallExpression
    val ktCallExpression2 = tree.psiFile.children[3].children[1].children[2].children[1] as KtCallExpression

    @Test
    fun `match method by type and name`() {
        val funMatcher = FunMatcher {
            type = "sample.SampleClass"
            names = listOf("sayHello")
        }

        assertThat(funMatcher.matches(ktCallExpression1, tree.bindingContext)).isTrue
        assertThat(funMatcher.matches(ktCallExpression2, tree.bindingContext)).isTrue
    }

    @Test
    fun `Don't match method by type or name`() {
        val wrongTypeMethodMatcher = FunMatcher {
            type = "sample.MySampleClass"
            names = listOf("sayHello")
        }

        val wrongNameMethodMatcher = FunMatcher {
            type = "sample.SampleClass"
            names = listOf("sayBye")
        }

        assertThat(wrongTypeMethodMatcher.matches(ktCallExpression1, tree.bindingContext)).isFalse
        assertThat(wrongTypeMethodMatcher.matches(ktCallExpression2, tree.bindingContext)).isFalse

        assertThat(wrongNameMethodMatcher.matches(ktCallExpression1, tree.bindingContext)).isFalse
        assertThat(wrongNameMethodMatcher.matches(ktCallExpression2, tree.bindingContext)).isFalse
    }

    @Test
    fun `Don't match method without binding context`() {
        val funMatcher = FunMatcher {
            type = "sample.SampleClass"
            names = listOf("sayHello")
        }

        assertThat(funMatcher.matches(ktCallExpression1, BindingContext.EMPTY)).isFalse
        assertThat(funMatcher.matches(ktCallExpression2, BindingContext.EMPTY)).isFalse
    }

    @Test
    fun `Match method with parameters`() {
        val funMatcher = FunMatcher {
            type = "sample.SampleClass"
            names = listOf("sayHello")
            withArguments("String")
        }

        assertThat(funMatcher.matches(ktCallExpression1, tree.bindingContext)).isTrue
        assertThat(funMatcher.matches(ktCallExpression2, tree.bindingContext)).isTrue
    }

    @Test
    fun `Match method without type`() {
        val funMatcher = FunMatcher {
            names = listOf("sayHello")
            withArguments("String")
        }

        assertThat(funMatcher.matches(ktCallExpression1, tree.bindingContext)).isTrue
        assertThat(funMatcher.matches(ktCallExpression2, tree.bindingContext)).isTrue
    }

    @Test
    fun `Don't match method with wrong number of parameters`() {
        val funMatcher = FunMatcher {
            type = "sample.SampleClass"
            names = listOf("sayHello")
            withArguments("String", "String")
        }

        assertThat(funMatcher.matches(ktCallExpression1, tree.bindingContext)).isFalse
        assertThat(funMatcher.matches(ktCallExpression2, tree.bindingContext)).isFalse
    }

    @Test
    fun `Don't match method with wrong parameters`() {
        val funMatcher = FunMatcher {
            type = "sample.SampleClass"
            names = listOf("sayHello")
            withArguments("Int")
        }

        assertThat(funMatcher.matches(ktCallExpression1, tree.bindingContext)).isFalse
        assertThat(funMatcher.matches(ktCallExpression2, tree.bindingContext)).isFalse
    }

    @Test
    fun `Don't match method with no parameters matcher`() {
        val funMatcher = FunMatcher {
            type = "sample.SampleClass"
            names = listOf("sayHello")
            withArguments("Int")
            withNoArguments()
        }

        assertThat(funMatcher.matches(ktCallExpression1, tree.bindingContext)).isFalse
        assertThat(funMatcher.matches(ktCallExpression2, tree.bindingContext)).isFalse
    }

    @Test
    fun `Match method with no parameters matcher`() {
        val funMatcher = FunMatcher {
            type = "sample.SampleClass"
            names = listOf("sayHello")
            withArguments("String")
            withNoArguments()
        }

        assertThat(funMatcher.matches(ktCallExpression1, tree.bindingContext)).isTrue
        assertThat(funMatcher.matches(ktCallExpression2, tree.bindingContext)).isTrue
    }

    @Test
    fun `Match method declaration`() {
        val funMatcher = FunMatcher {
            type = "sample.MySampleClass"
            names = listOf("sayHello")
            withArguments("String")
            withNoArguments()
        }
        val ktNamedFunction = tree.psiFile.children[5].children[1].children[0] as KtNamedFunction

        assertThat(funMatcher.matches(ktNamedFunction, tree.bindingContext)).isTrue
    }

    @Test
    fun `Don't match method declaration without binding context`() {
        val funMatcher = FunMatcher {
            type = "sample.MySampleClass"
            names = listOf("sayHello")
            withArguments("String")
            withNoArguments()
        }
        val ktNamedFunction = tree.psiFile.children[5].children[1].children[0] as KtNamedFunction

        assertThat(funMatcher.matches(ktNamedFunction, BindingContext.EMPTY)).isFalse
    }

    @Test
    fun `Don't match method declaration with wrong parameters`() {
        val funMatcher = FunMatcher {
            type = "sample.MySampleClass"
            names = listOf("sayHello")
            withNoArguments()
        }
        val ktNamedFunction = tree.psiFile.children[5].children[1].children[0] as KtNamedFunction

        assertThat(funMatcher.matches(ktNamedFunction, tree.bindingContext)).isFalse
    }

    @Test
    fun `Don't match method declaration with wrong supertype`() {
        val funMatcher = FunMatcher {
            supertype = "sample.MySampleClass"
            names = listOf("sayHello")
            withArguments("String")
        }
        val ktNamedFunction = tree.psiFile.children[5].children[1].children[0] as KtNamedFunction

        assertThat(funMatcher.matches(ktNamedFunction, tree.bindingContext)).isFalse
    }

    @Test
    fun `Match method declaration by supertype`() {
        val funMatcher = FunMatcher {
            supertype = "sample.MyInterface"
            names = listOf("sayHello")
            withArguments("String")
        }
        val ktNamedFunction = tree.psiFile.children[5].children[1].children[0] as KtNamedFunction

        assertThat(funMatcher.matches(ktNamedFunction, tree.bindingContext)).isTrue
    }

    @Test
    fun `Don't match method declaration by supertype without binding context`() {
        val funMatcher = FunMatcher {
            supertype = "sample.MyInterface"
            names = listOf("sayHello")
            withArguments("String")
        }
        val ktNamedFunction = tree.psiFile.children[5].children[1].children[0] as KtNamedFunction

        assertThat(funMatcher.matches(ktNamedFunction, BindingContext.EMPTY)).isFalse
    }

    @Test
    fun `Match constructor`() {
        val funMatcher = ConstructorMatcher {
            type = "sample.SampleClass"
            withNoArguments()
        }

        val callExpression = tree.psiFile.children[3].children[1].children[0].children[0] as KtCallExpression

        assertThat(funMatcher.matches(callExpression, tree.bindingContext)).isTrue
    }

    @Test
    fun `Don't match constructor without binding context`() {
        val funMatcher = ConstructorMatcher {
            type = "sample.SampleClass"
            withNoArguments()
        }

        val callExpression = tree.psiFile.children[3].children[1].children[0].children[0] as KtCallExpression

        assertThat(funMatcher.matches(callExpression, BindingContext.EMPTY)).isFalse
    }



    @Test
    fun `Don't match constructor`() {
        val funMatcher = ConstructorMatcher {
            type = "sample.SampleClass"
            withArguments("String")
        }

        val callExpression = tree.psiFile.children[3].children[1].children[0].children[0] as KtCallExpression

        assertThat(funMatcher.matches(callExpression, tree.bindingContext)).isFalse
    }

    @Test
    fun `Don't match the constructor by supertype`() {
        val funMatcher = ConstructorMatcher {
            supertype = "sample.MyInterface"
            withNoArguments()
        }

        val callExpression = tree.psiFile.children[3].children[1].children[3].children[0] as KtCallExpression

        assertThat(funMatcher.matches(callExpression, tree.bindingContext)).isFalse
    }
}
