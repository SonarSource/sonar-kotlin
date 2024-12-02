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

import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.analysis.api.resolution.KaVariableAccessCall
import org.jetbrains.kotlin.analysis.api.resolution.singleVariableAccessCall
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.*
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.frontend.secondaryOf
import org.sonarsource.kotlin.api.visiting.withKaSession

private const val MESSAGE = "Avoid hardcoded dispatchers."
private const val DISPATCHERS_OBJECT = "$KOTLINX_COROUTINES_PACKAGE.Dispatchers"

@Rule(key = "S6310")
class InjectableDispatchersCheck : CallAbstractCheck() {
    override val functionsToVisit = FUNS_ACCEPTING_DISPATCHERS

    override fun visitFunctionCall(
        callExpression: KtCallExpression,
        resolvedCall: KaFunctionCall<*>,
        matchedFun: FunMatcherImpl,
        kotlinFileContext: KotlinFileContext
    ) {
        val arguments = resolvedCall.argumentMapping.keys.toList()
        if (arguments.isEmpty()) return

        val argExpr = arguments.first()
        val argValueExpr = argExpr.predictRuntimeValueExpression() as? KtQualifiedExpression ?: return
        val variableAccessCall: KaVariableAccessCall = withKaSession {
            argValueExpr.resolveToCall()?.singleVariableAccessCall() ?: return
        }
        val receiverFqn = (variableAccessCall.partiallyAppliedSymbol.dispatchReceiver?.type as? KaClassType)
                ?.symbol?.classId?.asFqNameString()

        if (receiverFqn == DISPATCHERS_OBJECT) {
            val secondaries = if (argExpr !== argValueExpr) {
                listOf(kotlinFileContext.secondaryOf(argValueExpr, "Hard-coded dispatcher"))
            } else emptyList()

            kotlinFileContext.reportIssue(argExpr, MESSAGE, secondaries)
        }
    }
}
