package org.sonarsource.kotlin.api

import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.sonarsource.kotlin.plugin.KotlinFileContext

abstract class CallAbstractCheck : AbstractCheck() {
    abstract fun functionsToVisit(): List<FunMatcher>
    abstract fun visitFunctionCall(callExpression: KtCallExpression, resolvedCall: ResolvedCall<*>, kotlinFileContext: KotlinFileContext)

    final override fun visitCallExpression(callExpression: KtCallExpression, kotlinFileContext: KotlinFileContext) {
        val resolvedCall = callExpression.getResolvedCall(kotlinFileContext.bindingContext) ?: return
        if (functionsToVisit().any { resolvedCall matches it }) visitFunctionCall(callExpression, resolvedCall, kotlinFileContext)
    }
}
