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

import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.api.FunMatcher
import org.sonarsource.kotlin.api.predictRuntimeIntValue
import org.sonarsource.kotlin.plugin.KotlinFileContext

val COLLECTION_SIZE_METHOD = FunMatcher(qualifier = "kotlin.collections.Collection", name = "size") {
    withNoArguments()
}
val ARRAY_SIZE_METHOD = FunMatcher(qualifier = "kotlin.Array", name = "size") {
    withNoArguments()
}

val MATCHERS = listOf(COLLECTION_SIZE_METHOD, ARRAY_SIZE_METHOD)

val ISSUE_MESSAGE_SIZE_NEVER_LT = """The size of an array/collection is never "<0", update this test to use ".isEmpty()"."""
val ISSUE_MESSAGE_SIZE_ALWAYS_GTEQ =
    """The size of an array/collection is always ">=0", update this test to either ".isNotEmpty()" or ".isEmpty()"."""

@Rule(key = "S3981")
class CollectionSizeAndArrayLengthCheck : AbstractCheck() {

    override fun visitBinaryExpression(bet: KtBinaryExpression, fileCtx: KotlinFileContext) {
        val ctx = fileCtx.bindingContext
        val opToken = bet.operationToken

        val leftIntValue = bet.left?.predictRuntimeIntValue(ctx)
        val rightIntValue = bet.right?.predictRuntimeIntValue(ctx)

        val msg = if (leftIntValue.isZeroOrNegative()) {
            checkConditionsAndSelectMessage(ctx, opToken, bet.right, leftIntValue, KtTokens.LTEQ, KtTokens.GT)
        } else if (rightIntValue.isZeroOrNegative()) {
            checkConditionsAndSelectMessage(ctx, opToken, bet.left, rightIntValue, KtTokens.GTEQ, KtTokens.LT)
        } else {
            null
        }

        msg?.let { fileCtx.reportIssue(bet, it) }

    }

    private fun checkConditionsAndSelectMessage(
        ctx: BindingContext,
        opToken: IElementType,
        testedExpr: KtExpression?,
        integerValue: Int?,
        opWithEq: KtSingleValueToken,
        opWithoutEq: KtSingleValueToken,
    ): String? {
        if (testedExpr is KtDotQualifiedExpression && MATCHERS.any { it.matches(testedExpr.getResolvedCall(ctx)) }) {
            if (opToken == opWithEq) {
                return ISSUE_MESSAGE_SIZE_ALWAYS_GTEQ
            } else if (opToken == opWithoutEq || (integerValue.isNegative() && opToken == KtTokens.EQEQ)) {
                return ISSUE_MESSAGE_SIZE_NEVER_LT
            }
        }
        return null
    }

    fun Int?.isZeroOrNegative(): Boolean {
        return this != null && this <= 0
    }

    fun Int?.isNegative(): Boolean {
        return this != null && this < 0
    }

}
