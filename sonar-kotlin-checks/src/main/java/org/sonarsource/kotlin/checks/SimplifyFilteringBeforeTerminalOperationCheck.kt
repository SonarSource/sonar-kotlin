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
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.CallAbstractCheck
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

private const val KOTLIN_COLLECTIONS_QUALIFIER = "kotlin.collections"
private val FILTER_MATCHER = FunMatcher(qualifier = KOTLIN_COLLECTIONS_QUALIFIER, name = "filter") { withArguments("kotlin.Function1") }

@Rule(key = "S6527")
class SimplifyFilteringBeforeTerminalOperationCheck : CallAbstractCheck() {
    override val functionsToVisit = setOf(
        FunMatcher(qualifier = KOTLIN_COLLECTIONS_QUALIFIER, name = "any") { withNoArguments() },
        FunMatcher(qualifier = KOTLIN_COLLECTIONS_QUALIFIER, name = "none") { withNoArguments() },
        FunMatcher(qualifier = KOTLIN_COLLECTIONS_QUALIFIER, name = "count") { withNoArguments() },
        FunMatcher(qualifier = KOTLIN_COLLECTIONS_QUALIFIER, name = "last") { withNoArguments() },
        FunMatcher(qualifier = KOTLIN_COLLECTIONS_QUALIFIER, name = "lastOrNull") { withNoArguments() },
        FunMatcher(qualifier = KOTLIN_COLLECTIONS_QUALIFIER, name = "first") { withNoArguments() },
        FunMatcher(qualifier = KOTLIN_COLLECTIONS_QUALIFIER, name = "firstOrNull") { withNoArguments() },
        FunMatcher(qualifier = KOTLIN_COLLECTIONS_QUALIFIER, name = "single") { withNoArguments() },
        FunMatcher(qualifier = KOTLIN_COLLECTIONS_QUALIFIER, name = "singleOrNull") { withNoArguments() }
    )

    override fun visitFunctionCall(callExpression: KtCallExpression, resolvedCall: ResolvedCall<*>, kotlinFileContext: KotlinFileContext) {
        val chainedCallBeforeTerminalOperationCall = callExpression.parent
            .let { it as? KtDotQualifiedExpression }
            ?.receiverExpression
            .let { it as? KtDotQualifiedExpression }
            ?.selectorExpression
            .let { it as? KtCallExpression }

        if (chainedCallBeforeTerminalOperationCall != null
            && FILTER_MATCHER.matches(chainedCallBeforeTerminalOperationCall, kotlinFileContext.bindingContext)
        ) {
            val filterCallText = chainedCallBeforeTerminalOperationCall.text
            val terminalOperationCallText = callExpression.text
            val filterPredicateText = chainedCallBeforeTerminalOperationCall.valueArguments[0].text
            val terminalOperationWithPredicate = "${callExpression.calleeExpression!!.text} $filterPredicateText"

            val message = "Remove \"$filterCallText\" and replace \"$terminalOperationCallText\" with \"$terminalOperationWithPredicate\"."

            kotlinFileContext.reportIssue(chainedCallBeforeTerminalOperationCall, message)
        }
    }
}
