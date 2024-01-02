/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2024 SonarSource SA
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
package org.sonarsource.kotlin.plugin.cpd

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
import org.slf4j.LoggerFactory
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.fs.TextRange
import org.sonar.api.batch.sensor.SensorContext
import org.sonarsource.kotlin.api.checks.hasCacheEnabled
import org.sonarsource.kotlin.api.reporting.KotlinTextRanges.textRange
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.KotlinFileVisitor

private val LOG = LoggerFactory.getLogger(CopyPasteDetector::class.java)

class CopyPasteDetector : KotlinFileVisitor() {
    override fun visit(kotlinFileContext: KotlinFileContext) {
        val sensorContext = kotlinFileContext.inputFileContext.sensorContext
        val cpdTokens = sensorContext.newCpdTokens().onFile(kotlinFileContext.inputFileContext.inputFile)

        val tokens = collectCpdRelevantNodes(kotlinFileContext.ktFile).map { node ->
            val text = if (node is KtStringTemplateEntry) "LITERAL" else node.text
            val cpdToken = CPDToken(kotlinFileContext.textRange(node), text)
            cpdTokens.addToken(cpdToken.range, cpdToken.text)
            cpdToken
        }

        cpdTokens.save()

        cacheTokensForNextAnalysis(sensorContext, kotlinFileContext.inputFileContext.inputFile, tokens)
    }

    private fun collectCpdRelevantNodes(
        node: PsiElement,
        acc: MutableList<PsiElement> = mutableListOf()
    ): List<PsiElement> {
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

    private fun cacheTokensForNextAnalysis(sensorContext: SensorContext, inputFile: InputFile, tokens: List<CPDToken>) {
        if (sensorContext.hasCacheEnabled()) {
            LOG.trace("Caching ${tokens.size} CPD tokens for next analysis of input file ${inputFile.key()}.")
            val nextCache = sensorContext.nextCache()
            nextCache.storeCPDTokens(inputFile, tokens)
        } else {
            LOG.trace("No CPD tokens cached for next analysis of input file moduleKey:dummy.kt.")
        }
    }
}

data class CPDToken(val range: TextRange, val text: String)
