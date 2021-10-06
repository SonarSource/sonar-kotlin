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

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtBreakExpression
import org.jetbrains.kotlin.psi.KtContinueExpression
import org.jetbrains.kotlin.psi.KtExpressionWithLabel
import org.jetbrains.kotlin.psi.KtFinallySection
import org.jetbrains.kotlin.psi.KtLabeledExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtLoopExpression
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtThrowExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.plugin.KotlinFileContext

private class FinallyBlockVisitor(private val report: (PsiElement) -> Unit) : KtTreeVisitorVoid() {
    private var loopDepthCounter = 0
    private var lambdaDepthCounter = 0
    private val labelsInFinallyBlock = ArrayList<String>()
    private val jumpsToLabel = ArrayList<KtExpressionWithLabel>()

    override fun visitFinallySection(finallySection: KtFinallySection) {
        super.visitFinallySection(finallySection)
        jumpsToLabel
            .filter { isEscapingTheBlock(it, labelsInFinallyBlock) }
            .forEach(report)
    }

    override fun visitLabeledExpression(expression: KtLabeledExpression) {
        expression.name?.let { labelsInFinallyBlock.add(it) }
        super.visitLabeledExpression(expression)
    }

    override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
        lambdaDepthCounter++
        super.visitLambdaExpression(lambdaExpression)
        lambdaDepthCounter--
    }

    override fun visitLoopExpression(loopExpression: KtLoopExpression) {
        loopDepthCounter++
        super.visitLoopExpression(loopExpression)
        loopDepthCounter--
    }

    override fun visitBreakExpression(expression: KtBreakExpression) {
        visitAndCheck(loopDepthCounter, expression)
    }

    override fun visitContinueExpression(expression: KtContinueExpression) {
        visitAndCheck(loopDepthCounter, expression)
    }

    override fun visitReturnExpression(expression: KtReturnExpression) {
        visitAndCheck(lambdaDepthCounter, expression)
    }

    override fun visitThrowExpression(expression: KtThrowExpression) {
        report(expression)
    }

    private fun visitAndCheck(depthCounter: Int, expression: KtExpressionWithLabel) {
        if (depthCounter == 0) {
            report(expression)
        } else if (expression.labelQualifier != null) {
            jumpsToLabel.add(expression)
        }
        super.visitExpressionWithLabel(expression)
    }
}

@Rule(key = "S1143")
class ReturnInFinallyCheck : AbstractCheck() {

    override fun visitFinallySection(finallySection: KtFinallySection, kotlinFileContext: KotlinFileContext) {
        val visitor = FinallyBlockVisitor { kotlinFileContext.reportIssue(it.firstChild, it.buildReportMessage()) }
        finallySection.accept(visitor)
    }
}

/**
 * Checks if the label in the element points to an unexpected label
 */
private fun isEscapingTheBlock(element: KtExpressionWithLabel, expectedLabels: List<String>): Boolean {
    // Because we only call this function on expressions that have a label, we can assume this is safe
    val label = element.getLabelName()!!
    return label !in expectedLabels
}

private fun PsiElement.buildReportMessage(): String {
    val keyword = when (this) {
        is KtReturnExpression -> "return"
        is KtBreakExpression -> "break"
        is KtContinueExpression -> "continue"
        else -> "throw"
    }
    return "Remove this $keyword statement from this finally block."
}
