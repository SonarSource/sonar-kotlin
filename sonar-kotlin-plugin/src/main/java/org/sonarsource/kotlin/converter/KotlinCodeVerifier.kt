/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2022 SonarSource SA
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
package org.sonarsource.kotlin.converter

import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiErrorElement
import org.jetbrains.kotlin.com.intellij.psi.PsiFile
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBinaryExpressionWithTypeRHS
import org.jetbrains.kotlin.psi.KtCollectionLiteralExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtIsExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtOperationReferenceExpression
import org.jetbrains.kotlin.psi.KtParenthesizedExpression
import org.jetbrains.kotlin.psi.KtPostfixExpression
import org.jetbrains.kotlin.psi.KtPrefixExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtThisExpression
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType

class KotlinCodeVerifier {

    companion object {
        private val KDOC_TAGS = listOf(
            "@param", "@name", "@return", "@constructor", "@receiver", "@property", "@throws",
            "@exception", "@sample", "@see", "@author", "@since", "@suppress")

        private fun isKDoc(content: String) = KDOC_TAGS.any { content.lowercase().contains(it) }

        // Filter natural language sentences parsed
        // as literals, infix notations or single expressions
        private fun isSimpleExpression(tree: PsiFile) =
            // FIXME  Since kotlin 1.3, compiler adds 2 hidden elements in the hierarchy: a `KtScript`, having a `KtBlockExpression`
            tree.lastChild.lastChild.children.let { elements ->
                elements.all { element: PsiElement ->
                    element is KtNameReferenceExpression ||
                        element is KtCollectionLiteralExpression ||
                        element is KtConstantExpression ||
                        element is KtIsExpression ||
                        element is KtThisExpression ||
                        element is KtStringTemplateExpression ||
                        isInfixNotation(element)
                } || isSingleExpression(elements)
            }

        private fun removeParenthesizedExpressions(elements: Array<PsiElement>) = elements
            .filter { element: PsiElement? -> element !is KtParenthesizedExpression }

        // Check for strings parsed as a single expression
        // e.g. "this is fine" as IsExpression, "-- foo" as InfixExpression
        private fun isSingleExpression(elements: Array<PsiElement>) =
            removeParenthesizedExpressions(elements).let { elementsWithoutParenthesis ->
                when {
                    elementsWithoutParenthesis.isEmpty() -> true
                    elementsWithoutParenthesis.size > 1 -> false
                    else -> elementsWithoutParenthesis[0].let { element ->
                        element is KtPrefixExpression ||
                            element is KtPostfixExpression ||
                            element is KtBinaryExpression ||
                            element is KtBinaryExpressionWithTypeRHS ||
                            element is KtDotQualifiedExpression
                    }
                }
            }


        // Kotlin supports infix function invocation like `1 shl 2` instead of `1.shl(2)`
        // A regular three words sentence would be parsed as infix notation by Kotlin
        private fun isInfixNotation(element: PsiElement) =
            element is KtBinaryExpression && element.getChildren().let { binaryExprChildren ->
                binaryExprChildren.size == 3 && binaryExprChildren[1] is KtOperationReferenceExpression
            }
    }

    fun containsCode(content: String): Boolean {
        val words = content.trim().split(Regex("\\w+")).filter { it.isNotBlank() }.size
        return if (words < 2 || isKDoc(content)) {
            false
        } else {
            val environment = Environment(emptyList(), LanguageVersion.LATEST_STABLE)
            try {
                val wrappedContent = "fun function () { $content }"

                val ktFile = environment.ktPsiFactory.createFile(wrappedContent)

                ktFile.findDescendantOfType<PsiErrorElement>() == null && ! isSimpleExpression(ktFile)
            } catch (e: Exception) {
                false
            } finally {
                Disposer.dispose(environment.disposable)
            }
        }
    }
}
