/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import org.jetbrains.kotlin.analysis.api.resolution.KaCallableMemberCall
import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.analysis.api.resolution.successfulCallOrNull
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPsiUtil.deparenthesize
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.CallAbstractCheck
import org.sonarsource.kotlin.api.checks.ConstructorMatcher
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.checks.getFirstArgumentExpression
import org.sonarsource.kotlin.api.checks.predictRuntimeIntValue
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.withKaSession

private const val MESSAGE = """Increase the "corePoolSize"."""

private val THREAD_POOL_CONSTRUCTOR_MATCHER =
    ConstructorMatcher("java.util.concurrent.ScheduledThreadPoolExecutor") { withArguments("kotlin.Int") }

private val POOL_SIZE_SETTER_MATCHER =
    FunMatcher(definingSupertype = "java.util.concurrent.ThreadPoolExecutor", name = "setCorePoolSize") { withArguments("kotlin.Int") }

@Rule(key = "S2122")
class ScheduledThreadPoolExecutorZeroCheck : CallAbstractCheck() {

    override val functionsToVisit = listOf(THREAD_POOL_CONSTRUCTOR_MATCHER, POOL_SIZE_SETTER_MATCHER)

    override fun visitFunctionCall(callExpression: KtCallExpression, resolvedCall: KaFunctionCall<*>, kotlinFileContext: KotlinFileContext) = withKaSession {
        val corePoolSizeExpression = resolvedCall.getFirstArgumentExpression() ?: return
        if (isZero(deparenthesize(corePoolSizeExpression))) {
            kotlinFileContext.reportIssue(corePoolSizeExpression, MESSAGE)
        }
    }

    override fun visitBinaryExpression(expression: KtBinaryExpression, ctx: KotlinFileContext) = withKaSession {
        if (expression.operationToken == KtTokens.EQ &&
            isZero(deparenthesize(expression.right)) &&
            POOL_SIZE_SETTER_MATCHER.matches(
                expression.resolveToCall()?.successfulCallOrNull<KaCallableMemberCall<*, *>>()
            )
        ) {
            ctx.reportIssue(expression.right!!, MESSAGE)
        }
    }

    private fun isZero(expression: KtExpression?): Boolean =
        expression?.predictRuntimeIntValue() == 0

}
