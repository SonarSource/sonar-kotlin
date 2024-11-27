/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2024 SonarSource SA
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

import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.analysis.api.resolution.singleFunctionCallOrNull
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi2ir.deparenthesize
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.util.getCall
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.CallAbstractCheck
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.reporting.message
import org.sonarsource.kotlin.api.visiting.analyze

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

    override fun visitFunctionCall(callExpression: KtCallExpression, resolvedCall: KaFunctionCall<*>, kotlinFileContext: KotlinFileContext): Unit = analyze {
        val x = callExpression.parent
            .let { it as? KtDotQualifiedExpression }
            ?.receiverExpression
            // TODO note that resolveToCall doesn't deparenthesize unlike getCall
            ?.deparenthesize()
        x?.resolveToCall()?.singleFunctionCallOrNull()
            ?.takeIf { callBeforeTerminalOp -> FILTER_MATCHER.matches(callBeforeTerminalOp) }
            ?.let { filterCallBeforeTerminalOp ->
                // TODO pay attention on next line
                val callElement = if (x is KtDotQualifiedExpression) x.selectorExpression!! else x
                val filterCallText = callElement.text
                val filterPredicateText = filterCallBeforeTerminalOp.argumentMapping.keys.first().text
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

                kotlinFileContext.reportIssue(callElement, message)
            }
    }
}
