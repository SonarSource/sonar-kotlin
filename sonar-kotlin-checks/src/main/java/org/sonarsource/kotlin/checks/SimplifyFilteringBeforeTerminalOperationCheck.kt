/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
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

import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.util.getCall
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.CallAbstractCheck
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.reporting.message

private const val KOTLIN_COLLECTIONS_QUALIFIER = "kotlin.collections"
private val FILTER_MATCHER = FunMatcher(qualifier = KOTLIN_COLLECTIONS_QUALIFIER, name = "filter") { withArguments("kotlin.Function1") }

@org.sonarsource.kotlin.api.frontend.K1only
@Rule(key = "S6527")
class SimplifyFilteringBeforeTerminalOperationCheck : CallAbstractCheck() {
    override val functionsToVisit = listOf(
        FunMatcher(qualifier = KOTLIN_COLLECTIONS_QUALIFIER) {
            withNames("any", "none", "count", "last", "lastOrNull", "first", "firstOrNull", "single", "singleOrNull")
            withNoArguments()
        }
    )

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

                val message = message {
                    +"Remove "
                    code(filterCallText)
                    +" and replace "
                    code(terminalOpCallText)
                    +" with "
                    code(terminalOpWithPredicate)
                    +"."
                }

                kotlinFileContext.reportIssue(filterCallBeforeTerminalOp.callElement, message)
            }
    }
}
