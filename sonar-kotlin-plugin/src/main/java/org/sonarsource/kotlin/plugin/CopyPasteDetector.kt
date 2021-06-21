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
package org.sonarsource.kotlin.plugin

import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.psi.KtFileAnnotationList
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtImportList
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.KtStringTemplateEntry
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.sonarsource.kotlin.converter.KotlinTextRanges.commonTextRange
import org.sonarsource.kotlin.visiting.KotlinFileVisitor

class CopyPasteDetector : KotlinFileVisitor() {
    override fun visit(kotlinFileContext: KotlinFileContext) {
        val cpdTokens =
            kotlinFileContext.inputFileContext.sensorContext.newCpdTokens().onFile(kotlinFileContext.inputFileContext.inputFile)

        collectCpdRelevantNodes(kotlinFileContext.ktFile).forEach { node ->
            val text = if (node is KtStringTemplateEntry) "LITERAL" else node.text
            cpdTokens.addToken(kotlinFileContext.commonTextRange(node), text)
        }

        cpdTokens.save()
    }

    private fun collectCpdRelevantNodes(node: PsiElement, acc: MutableList<PsiElement> = mutableListOf()): List<PsiElement> {
        if (!isExcludedFromCpd(node)) {
            if ((node is LeafPsiElement && node !is PsiWhiteSpace) || node is KtStringTemplateEntry) {
                acc.add(node)
            } else {
                node.allChildren.forEach { collectCpdRelevantNodes(it, acc) }
            }
        }

        return acc
    }

    private fun isExcludedFromCpd(node: PsiElement) =
        node is KtPackageDirective ||
            node is KtImportList ||
            node is KtImportDirective ||
            node is KtFileAnnotationList ||
            node is PsiWhiteSpace ||
            node is PsiComment ||
            node is KDoc
}
