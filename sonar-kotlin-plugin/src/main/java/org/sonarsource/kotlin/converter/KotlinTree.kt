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
package org.sonarsource.kotlin.converter

import org.jetbrains.kotlin.com.intellij.openapi.editor.Document
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiErrorElement
import org.jetbrains.kotlin.com.intellij.psi.PsiFile
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.sonar.api.batch.fs.InputFile
import org.sonarsource.kotlin.api.ParseException
import org.sonarsource.kotlin.converter.KotlinTextRanges.textPointerAtOffset

class KotlinTree private constructor(
    val psiFile: KtFile,
    val document: Document,
    val bindingContext: BindingContext,
) {
    companion object {
        @JvmStatic
        fun of(content: String, environment: Environment, inputFile: InputFile): KotlinTree {
            val psiFile: KtFile = environment.ktPsiFactory.createFile(normalizeEol(content))
            val document = try {
                psiFile.viewProvider.document ?: throw ParseException("Cannot extract document")
            } catch (e: AssertionError) {
                // A KotlinLexerException may occur when attempting to read invalid files
                throw ParseException("Cannot correctly map AST with a null Document object")
            }

            checkParsingErrors(psiFile, document, inputFile)
            val bindingContext = bindingContext(
                environment.env,
                environment.classpath,
                listOf(psiFile),
            )

            return KotlinTree(psiFile, document, bindingContext)
        }

        private fun descendants(element: PsiElement): Sequence<PsiElement> {
            return element.children.asSequence().flatMap { tree: PsiElement -> sequenceOf(tree) + descendants(tree) }
        }

        private fun checkParsingErrors(psiFile: PsiFile, document: Document, inputFile: InputFile) {
            descendants(psiFile)
                .firstOrNull { it is PsiErrorElement }
                ?.let { element: PsiElement ->
                    throw ParseException(
                        "Cannot convert file due to syntactic errors",
                        inputFile.textPointerAtOffset(document, element.startOffset)
                    )
                }
        }

        private fun normalizeEol(content: String) = content.replace("""\r\n?""".toRegex(), "\n")
    }
}
