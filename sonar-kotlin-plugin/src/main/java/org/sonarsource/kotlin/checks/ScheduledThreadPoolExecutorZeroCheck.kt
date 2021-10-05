/*
 * SonarSource Kotlin
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

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtPsiUtil.deparenthesize
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi2ir.unwrappedSetMethod
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.CallAbstractCheck
import org.sonarsource.kotlin.api.ConstructorMatcher
import org.sonarsource.kotlin.api.FunMatcher
import org.sonarsource.kotlin.plugin.KotlinFileContext

private const val MESSAGE = "Increase the \"corePoolSize\"."

private val THREAD_POOL_CONSTRUCTOR_MATCHER =
    ConstructorMatcher("java.util.concurrent.ScheduledThreadPoolExecutor") { withArguments("kotlin.Int") }

private val POOL_SIZE_SETTER_MATCHER =
    FunMatcher(qualifier = "java.util.concurrent.ThreadPoolExecutor", name = "setCorePoolSize") { withArguments("kotlin.Int") }

@Rule(key = "S2122")
class ScheduledThreadPoolExecutorZeroCheck : CallAbstractCheck() {

    override val functionsToVisit = listOf(THREAD_POOL_CONSTRUCTOR_MATCHER, POOL_SIZE_SETTER_MATCHER)

    override fun visitFunctionCall(callExpression: KtCallExpression, resolvedCall: ResolvedCall<*>, ctx: KotlinFileContext) {
        val corePoolSizeExpression = callExpression.valueArgumentList!!.arguments[0].getArgumentExpression()
        if (isZero(deparenthesize(corePoolSizeExpression))) {
            ctx.reportIssue(corePoolSizeExpression!!, MESSAGE)
        }
    }

    override fun visitBinaryExpression(expression: KtBinaryExpression, ctx: KotlinFileContext) {
        if (expression.operationToken == KtTokens.EQ &&
            isZero(deparenthesize(expression.right)) &&
            isCorePoolSizeSetter(ctx, deparenthesize(expression.left))
        ) {
            ctx.reportIssue(expression.right!!, MESSAGE)
        }
    }

    private fun isZero(expression: KtExpression?): Boolean =
        (expression is KtConstantExpression) && (expression.node.text == "0")

    private fun isCorePoolSizeSetter(ctx: KotlinFileContext, expression: KtExpression?): Boolean = when (expression) {
        is KtNameReferenceExpression -> (expression.getReferencedName() == "corePoolSize") &&
            (POOL_SIZE_SETTER_MATCHER.matches(
                (ctx.bindingContext.get(BindingContext.REFERENCE_TARGET, expression) as? PropertyDescriptor)?.unwrappedSetMethod))
        is KtQualifiedExpression -> isCorePoolSizeSetter(ctx, deparenthesize(expression.selectorExpression))
        else -> false
    }

}
