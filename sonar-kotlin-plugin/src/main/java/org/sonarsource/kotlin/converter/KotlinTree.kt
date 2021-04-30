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
import org.jetbrains.kotlin.resolve.BindingContext
import org.sonarsource.slang.api.ParseException
import org.sonarsource.slang.api.TopLevelTree
import org.sonarsource.slang.impl.TreeMetaDataProvider

class KotlinTree(
    val psiFile: KtFile,
    val document: Document?,
    val metaDataProvider: TreeMetaDataProvider,
    val bindingContext: BindingContext,
    private val slangAst: TopLevelTree,
) : TopLevelTree by slangAst {
    companion object {
        @JvmStatic
        fun of(content: String, environment: Environment): KotlinTree {
            val psiFile: KtFile = environment.ktPsiFactory.createFile(normalizeEol(content))
            val document = try {
                psiFile.viewProvider.document
            } catch (e: AssertionError) {
                // A KotlinLexerException may occur when attempting to read invalid files
                throw ParseException("Cannot correctly map AST with a null Document object")
            }

            val metaDataProvider = CommentAnnotationsAndTokenVisitor(document).let { commentsAndTokens ->
                psiFile.accept(commentsAndTokens)
                TreeMetaDataProvider(commentsAndTokens.allComments, commentsAndTokens.tokens, commentsAndTokens.allAnnotations)
            }

            checkParsingErrors(psiFile, document, metaDataProvider)
            val bindingContext = bindingContext(
                environment.env,
                environment.classpath,
                listOf(psiFile),
            )
            val slangAst = KotlinTreeVisitor(psiFile, metaDataProvider).sLangAST as TopLevelTree

            return KotlinTree(psiFile, document, metaDataProvider, bindingContext, slangAst)
        }

        private fun descendants(element: PsiElement): Sequence<PsiElement> {
            return element.children.asSequence().flatMap { tree: PsiElement -> sequenceOf(tree) + descendants(tree) }
        }

        private fun checkParsingErrors(psiFile: PsiFile, document: Document?, metaDataProvider: TreeMetaDataProvider) {
            descendants(psiFile)
                .firstOrNull { it is PsiErrorElement }
                ?.let { element: PsiElement ->
                    throw ParseException(
                        "Cannot convert file due to syntactic errors",
                        getErrorLocation(document, metaDataProvider, element)
                    )
                }
        }

        private fun getErrorLocation(document: Document?, metaDataProvider: TreeMetaDataProvider, element: PsiElement) =
            metaDataProvider.metaData(KotlinTextRanges.textRange(document!!, element)).textRange().start()

        private fun normalizeEol(content: String) = content.replace("""\r\n?""".toRegex(), "\n")
    }
}
