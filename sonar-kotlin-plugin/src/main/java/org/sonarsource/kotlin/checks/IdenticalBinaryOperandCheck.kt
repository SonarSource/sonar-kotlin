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
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.converter.KotlinTextRanges
import org.sonarsource.kotlin.plugin.KotlinFileContext
import org.sonarsource.slang.checks.api.SecondaryLocation

/**
 * Replacement for [org.sonarsource.slang.checks.IdenticalBinaryOperandCheck]
 */
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
            val psiDocument = context.ktFile.viewProvider.document!!
            context.reportIssue(
                rightOperand,
                "Correct one of the identical sub-expressions on both sides this operator",
                listOf(SecondaryLocation(KotlinTextRanges.textRange(psiDocument, leftOperand), "")),
            )
        }
    }
}
