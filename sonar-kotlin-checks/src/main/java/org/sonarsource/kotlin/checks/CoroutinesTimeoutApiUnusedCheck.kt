/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.analysis.api.resolution.successfulFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.CallAbstractCheck
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.checks.FunMatcherImpl
import org.sonarsource.kotlin.api.checks.KOTLINX_COROUTINES_PACKAGE
import org.sonarsource.kotlin.api.checks.matches
import org.sonarsource.kotlin.api.reporting.SecondaryLocation
import org.sonarsource.kotlin.api.reporting.KotlinTextRanges.textRange
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.withKaSession

private const val MESSAGE = """Use "withTimeoutOrNull { }" instead of manual delayed cancellation."""

private val DELAY_MATCHER = FunMatcher(name = "delay", qualifier = KOTLINX_COROUTINES_PACKAGE)
private val LAUNCH_ASYNC_MATCHER = FunMatcher(qualifier = KOTLINX_COROUTINES_PACKAGE) {
    withNames("launch", "async")
}

@Rule(key = "S6316")
class CoroutinesTimeoutApiUnusedCheck : CallAbstractCheck() {
    override val functionsToVisit = listOf(FunMatcher(definingSupertype = "$KOTLINX_COROUTINES_PACKAGE.Job", name = "cancel"))

    override fun visitFunctionCall(
        callExpression: KtCallExpression,
        resolvedCall: KaFunctionCall<*>,
        matchedFun: FunMatcherImpl,
        kotlinFileContext: KotlinFileContext,
    ) = withKaSession {
        val cancelCallCalleeExpression = callExpression.calleeExpression ?: return

        val kaSymbol = (callExpression.context as? KtDotQualifiedExpression)
            ?.receiverExpression?.mainReference?.resolveToSymbol()
            ?: return

        val siblingIter = callExpression.parent.siblings(forward = false, withItself = false)
            .filter { it is KtElement }
            .iterator()

        if (!siblingIter.hasNext()) return

        // For now we only consider extremely simple cases where the job creation is directly followed by a delay(...) and subsequent
        // cancel() methods. If there is anything done in between we don't report anything, as it is non-trivial to check that this
        // rule remains valid.
        val delayCall = asDelayCallIfMatching(siblingIter.next()) ?: return
        val initializerCall = asInitializerCallIfMatching(siblingIter.next(), kaSymbol) ?: return

        kotlinFileContext.reportIssue(
            cancelCallCalleeExpression, MESSAGE, listOf(
                SecondaryLocation(kotlinFileContext.textRange(delayCall)),
                SecondaryLocation(kotlinFileContext.textRange(initializerCall))
            )
        )
    }

    private fun asDelayCallIfMatching(element: PsiElement): KtExpression? = withKaSession {
        if (element is KtCallExpression &&
            element.resolveToCall()?.successfulFunctionCallOrNull() matches DELAY_MATCHER) {
            element.calleeExpression
        } else null
    }

    private fun asInitializerCallIfMatching(
        element: PsiElement,
        targetInitializer: KaSymbol?,
    ): KtExpression? = withKaSession {
        if (element is KtProperty && element.symbol == targetInitializer) {
            val initializer = element.initializer as? KtCallExpression ?: return null
            if (initializer.resolveToCall()?.successfulFunctionCallOrNull() matches LAUNCH_ASYNC_MATCHER) {
                return initializer.calleeExpression
            }
        }
        return null
    }
}
