package org.sonarsource.kotlin.checks

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.getReferenceTargets
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.CallAbstractCheck
import org.sonarsource.kotlin.api.FunMatcher
import org.sonarsource.kotlin.api.KOTLINX_COROUTINES_PACKAGE
import org.sonarsource.kotlin.api.SecondaryLocation
import org.sonarsource.kotlin.api.matches
import org.sonarsource.kotlin.converter.KotlinTextRanges.textRange
import org.sonarsource.kotlin.plugin.KotlinFileContext

private const val MESSAGE = """Use "withTimeoutOrNull { }" instead of manual delayed cancellation"""

private val DELAY_MATCHER = FunMatcher(name = "delay", qualifier = KOTLINX_COROUTINES_PACKAGE)
private val LAUNCH_ASYNC_MATCHER = FunMatcher(qualifier = KOTLINX_COROUTINES_PACKAGE) {
    withNames("launch", "async")
}

@Rule(key = "S6316")
class CoroutinesTimeoutApiUnusedCheck : CallAbstractCheck() {
    override fun functionsToVisit() = listOf(FunMatcher(supertype = "$KOTLINX_COROUTINES_PACKAGE.Job", name = "cancel"))

    override fun visitFunctionCall(
        callExpression: KtCallExpression,
        resolvedCall: ResolvedCall<*>,
        kotlinFileContext: KotlinFileContext
    ) {
        val bindingContext = kotlinFileContext.bindingContext

        val cancelCallCalleeExpression = callExpression.calleeExpression ?: return

        val jobDeclaration = (callExpression.context as? KtDotQualifiedExpression)
            ?.receiverExpression?.getReferenceTargets(bindingContext)?.toList()?.getOrNull(0) ?: return

        val siblingIter = callExpression.parent.siblings(forward = false, withItself = false)
            .filter { it is KtElement }
            .iterator()

        // For now we only consider extremely simple cases where the job creation is directly followed by a delay(...) and subsequent
        // cancel() methods. If there is anything done in between we don't report anything, as it is non-trivial to check that this
        // rule remains valid.
        val delayCall = asDelayCallIfMatching(siblingIter.next(), bindingContext) ?: return
        val initializerCall = asInitializerCallIfMatching(siblingIter.next(), jobDeclaration, bindingContext) ?: return

        kotlinFileContext.reportIssue(
            cancelCallCalleeExpression, MESSAGE, listOf(
                SecondaryLocation(kotlinFileContext.textRange(delayCall)),
                SecondaryLocation(kotlinFileContext.textRange(initializerCall))
            )
        )
    }

    private fun asDelayCallIfMatching(element: PsiElement, bindingContext: BindingContext) =
        if (element is KtCallExpression && element.getResolvedCall(bindingContext) matches DELAY_MATCHER) {
            element.calleeExpression
        } else null

    private fun asInitializerCallIfMatching(
        element: PsiElement,
        targetInitializer: DeclarationDescriptor,
        bindingContext: BindingContext,
    ): KtExpression? {
        if (element is KtProperty && bindingContext.get(BindingContext.VARIABLE, element) === targetInitializer) {
            val initializer = element.initializer as? KtCallExpression ?: return null
            if (initializer.getResolvedCall(bindingContext) matches LAUNCH_ASYNC_MATCHER) {
                return initializer.calleeExpression
            }
        }
        return null
    }
}
