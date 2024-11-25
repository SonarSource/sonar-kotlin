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
package org.sonarsource.kotlin.api.checks

import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

abstract class CallAbstractCheck : AbstractCheck() {
    abstract val functionsToVisit: Iterable<FunMatcherImpl>

    open fun visitFunctionCall(
        callExpression: KtCallExpression,
        resolvedCall: ResolvedCall<*>,
        matchedFun: FunMatcherImpl,
        kotlinFileContext: KotlinFileContext
    ) = visitFunctionCall(callExpression, resolvedCall, kotlinFileContext)

    open fun visitFunctionCall(callExpression: KtCallExpression, resolvedCall: ResolvedCall<*>, kotlinFileContext: KotlinFileContext) = Unit

    final override fun visitCallExpression(callExpression: KtCallExpression, kotlinFileContext: KotlinFileContext) {
        val resolvedCall = callExpression.getResolvedCall(kotlinFileContext.bindingContext) ?: return
        functionsToVisit.firstOrNull { resolvedCall matches it }
            ?.let { visitFunctionCall(callExpression, resolvedCall, it, kotlinFileContext) }
    }
}
