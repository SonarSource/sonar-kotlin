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
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.util.getCall
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.ArgumentMatcher
import org.sonarsource.kotlin.api.checks.CallAbstractCheck
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.checks.FunMatcherImpl
import org.sonarsource.kotlin.api.checks.INT_TYPE
import org.sonarsource.kotlin.api.checks.predictReceiverExpression
import org.sonarsource.kotlin.api.checks.predictRuntimeIntValue
import org.sonarsource.kotlin.api.checks.predictRuntimeStringValue
import org.sonarsource.kotlin.api.checks.predictRuntimeValueExpression
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

private val PREPARE_STATEMENT = FunMatcher(qualifier = "java.sql.Connection", name = "prepareStatement")

private val PREPARE_STATEMENT_SET = FunMatcher(qualifier = "java.sql.PreparedStatement", nameRegex = "^set.*+".toRegex()) {
    withArguments(ArgumentMatcher(INT_TYPE), ArgumentMatcher.ANY)
}

private val RESULT_SET_GET = FunMatcher(qualifier = "java.sql.ResultSet", nameRegex = "^get.*+".toRegex()) {
    withArguments(ArgumentMatcher(INT_TYPE))
    withArguments(ArgumentMatcher(INT_TYPE), ArgumentMatcher.ANY)
}

@org.sonarsource.kotlin.api.frontend.K1only
@Rule(key = "S2695")
class PreparedStatementAndResultSetCheck : CallAbstractCheck() {

    override val functionsToVisit = listOf(PREPARE_STATEMENT_SET, RESULT_SET_GET)

    override fun visitFunctionCall(
        callExpression: KtCallExpression,
        resolvedCall: ResolvedCall<*>,
        matchedFun: FunMatcherImpl,
        kotlinFileContext: KotlinFileContext,
    ) {
        val bindingContext = kotlinFileContext.bindingContext

        val firstArgument = callExpression.valueArguments[0].getArgumentExpression()!!
        val receiver = callExpression.predictReceiverExpression(bindingContext) ?: return
        val firstArgumentValue = firstArgument.predictRuntimeIntValue(bindingContext) ?: return

        when (matchedFun) {
            RESULT_SET_GET ->
                if (firstArgumentValue == 0) kotlinFileContext.reportIssue(firstArgument, "ResultSet indices start at 1.")

            PREPARE_STATEMENT_SET -> {
                if (firstArgumentValue == 0) {
                    kotlinFileContext.reportIssue(firstArgument, "PreparedStatement indices start at 1.")
                } else {
                    getNumberOfParameters(bindingContext, receiver)?.let {
                        if (firstArgumentValue > it) {
                            kotlinFileContext.reportIssue(
                                firstArgument,
                                """This "PreparedStatement" ${if (it == 0) "has no" else "only has $it"} parameters."""
                            )
                        }
                    }
                }
            }
        }

    }
}

private fun getNumberOfParameters(bindingContext: BindingContext, receiver: KtExpression) =
    receiver.predictRuntimeValueExpression(bindingContext)
        .getCall(bindingContext)?.let {
            if (PREPARE_STATEMENT.matches(it, bindingContext)) {
                it.valueArguments[0].getArgumentExpression()
                    ?.predictRuntimeStringValue(bindingContext)
                    ?.count { c -> c.toString() == "?" }
            } else null
        }
