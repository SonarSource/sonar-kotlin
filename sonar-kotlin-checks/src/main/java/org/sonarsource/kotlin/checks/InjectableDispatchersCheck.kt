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

import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.CallAbstractCheck
import org.sonarsource.kotlin.api.checks.FUNS_ACCEPTING_DISPATCHERS
import org.sonarsource.kotlin.api.checks.KOTLINX_COROUTINES_PACKAGE
import org.sonarsource.kotlin.api.checks.predictRuntimeValueExpression
import org.sonarsource.kotlin.api.checks.resolveReferenceTarget
import org.sonarsource.kotlin.api.checks.scope
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.frontend.secondaryOf

private const val MESSAGE = "Avoid hardcoded dispatchers."
private const val DISPATCHERS_OBJECT = "$KOTLINX_COROUTINES_PACKAGE.Dispatchers"

@Rule(key = "S6310")
class InjectableDispatchersCheck : CallAbstractCheck() {
    override val functionsToVisit = FUNS_ACCEPTING_DISPATCHERS

    override fun visitFunctionCall(callExpression: KtCallExpression, resolvedCall: ResolvedCall<*>, kotlinFileContext: KotlinFileContext) {
        val bindingContext = kotlinFileContext.bindingContext

        val arguments = resolvedCall.valueArgumentsByIndex
        if (arguments.isNullOrEmpty()) return

        val argExpr = (arguments[0] as? ExpressionValueArgument)?.valueArgument?.getArgumentExpression() ?: return
        val argValueExpr = argExpr.predictRuntimeValueExpression(bindingContext) as? KtQualifiedExpression ?: return
        val argReference = argValueExpr.resolveReferenceTarget(bindingContext) ?: return

        if (argReference.scope() == DISPATCHERS_OBJECT) {
            val secondaries = if (argExpr !== argValueExpr) {
                listOf(kotlinFileContext.secondaryOf(argValueExpr, "Hard-coded dispatcher"))
            } else emptyList()

            kotlinFileContext.reportIssue(argExpr, MESSAGE, secondaries)
        }
    }
}
