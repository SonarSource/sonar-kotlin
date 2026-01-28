/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2026 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1.0.1, as published by SonarSource Sàrl.
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

import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.analysis.api.resolution.successfulFunctionCallOrNull
import org.jetbrains.kotlin.psi.KtCallExpression
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.withKaSession

abstract class CallAbstractCheck : AbstractCheck() {
    abstract val functionsToVisit: Iterable<FunMatcherImpl>

    open fun visitFunctionCall(
        callExpression: KtCallExpression,
        resolvedCall: KaFunctionCall<*>,
        matchedFun: FunMatcherImpl,
        kotlinFileContext: KotlinFileContext
    ) = visitFunctionCall(callExpression, resolvedCall, kotlinFileContext)

    open fun visitFunctionCall(callExpression: KtCallExpression, resolvedCall: KaFunctionCall<*>, kotlinFileContext: KotlinFileContext) = Unit

    final override fun visitCallExpression(callExpression: KtCallExpression, kotlinFileContext: KotlinFileContext) {
        withKaSession {
            val resolvedCall = callExpression.resolveToCall()
                // Using successfulFunctionCallOrNull instead of singleFunctionCallOrNull to avoid incorrect resolution
                // of functions when semantics are missing. E.g. extension methods may not be found without semantics,
                // leading to a function definition to be selected that has the same name but different signature.
                ?.successfulFunctionCallOrNull() ?: return
            functionsToVisit.firstOrNull { it.matches(resolvedCall) }
                ?.let { visitFunctionCall(callExpression, resolvedCall, it, kotlinFileContext) }
        }
    }
}
