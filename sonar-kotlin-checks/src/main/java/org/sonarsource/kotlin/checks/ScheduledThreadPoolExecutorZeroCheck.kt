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
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPsiUtil.deparenthesize
import org.jetbrains.kotlin.resolve.calls.util.getFirstArgumentExpression
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.CallAbstractCheck
import org.sonarsource.kotlin.api.checks.ConstructorMatcher
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.checks.predictRuntimeIntValue
import org.sonarsource.kotlin.api.checks.setterMatches
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

private const val MESSAGE = """Increase the "corePoolSize"."""

private val THREAD_POOL_CONSTRUCTOR_MATCHER =
    ConstructorMatcher("java.util.concurrent.ScheduledThreadPoolExecutor") { withArguments("kotlin.Int") }

private val POOL_SIZE_SETTER_MATCHER =
    FunMatcher(definingSupertype = "java.util.concurrent.ThreadPoolExecutor", name = "setCorePoolSize") { withArguments("kotlin.Int") }

@org.sonarsource.kotlin.api.frontend.K1only
@Rule(key = "S2122")
class ScheduledThreadPoolExecutorZeroCheck : CallAbstractCheck() {

    override val functionsToVisit = listOf(THREAD_POOL_CONSTRUCTOR_MATCHER, POOL_SIZE_SETTER_MATCHER)

    override fun visitFunctionCall(callExpression: KtCallExpression, resolvedCall: ResolvedCall<*>, kotlinFileContext: KotlinFileContext) {
        val corePoolSizeExpression = resolvedCall.getFirstArgumentExpression() ?: return
        if (isZero(kotlinFileContext, deparenthesize(corePoolSizeExpression))) {
            kotlinFileContext.reportIssue(corePoolSizeExpression, MESSAGE)
        }
    }

    override fun visitBinaryExpression(expression: KtBinaryExpression, ctx: KotlinFileContext) {
        if (expression.operationToken == KtTokens.EQ &&
            isZero(ctx, deparenthesize(expression.right)) &&
            deparenthesize(expression.left).setterMatches(ctx.bindingContext, "corePoolSize", POOL_SIZE_SETTER_MATCHER)
        ) {
            ctx.reportIssue(expression.right!!, MESSAGE)
        }
    }

    private fun isZero(ctx: KotlinFileContext, expression: KtExpression?): Boolean =
        expression?.predictRuntimeIntValue(ctx.bindingContext) == 0

}
