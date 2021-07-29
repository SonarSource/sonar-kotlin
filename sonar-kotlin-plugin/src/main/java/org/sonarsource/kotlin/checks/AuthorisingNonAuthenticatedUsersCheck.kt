package org.sonarsource.kotlin.checks

import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.CallAbstractCheck
import org.sonarsource.kotlin.api.ConstructorMatcher
import org.sonarsource.kotlin.api.FunMatcher
import org.sonarsource.kotlin.api.SecondaryLocation
import org.sonarsource.kotlin.api.predictReceiverExpression
import org.sonarsource.kotlin.converter.KotlinTextRanges.textRange
import org.sonarsource.kotlin.plugin.KotlinFileContext

private const val BUILDER = "android.security.keystore.KeyGenParameterSpec.Builder"

private val KEY_GEN_BUILDER_MATCHER = ConstructorMatcher(typeName = BUILDER)
private val KEY_GEN_BUILDER_BUILD_MATCHER = FunMatcher(qualifier = BUILDER, name = "build")

private val KEY_GEN_BUILDER_SET_AUTH_MATCHER = FunMatcher(qualifier = BUILDER, name = "setUserAuthenticationRequired") {
    withArguments("kotlin.Boolean")
}

@Rule(key = "S6288")
class AuthorisingNonAuthenticatedUsersCheck : CallAbstractCheck() {
    override fun functionsToVisit() = listOf(KEY_GEN_BUILDER_BUILD_MATCHER)

    override fun visitFunctionCall(
        callExpression: KtCallExpression,
        resolvedCall: ResolvedCall<*>,
        kotlinFileContext: KotlinFileContext,
    ) {
        val bindingContext = kotlinFileContext.bindingContext
        val call = callExpression.getCall(bindingContext) ?: return
        var receiver = call
        val secondaryLocations = mutableListOf<SecondaryLocation>()

        while (!KEY_GEN_BUILDER_MATCHER.matches(receiver, bindingContext)) {
            val callElement = receiver.callElement as? KtCallExpression ?: return

            if (KEY_GEN_BUILDER_SET_AUTH_MATCHER.matches(receiver, bindingContext)) {
                val argumentExpression = receiver.valueArguments[0]?.getArgumentExpression()!!
                argumentExpression.getType(bindingContext)?.let {
                    val argValue = bindingContext.get(BindingContext.COMPILE_TIME_VALUE, argumentExpression)
                        ?.getValue(it) as? Boolean
                    if (argValue != false) return
                    secondaryLocations.add(SecondaryLocation(kotlinFileContext.textRange(callElement.calleeExpression!!)))
                }
            }

            receiver = callElement.predictReceiverExpression(bindingContext)
                ?.getCall(bindingContext) ?: return
        }
        kotlinFileContext.reportIssue(receiver.calleeExpression!!,
            "Make sure authorizing non-authenticated users to use this key is safe here.",
            secondaryLocations)
    }

}
