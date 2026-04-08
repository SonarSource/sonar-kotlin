/*
 * SonarSource Kotlin
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
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
        fun of(environment: Environment, inputFile: InputFile, virtualFile: KotlinVirtualFile): KotlinSyntaxStructure {

            // TODO SONARKT-711 improve performance, see also
            // https://github.com/Kotlin/analysis-api/commit/eea50c3d826584461e7bb0087deb9f0d9b55eb8c
            // which requires Kotlin 2.1.20
            // https://github.com/JetBrains/kotlin/commit/774d253de8263e284f045a452369a7308d495d03
            // At the beginning of Analysis API, the only way to retrieve KtFile from k2session was to use modulesWithFiles.
            // Currently, the API already allows analysis of String (i.e. in-memory file) and not KtFile.
            // Maybe, we can avoid creation of Kotlin VirtualFiles completely, but this needs to be investigated.
            val psiFile = environment.k2session!!.modulesWithFiles.values.first().find {
                it.virtualFile == virtualFile
            }

            if (psiFile == null) {
                throw ParseException("Cannot find KtFile for virtual file ${virtualFile.path}")
            } else if (psiFile !is KtFile) {
                throw ParseException("Cannot find KtFile for virtual file ${virtualFile.path}, found a file of type ${psiFile::class.java}: $psiFile")
            }

            val document = try {
                psiFile.viewProvider.document ?: throw ParseException("Cannot extract document")
            } catch (_: AssertionError) {
                // A KotlinLexerException may occur when attempting to read invalid files
                throw ParseException("Cannot correctly map AST with a null Document object")
            }

            checkParsingErrors(psiFile, document, inputFile)

            return KotlinSyntaxStructure(psiFile, document, inputFile)
        }
    }
}

fun checkParsingErrors(psiFile: PsiFile, document: Document, inputFile: InputFile) = descendants(psiFile)
    .firstIsInstanceOrNull<PsiErrorElement>()
    ?.let { element ->
        throw ParseException(
            "Cannot convert file due to syntactic errors",
            inputFile.textPointerAtOffset(document, element.startOffset)
        )
    }

private fun descendants(element: PsiElement): Sequence<PsiElement> =
    element.children.asSequence().flatMap { tree -> sequenceOf(tree) + descendants(tree) }
