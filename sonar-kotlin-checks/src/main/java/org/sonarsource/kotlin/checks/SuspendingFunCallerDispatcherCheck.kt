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

import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.analysis.api.resolution.singleFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.CallAbstractCheck
import org.sonarsource.kotlin.api.checks.FUNS_ACCEPTING_DISPATCHERS
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.withKaSession

@Rule(key = "S6311")
class SuspendingFunCallerDispatcherCheck : CallAbstractCheck() {
    override val functionsToVisit = FUNS_ACCEPTING_DISPATCHERS

    override fun visitFunctionCall(
        callExpression: KtCallExpression,
        resolvedCall: KaFunctionCall<*>,
        kotlinFileContext: KotlinFileContext,
    ) = withKaSession {
        val arguments = resolvedCall.argumentMapping.keys
        /* Last Call Expression is always the call itself, so we drop it */
        val callExpressions = callExpression.collectDescendantsOfType<KtCallExpression>().dropLast(1)
        if (callExpressions.isNotEmpty()
            && callExpressions.all {
                (it.resolveToCall()?.singleFunctionCallOrNull()?.partiallyAppliedSymbol?.signature?.symbol as? KaNamedFunctionSymbol)?.isSuspend == true
            }
            && arguments.size == 2
        ) {
            val argExpr = arguments.elementAt(0)
            kotlinFileContext.reportIssue(argExpr, "Remove this dispatcher. It is pointless when used with only suspending functions.")
        }
    }
}
