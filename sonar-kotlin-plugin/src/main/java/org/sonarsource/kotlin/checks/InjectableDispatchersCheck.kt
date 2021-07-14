package org.sonarsource.kotlin.checks

import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.ASYNC
import org.sonarsource.kotlin.api.CallAbstractCheck
import org.sonarsource.kotlin.api.FunMatcher
import org.sonarsource.kotlin.api.KOTLINX_COROUTINES_PACKAGE
import org.sonarsource.kotlin.api.LAUNCH
import org.sonarsource.kotlin.api.WITH_CONTEXT
import org.sonarsource.kotlin.api.predictRuntimeValueExpression
import org.sonarsource.kotlin.api.resolveReferenceTarget
import org.sonarsource.kotlin.api.scope
import org.sonarsource.kotlin.api.secondaryOf
import org.sonarsource.kotlin.plugin.KotlinFileContext

private const val MESSAGE = "Avoid hardcoded dispatchers"
private const val DISPATCHERS_OBJECT = "$KOTLINX_COROUTINES_PACKAGE.Dispatchers"
private const val COROUTINES_PACKAGE = KOTLINX_COROUTINES_PACKAGE

private val FUNS_ACCEPTING_DISPATCHERS = listOf(
    FunMatcher(qualifier = COROUTINES_PACKAGE, name = WITH_CONTEXT),
    FunMatcher(qualifier = COROUTINES_PACKAGE, name = ASYNC),
    FunMatcher(qualifier = COROUTINES_PACKAGE, name = LAUNCH),
)

@Rule(key = "S6310")
class InjectableDispatchersCheck : CallAbstractCheck() {
    override fun functionsToVisit() = FUNS_ACCEPTING_DISPATCHERS

    override fun visitFunctionCall(callExpression: KtCallExpression, resolvedCall: ResolvedCall<*>, kotlinFileContext: KotlinFileContext) {
        val bindingContext = kotlinFileContext.bindingContext

        val arguments = resolvedCall.valueArgumentsByIndex
        if (arguments == null || arguments.isEmpty()) return

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
