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
package org.sonarsource.kotlin.api.frontend

import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.sonar.api.batch.fs.InputFile
import org.sonarsource.kotlin.api.reporting.KotlinTextRanges.textPointerAtOffset

class KotlinTree(
    val psiFile: KtFile,
    val document: Document,
    val bindingContext: BindingContext,
    val diagnostics: List<Diagnostic>,
    val regexCache: RegexCache,
    val doResolve: Boolean,
)

data class KotlinSyntaxStructure(val ktFile: KtFile, val document: Document, val inputFile: InputFile) {
    companion object {
        @JvmStatic
        fun of(content: String, environment: Environment, inputFile: InputFile): KotlinSyntaxStructure {

            val psiFile: KtFile = if (environment.k2session != null) {
                val inputFilePath = FileUtil.toSystemIndependentName(inputFile.file().path)
                // TODO inefficient
                environment.k2session!!.modulesWithFiles.values.first().find {
                    it.virtualFile.path == inputFilePath
                } as KtFile
            } else
                environment.ktPsiFactory.createFile(inputFile.uri().path, normalizeEol(content))

            val document = try {
                psiFile.viewProvider.document ?: throw ParseException("Cannot extract document")
            } catch (e: AssertionError) {
                // A KotlinLexerException may occur when attempting to read invalid files
                throw ParseException("Cannot correctly map AST with a null Document object")
            }

            checkParsingErrors(psiFile, document, inputFile)

            return KotlinSyntaxStructure(psiFile, document, inputFile)
        }
    }
}

fun checkParsingErrors(psiFile: PsiFile, document: Document, inputFile: InputFile) {
    descendants(psiFile)
        .firstOrNull { it is PsiErrorElement }
        ?.let { element: PsiElement ->
            throw ParseException(
                "Cannot convert file due to syntactic errors",
                inputFile.textPointerAtOffset(document, element.startOffset)
            )
        }
}

private fun descendants(element: PsiElement): Sequence<PsiElement> {
    return element.children.asSequence().flatMap { tree: PsiElement -> sequenceOf(tree) + descendants(tree) }
}

private fun normalizeEol(content: String) = content.replace("""\r\n?""".toRegex(), "\n")
