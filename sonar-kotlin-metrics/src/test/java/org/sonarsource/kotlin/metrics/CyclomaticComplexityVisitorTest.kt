/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2024 SonarSource SA
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
import org.assertj.core.api.Assertions
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtLoopExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtWhenEntry
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.sonarsource.kotlin.api.frontend.Environment

internal class CyclomaticComplexityVisitorTest {

    @Test
    fun `test when expression`() {
        val content =
            """class X(val a: Int) {
                   init {
                       when (a) {      
                           0 -> println("none") 
                           1 -> println("one")
                           2 -> println("many")
                           else -> println("it's complicated")
                       }
                   }
             }""".trimMargin()
        val trees = getComplexityTrees(content)
        Assertions.assertThat(trees)
            .hasSize(4)
            .allMatch { it is KtWhenEntry }
    }

    @Test
    fun `test functions with condition`() {
        val content =
            """fun foo (a: Int) {
                  if (a == 2) {
                    print(a + 1) 
                  } else {
                    print(a)
                  }
               }"""
        val trees = getComplexityTrees(content)
        Assertions.assertThat(trees).hasSize(2)
        Assertions.assertThat(trees[0]).isInstanceOf(KtNamedFunction::class.java)
        Assertions.assertThat(trees[1]).isInstanceOf(LeafPsiElement::class.java)
    }

    @Test
    fun `test binary expressions`() {
        val content =
            """fun foo (a: Int, b: Int) {
                  if (a == 2 || b > 5 && (a + b) <= 10) {
                    print(a + 1) 
                  } else {
                    print(a)
                  }
               }"""
        val trees = getComplexityTrees(content)
        Assertions.assertThat(trees).hasSize(4)
        Assertions.assertThat(trees[0]).isInstanceOf(KtNamedFunction::class.java)
        Assertions.assertThat(trees[1]).isInstanceOf(LeafPsiElement::class.java)
        Assertions.assertThat(trees[2]).isInstanceOf(KtBinaryExpression::class.java)
        Assertions.assertThat(trees[3]).isInstanceOf(KtBinaryExpression::class.java)
    }

    @Test
    fun `test loops`() {
        val content =
            """class X(val list: List<Int>) {
                   init {
                       for (var x in list) {
                           while (x > 0) {
                               x = x + 1
                           }
                       }
                   }
             }""".trimMargin()

        val trees = getComplexityTrees(content)
        Assertions.assertThat(trees)
            .hasSize(2)
            .allMatch { it is KtLoopExpression }
    }

    @Test
    fun `test do while loop`() {
        val content =
            """class X(var x: Int, val y: Int) {
                   init {
                       do { x = x - 1 } while (x > y)
                   }
             }""".trimMargin()

        val trees = getComplexityTrees(content)
        Assertions.assertThat(trees)
            .hasSize(1)
            .allMatch { it is KtLoopExpression }
    }

    @Test
    fun `test anonymous function`() {
        val content =
            """class X() {
                   val decrement = fun(x: Int) { x = x - 1 }
             }""".trimMargin()

        val trees = getComplexityTrees(content)
        Assertions.assertThat(trees).isEmpty()
    }

    @Test
    fun `test function without body`() {
        val content =
            """abstract class X() {
                   fun f(x: Int)
             }""".trimMargin()

        val trees = getComplexityTrees(content)
        Assertions.assertThat(trees).isEmpty()
    }

    private val disposable = Disposer.newDisposable()

    @AfterEach
    fun dispose() {
        Disposer.dispose(disposable)
    }

    private fun getComplexityTrees(content: String): List<PsiElement> {
        val env = Environment(disposable, emptyList(), LanguageVersion.LATEST_STABLE)
        val ktFile = env.ktPsiFactory.createFile(content)
        val cyclomaticComplexityVisitor = CyclomaticComplexityVisitor()
        ktFile.accept(cyclomaticComplexityVisitor)
        return cyclomaticComplexityVisitor.complexityTrees()
    }

}
