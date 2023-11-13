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

import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.utils.IDEAPluginsCompatibilityAPI
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.CallAbstractCheck
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

private const val KOTLIN_COLLECTIONS_QUALIFIER = "kotlin.collections"
private val FILTER_MATCHER = FunMatcher(qualifier = KOTLIN_COLLECTIONS_QUALIFIER, name = "filter") { withArguments("kotlin.Function1") }

@Rule(key = "S6527")
class SimplifyFilteringBeforeTerminalOperationCheck : CallAbstractCheck() {
    override val functionsToVisit = listOf(
        FunMatcher(qualifier = KOTLIN_COLLECTIONS_QUALIFIER) {
            withNames("any", "none", "count", "last", "lastOrNull", "first", "firstOrNull", "single", "singleOrNull")
            withNoArguments()
        }
    )

    @OptIn(IDEAPluginsCompatibilityAPI::class)
    override fun visitFunctionCall(callExpression: KtCallExpression, resolvedCall: ResolvedCall<*>, kotlinFileContext: KotlinFileContext) {
        callExpression.parent
            .let { it as? KtDotQualifiedExpression }
            ?.receiverExpression
            ?.getCall(kotlinFileContext.bindingContext)
            ?.takeIf { callBeforeTerminalOp -> FILTER_MATCHER.matches(callBeforeTerminalOp, kotlinFileContext.bindingContext) }
            ?.let { filterCallBeforeTerminalOp ->
                val filterCallText = filterCallBeforeTerminalOp.callElement.text
                val filterPredicateText = filterCallBeforeTerminalOp.valueArguments[0].asElement().text
                val terminalOpCallText = callExpression.text
                val terminalOpWithPredicate = "${callExpression.calleeExpression!!.text} $filterPredicateText"

                val message = "Remove \"$filterCallText\" and replace \"$terminalOpCallText\" with \"$terminalOpWithPredicate\"."

                kotlinFileContext.reportIssue(filterCallBeforeTerminalOp.callElement, message)
            }
    }
}
