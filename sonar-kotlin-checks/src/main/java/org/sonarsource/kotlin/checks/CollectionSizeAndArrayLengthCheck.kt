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

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.analysis.api.resolution.singleVariableAccessCall
import org.jetbrains.kotlin.analysis.api.resolution.successfulFunctionCallOrNull
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.checks.predictRuntimeIntValue
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.withKaSession

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
        val opToken = bet.operationToken

        val leftIntValue = bet.left?.predictRuntimeIntValue()
        val rightIntValue = bet.right?.predictRuntimeIntValue()

        val msg = if (leftIntValue.isZeroOrNegative()) {
            checkConditionsAndSelectMessage(opToken, bet.right, leftIntValue, KtTokens.LTEQ, KtTokens.GT)
        } else if (rightIntValue.isZeroOrNegative()) {
            checkConditionsAndSelectMessage(opToken, bet.left, rightIntValue, KtTokens.GTEQ, KtTokens.LT)
        } else {
            null
        }

        msg?.let { fileCtx.reportIssue(bet, it) }

    }

    private fun checkConditionsAndSelectMessage(
        opToken: IElementType,
        testedExpr: KtExpression?,
        integerValue: Int?,
        opWithEq: KtSingleValueToken,
        opWithoutEq: KtSingleValueToken,
    ): String? {
        if (testedExpr is KtDotQualifiedExpression &&
            withKaSession {
                val functionCall = testedExpr.resolveToCall()?.singleVariableAccessCall() ?: return null
                MATCHERS.any { it.matches(functionCall) }
            }) {
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
