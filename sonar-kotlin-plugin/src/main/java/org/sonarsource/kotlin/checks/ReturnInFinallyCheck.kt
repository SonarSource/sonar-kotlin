/*
 * SonarSource Kotlin
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
package org.sonarsource.kotlin.checks

import org.jetbrains.kotlin.psi.KtBreakExpression
import org.jetbrains.kotlin.psi.KtContinueExpression
import org.jetbrains.kotlin.psi.KtDoWhileExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpressionWithLabel
import org.jetbrains.kotlin.psi.KtFinallySection
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtLabeledExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtThrowExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.KtWhileExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.plugin.KotlinFileContext

/**
 * Checks if the label in the element points to an unexpected label
 */
private fun isEscapingTheBlock(element: KtExpressionWithLabel, expectedLabels: List<String>): Boolean {
    val label = element.getLabelName() ?: return false
    return label !in expectedLabels
}

private fun buildReportMessage(element: KtElement) =
    "Remove this %s statement from this finally block.".format(element.toString().lowercase())

@Rule(key = "S1143")
class ReturnInFinallyCheck : AbstractCheck() {

    override fun visitFinallySection(finallySection: KtFinallySection, kotlinFileContext: KotlinFileContext) {
        val report: (KtElement) -> Unit = {
            kotlinFileContext.reportIssue(it, buildReportMessage(it))
        }

        val visitor = FinallyBlockVisitor(report)
        finallySection.accept(visitor)
        visitor.jumpsToLabel
            .filter { isEscapingTheBlock(it, visitor.labelsInFinallyBlock) }
            .forEach(report)

        finallySection
            .collectDescendantsOfType<KtThrowExpression>()
            .forEach(report)
    }

    private class FinallyBlockVisitor(private val report: (KtElement) -> Unit) : KtTreeVisitorVoid() {
        private var loopDepthCounter = 0
        private var lambdaDepthCounter = 0
        val labelsInFinallyBlock = ArrayList<String>()
        val jumpsToLabel = ArrayList<KtExpressionWithLabel>()

        override fun visitLabeledExpression(expression: KtLabeledExpression) {
            expression.name?.let { labelsInFinallyBlock.add(it) }
            super.visitLabeledExpression(expression)
        }

        override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
            lambdaDepthCounter++
            super.visitLambdaExpression(lambdaExpression)
            lambdaDepthCounter--
        }

        override fun visitDoWhileExpression(expression: KtDoWhileExpression) {
            loopDepthCounter++
            super.visitDoWhileExpression(expression)
            loopDepthCounter--
        }

        override fun visitForExpression(expression: KtForExpression) {
            loopDepthCounter++
            super.visitForExpression(expression)
            loopDepthCounter--
        }

        override fun visitWhileExpression(expression: KtWhileExpression) {
            loopDepthCounter++
            super.visitWhileExpression(expression)
            loopDepthCounter--
        }

        override fun visitBreakExpression(expression: KtBreakExpression) {
            if (loopDepthCounter == 0) {
                report(expression)
            } else if (expression.labelQualifier != null) {
                jumpsToLabel.add(expression)
            }
            super.visitBreakExpression(expression)
        }

        override fun visitContinueExpression(expression: KtContinueExpression) {
            if (loopDepthCounter == 0) {
                report(expression)
            } else if (expression.labelQualifier != null) {
                jumpsToLabel.add(expression)
            }
            super.visitContinueExpression(expression)
        }

        override fun visitReturnExpression(expression: KtReturnExpression) {
            if (lambdaDepthCounter == 0) {
                report(expression)
            }
            super.visitReturnExpression(expression)
        }
    }
}
