package org.sonarsource.kotlin.checks

import org.jetbrains.kotlin.psi.KtNamedFunction
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.api.COROUTINES_CHANNEL
import org.sonarsource.kotlin.api.COROUTINES_FLOW
import org.sonarsource.kotlin.api.SecondaryLocation
import org.sonarsource.kotlin.api.returnTypeAsString
import org.sonarsource.kotlin.api.suspendModifier
import org.sonarsource.kotlin.converter.KotlinTextRanges.textRange
import org.sonarsource.kotlin.plugin.KotlinFileContext

private val FORBIDDEN_RETURN_TYPES = listOf(COROUTINES_FLOW, COROUTINES_CHANNEL)
private const val MESSAGE = """Functions returning "Flow" or "Channel" should not be suspending"""

@Rule(key = "S6309")
class FlowChannelReturningFunsNotSuspendingCheck : AbstractCheck() {
    override fun visitNamedFunction(function: KtNamedFunction, kotlinFileContext: KotlinFileContext) {
        val suspendModifier = function.suspendModifier()
        if (suspendModifier != null && function.returnTypeAsString(kotlinFileContext.bindingContext) in FORBIDDEN_RETURN_TYPES) {
            val secondaries = function.typeReference
                ?.let { listOf(SecondaryLocation(kotlinFileContext.textRange(it))) }
                ?: emptyList()
            kotlinFileContext.reportIssue(suspendModifier, MESSAGE, secondaries)
        }
    }
}
