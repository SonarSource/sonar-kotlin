package org.sonarsource.kotlin.plugin

import org.assertj.core.api.Assertions
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtLoopExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtWhenEntry
import org.junit.jupiter.api.Test
import org.sonarsource.kotlin.converter.KotlinConverter

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

    private fun getComplexityTrees(content: String): List<PsiElement> {
        val root = KotlinConverter(emptyList()).parse(content)
        val cyclomaticComplexityVisitor = CyclomaticComplexityVisitor()
        root.psiFile.accept(cyclomaticComplexityVisitor)
        return cyclomaticComplexityVisitor.complexityTrees()
    }
}
