package org.sonarsource.kotlin.checks

import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.typeUtil.supertypes
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.api.SecondaryLocation
import org.sonarsource.kotlin.api.suspendModifier
import org.sonarsource.kotlin.converter.KotlinTextRanges.textRange
import org.sonarsource.kotlin.plugin.KotlinFileContext

private const val COROUTINE_SCOPE = "kotlinx.coroutines.CoroutineScope"
private const val MESSAGE = "Extension functions on CoroutineScope should not be suspending"

@Rule(key = "S6312")
class CoroutineScopeFunSuspendingCheck : AbstractCheck() {
    override fun visitNamedFunction(function: KtNamedFunction, kotlinFileContext: KotlinFileContext) {
        // Only applicable for suspending extension functions
        val suspendModifier = function.suspendModifier() ?: return
        val receiverType = function.receiverTypeReference ?: return
        val resolvedReceiverType = kotlinFileContext.bindingContext.get(BindingContext.TYPE, receiverType) ?: return

        if (
            (resolvedReceiverType.getJetTypeFqName(false) == COROUTINE_SCOPE ||
                resolvedReceiverType.supertypes().any { it.getJetTypeFqName(false) == COROUTINE_SCOPE })
        ) {
            val secondaries = listOf(SecondaryLocation(kotlinFileContext.textRange(receiverType)))
            kotlinFileContext.reportIssue(suspendModifier, MESSAGE, secondaries)
        }
    }
}
