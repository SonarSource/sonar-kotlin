/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2023 SonarSource SA
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
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtContinueExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtExpressionWithLabel
import org.jetbrains.kotlin.psi.KtFinallySection
import org.jetbrains.kotlin.psi.KtLabeledExpression
import org.jetbrains.kotlin.psi.KtLoopExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtThrowExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.plugin.KotlinFileContext
import java.util.Stack

@Rule(key = "S1143")
class JumpInFinallyCheck : AbstractCheck() {

    override fun visitFinallySection(finallySection: KtFinallySection, kotlinFileContext: KotlinFileContext) {
        finallySection.accept(FinallyBlockVisitor {
            kotlinFileContext.reportIssue(it.firstChild, it.buildReportMessage())
        })
    }
}

private class FinallyBlockVisitor(private val report: (KtExpression) -> Unit) : KtTreeVisitorVoid() {
    private var loopDepthCounter = 0
    private var functionDepthCounter = 0
    private val stackedLabels = Stack<String>()
    private var alreadyEnteredFinallyBlock = false

    override fun visitLabeledExpression(expression: KtLabeledExpression) {
        // Because we enter a labeled expression, we can assume a label is set
        stackedLabels.push(expression.name)
        super.visitLabeledExpression(expression)
        stackedLabels.pop()
    }

    override fun visitFinallySection(finallySection: KtFinallySection) {
        if (!alreadyEnteredFinallyBlock) {
            alreadyEnteredFinallyBlock = true
            super.visitFinallySection(finallySection)
        }
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        stackedLabels.push(expression.firstChild.text)
        super.visitCallExpression(expression)
        stackedLabels.pop()
    }

    override fun visitLoopExpression(loopExpression: KtLoopExpression) {
        loopDepthCounter++
        super.visitLoopExpression(loopExpression)
        loopDepthCounter--
    }

    override fun visitNamedFunction(function: KtNamedFunction) {
        functionDepthCounter++
        super.visitNamedFunction(function)
        functionDepthCounter--
    }

    override fun visitBreakExpression(expression: KtBreakExpression) {
        checkAndVisit(loopDepthCounter, expression)
    }

    override fun visitContinueExpression(expression: KtContinueExpression) {
        checkAndVisit(loopDepthCounter, expression)
    }

    override fun visitReturnExpression(expression: KtReturnExpression) {
        checkAndVisit(functionDepthCounter, expression)
    }

    override fun visitThrowExpression(expression: KtThrowExpression) {
        if (functionDepthCounter == 0) {
            report(expression)
        }
    }

    private fun checkAndVisit(depthCounter: Int, expression: KtExpressionWithLabel) {
        if (expression.labelQualifier != null) {
            // At this point we are certain to have a label
            if (expression.getLabelName()!! !in stackedLabels) {
                report(expression)
            }
        } else if (depthCounter == 0) {
            report(expression)
        }
        visitExpressionWithLabel(expression)
    }
}

private fun KtExpression.buildReportMessage(): String {
    val keyword = when (this) {
        is KtReturnExpression -> "return"
        is KtBreakExpression -> "break"
        is KtContinueExpression -> "continue"
        else -> "throw"
    }
    return "Remove this $keyword statement from this finally block."
}
