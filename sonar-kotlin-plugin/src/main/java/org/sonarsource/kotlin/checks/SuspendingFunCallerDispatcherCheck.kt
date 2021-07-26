package org.sonarsource.kotlin.checks

import org.jetbrains.kotlin.backend.common.descriptors.isSuspend
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.CallAbstractCheck
import org.sonarsource.kotlin.api.FUNS_ACCEPTING_DISPATCHERS
import org.sonarsource.kotlin.plugin.KotlinFileContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall as getResolvedCallNative

@Rule(key = "S6311")
class SuspendingFunCallerDispatcherCheck : CallAbstractCheck() {
    override fun functionsToVisit() = FUNS_ACCEPTING_DISPATCHERS

    override fun visitFunctionCall(
        callExpression: KtCallExpression,
        resolvedCall: ResolvedCall<*>,
        kotlinFileContext: KotlinFileContext,
    ) {
        val bindingContext = kotlinFileContext.bindingContext

        val arguments = resolvedCall.valueArgumentsByIndex
        if (arguments == null
            || arguments.isEmpty()
            || arguments[0] == null
            || arguments[0] !is ExpressionValueArgument
        ) return

        /* Last Call Expression is always the call itself, so we drop it */
        val callExpressions = callExpression.collectDescendantsOfType<KtCallExpression>().dropLast(1)
        if (callExpressions.isNotEmpty()
            && callExpressions.all { it.getResolvedCallNative(bindingContext)?.resultingDescriptor?.isSuspend == true }
        ) {
            val argExpr = (arguments[0] as ExpressionValueArgument).valueArgument?.asElement() ?: return
            kotlinFileContext.reportIssue(argExpr, "Remove this dispatcher which is used for suspending functions only")
        }
    }
}
