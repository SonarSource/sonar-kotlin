/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2022 SonarSource SA
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
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
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
        if (!leftIntValue.isZeroOrNegative() && !rightIntValue.isZeroOrNegative()) return

        val isLeftZero = leftIntValue.isZeroOrNegative()

        val testedExpr = if (isLeftZero) bet.right else bet.left
        val integerValue = if (isLeftZero) leftIntValue else rightIntValue
        val opWithEq = if (isLeftZero) KtTokens.LTEQ else KtTokens.GTEQ
        val opWithoutEq = if (isLeftZero) KtTokens.GT else KtTokens.LT

        if (testedExpr is KtDotQualifiedExpression && MATCHERS.any { it.matches(testedExpr.getResolvedCall(ctx)) }) {
            if (opToken == opWithEq) {
                fileCtx.reportIssue(bet, ISSUE_MESSAGE_SIZE_ALWAYS_GTEQ)
            } else if (opToken == opWithoutEq || (integerValue.isNegative() && opToken == KtTokens.EQEQ)) {
                fileCtx.reportIssue(bet, ISSUE_MESSAGE_SIZE_NEVER_LT)
            }
        }

    }

    fun Int?.isZeroOrNegative(): Boolean {
        return this != null && this <= 0
    }

    fun Int?.isNegative(): Boolean {
        return this != null && this < 0
    }

}
