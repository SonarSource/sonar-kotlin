/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
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
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.CallAbstractCheck
import org.sonarsource.kotlin.api.FunMatcher
import org.sonarsource.kotlin.api.KOTLINX_COROUTINES_PACKAGE
import org.sonarsource.kotlin.api.SecondaryLocation
import org.sonarsource.kotlin.api.matches
import org.sonarsource.kotlin.converter.KotlinTextRanges.textRange
import org.sonarsource.kotlin.plugin.KotlinFileContext

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

        if (!siblingIter.hasNext()) return

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
