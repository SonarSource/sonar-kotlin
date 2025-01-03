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
package org.sonarsource.kotlin.checks

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.reporting.SecondaryLocation
import org.sonarsource.kotlin.api.reporting.KotlinTextRanges.textRange
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

@Rule(key = "S1764")
class IdenticalBinaryOperandCheck : AbstractCheck() {

    companion object {
        val OPERATORS = setOf(
            KtTokens.EQEQ,
            KtTokens.EXCLEQ,
            KtTokens.LT,
            KtTokens.GT,
            KtTokens.LTEQ,
            KtTokens.GTEQ,
            KtTokens.OROR,
            KtTokens.ANDAND,
            KtTokens.MINUS,
            KtTokens.DIV,
            KtTokens.EQEQEQ,
            KtTokens.EXCLEQEQEQ,
        )
    }

    override fun visitBinaryExpression(expression: KtBinaryExpression, context: KotlinFileContext) {
        val token = expression.operationReference.operationSignTokenType
        val rightOperand = expression.right!!
        val leftOperand = expression.left!!
        if (OPERATORS.contains(token) && SyntacticEquivalence.areEquivalent(leftOperand, rightOperand)) {
            context.reportIssue(
                rightOperand,
                "Correct one of the identical sub-expressions on both sides this operator.",
                listOf(SecondaryLocation(context.textRange(leftOperand), "")),
            )
        }
    }
}
