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

import org.jetbrains.kotlin.backend.common.descriptors.isSuspend
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.CallAbstractCheck
import org.sonarsource.kotlin.api.checks.FUNS_ACCEPTING_DISPATCHERS
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

@org.sonarsource.kotlin.api.frontend.K1only
@Rule(key = "S6311")
class SuspendingFunCallerDispatcherCheck : CallAbstractCheck() {
    override val functionsToVisit = FUNS_ACCEPTING_DISPATCHERS

    override fun visitFunctionCall(
        callExpression: KtCallExpression,
        resolvedCall: ResolvedCall<*>,
        kotlinFileContext: KotlinFileContext,
    ) {
        val bindingContext = kotlinFileContext.bindingContext

        val arguments = resolvedCall.valueArgumentsByIndex
        if (arguments.isNullOrEmpty()
            || arguments[0] == null
            || arguments[0] !is ExpressionValueArgument
        ) return

        /* Last Call Expression is always the call itself, so we drop it */
        val callExpressions = callExpression.collectDescendantsOfType<KtCallExpression>().dropLast(1)
        if (callExpressions.isNotEmpty()
            && callExpressions.all { it.getResolvedCall(bindingContext)?.resultingDescriptor?.isSuspend == true }
        ) {
            val argExpr = (arguments[0] as ExpressionValueArgument).valueArgument?.asElement() ?: return
            kotlinFileContext.reportIssue(argExpr, "Remove this dispatcher. It is pointless when used with only suspending functions.")
        }
    }
}
