/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2024 SonarSource SA
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
package org.sonarsource.kotlin.checks

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtUnaryExpression
import org.sonar.api.batch.fs.TextRange
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.reporting.KotlinTextRanges.merge
import org.sonarsource.kotlin.api.reporting.KotlinTextRanges.textRange
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

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

        val leftTextRange = context.textRange(expression.left!!)
        val rightTextRange = context.textRange(unaryOperation)
        val opTextRange = context.textRange(expression.operationReference)

        if (!hasSpacingBetween(opTextRange, rightTextRange) && hasSpacingBetween(leftTextRange, opTextRange)) {
            val range = context.merge(listOf(opTextRange, rightTextRange))
            context.reportIssue(range, getMessage(rightExpression))
        }
    }
}
