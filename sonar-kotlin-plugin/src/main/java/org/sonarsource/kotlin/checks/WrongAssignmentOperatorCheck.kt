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
package org.sonarsource.kotlin.checks

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtUnaryExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.converter.KotlinTextRanges
import org.sonarsource.kotlin.plugin.KotlinFileContext
import org.sonarsource.slang.api.TextRange
import org.sonarsource.slang.impl.TextRanges

/**
 * Replacement for [org.sonarsource.slang.checks.WrongAssignmentOperatorCheck]
 */
@Rule(key = "S2757")
class WrongAssignmentOperatorCheck : AbstractCheck() {
    companion object {
        private val SUSPICIOUS_UNARY_OPERATORS = listOf(
            KtTokens.EXCL,
            KtTokens.PLUS,
            KtTokens.MINUS,
        )

        private fun getMessage(expression: KtUnaryExpression): String {
            return if (expression.operationToken == KtTokens.EXCL) {
                // For expressions such as "b =! c" we want to display the other message
                "Add a space between \"=\" and \"!\" to avoid confusion."
            } else "Was \"${expression.operationReference.text}=\" meant instead?"
        }

        private fun hasSpacingBetween(firstToken: TextRange, secondToken: TextRange): Boolean {
            return (firstToken.end().line() != secondToken.start().line()
                || firstToken.end().lineOffset() != secondToken.start().lineOffset())
        }
    }

    override fun visitBinaryExpression(expression: KtBinaryExpression, context: KotlinFileContext) {
        if (expression.operationToken != KtTokens.EQ) return
        val rightExpression = expression.right!!
        if (rightExpression !is KtUnaryExpression || !SUSPICIOUS_UNARY_OPERATORS.contains(rightExpression.operationToken)) return

        val unaryOperation = rightExpression.operationReference
        val psiDocument = context.ktFile.viewProvider.document!!

        val leftTextRange = KotlinTextRanges.textRange(psiDocument, expression.left!!)
        val rightTextRange = KotlinTextRanges.textRange(psiDocument, unaryOperation)
        val opTextRange = KotlinTextRanges.textRange(psiDocument, expression.operationReference)

        if (!hasSpacingBetween(opTextRange, rightTextRange) && hasSpacingBetween(leftTextRange, opTextRange)) {
            val range = TextRanges.merge(listOf(opTextRange, rightTextRange))
            context.reportIssue(range, getMessage(rightExpression))
        }
    }
}
