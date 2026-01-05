/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2026 SonarSource SA
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
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.sonar.api.batch.fs.InputFile
import org.sonarsource.kotlin.api.reporting.KotlinTextRanges.textPointerAtOffset

class KotlinTree(
    val psiFile: KtFile,
    val document: Document,
) {
    val regexCache = RegexCache()
}

data class KotlinSyntaxStructure(val ktFile: KtFile, val document: Document, val inputFile: InputFile) {
    companion object {
        @JvmStatic
        fun of(content: String, environment: Environment, inputFile: InputFile): KotlinSyntaxStructure {

            val inputFilePath = FileUtil.toSystemIndependentName(inputFile.file().path)
            // TODO improve performance, see also
            // https://github.com/Kotlin/analysis-api/commit/eea50c3d826584461e7bb0087deb9f0d9b55eb8c
            // which requires Kotlin 2.1.20
            // https://github.com/JetBrains/kotlin/commit/774d253de8263e284f045a452369a7308d495d03
            val psiFile: KtFile = environment.k2session!!.modulesWithFiles.values.first().find {
                it.virtualFile.path == inputFilePath
            } as KtFile

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
