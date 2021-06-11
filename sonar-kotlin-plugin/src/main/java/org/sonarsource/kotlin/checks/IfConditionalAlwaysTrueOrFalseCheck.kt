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

import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtUnaryExpression
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.plugin.KotlinFileContext

/**
 * Replacement for [org.sonarsource.slang.checks.IfConditionalAlwaysTrueOrFalseCheck]
 */
@Rule(key = "S1145")
class IfConditionalAlwaysTrueOrFalseCheck : AbstractCheck() {

    override fun visitIfExpression(expression: KtIfExpression, kotlinFileContext: KotlinFileContext) {
        if (isAlwaysTrueOrFalse(expression.condition!!)) {
            /** TODO try to use [org.jetbrains.kotlin.resolve.CompileTimeConstantUtils.canBeReducedToBooleanConstant] */
            kotlinFileContext.reportIssue(
                expression.condition!!,
                "Remove this useless \"if\" statement.")
        }
    }

    private fun isAlwaysTrueOrFalse(condition: KtExpression) =
        condition.isTrueValueLiteral() || condition.isFalseValueLiteral()
            || condition.isSimpleExpressionWithLiteral(KtTokens.ANDAND) { it.isFalseValueLiteral() }
            || condition.isSimpleExpressionWithLiteral(KtTokens.OROR) { it.isTrueValueLiteral() }

    /** Replacement for [org.sonarsource.slang.checks.utils.ExpressionUtils.isTrueValueLiteral] */
    private fun KtExpression.isTrueValueLiteral(): Boolean {
        val e = skipParentheses()
        return (e is KtConstantExpression && e.text == "true")
            || (e.isNegation() && (e as KtUnaryExpression).baseExpression!!.isFalseValueLiteral())
    }

    /** Replacement for [org.sonarsource.slang.checks.utils.ExpressionUtils.isFalseValueLiteral] */
    private fun KtExpression.isFalseValueLiteral(): Boolean {
        val e = skipParentheses()
        return (e is KtConstantExpression && e.text == "false")
            || (e.isNegation() && (e as KtUnaryExpression).baseExpression!!.isTrueValueLiteral())
    }

    /** Replacement for [org.sonarsource.slang.checks.utils.ExpressionUtils.isNegation] */
    private fun KtExpression.isNegation() =
        this is KtUnaryExpression && this.operationToken == KtTokens.EXCL

    /** Replacement for [org.sonarsource.slang.checks.IfConditionalAlwaysTrueOrFalseCheck.isSimpleExpressionWithLiteral] */
    private fun KtExpression.isSimpleExpressionWithLiteral(
        operation: KtSingleValueToken,
        hasLiteralValue: (KtExpression) -> Boolean,
    ) = isSimpleExpression(operation) && anyDescendantOfType(hasLiteralValue)

    private fun KtExpression.isSimpleExpression(operation: KtSingleValueToken): Boolean =
        when (val e = skipParentheses()) {
            is KtNameReferenceExpression -> true
            is KtConstantExpression -> true
            is KtBinaryExpression -> e.operationToken == operation &&
                e.left!!.isSimpleExpression(operation) && e.right!!.isSimpleExpression(operation)
            else -> false
        }
}
