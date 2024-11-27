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
import org.jetbrains.kotlin.analysis.api.resolution.successfulFunctionCallOrNull
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
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
import org.sonarsource.kotlin.api.visiting.analyze

private val PREPARE_STATEMENT = FunMatcher(qualifier = "java.sql.Connection", name = "prepareStatement")

private val PREPARE_STATEMENT_SET = FunMatcher(qualifier = "java.sql.PreparedStatement", nameRegex = "^set.*+".toRegex()) {
    withArguments(ArgumentMatcher(INT_TYPE), ArgumentMatcher.ANY)
}

private val RESULT_SET_GET = FunMatcher(qualifier = "java.sql.ResultSet", nameRegex = "^get.*+".toRegex()) {
    withArguments(ArgumentMatcher(INT_TYPE))
    withArguments(ArgumentMatcher(INT_TYPE), ArgumentMatcher.ANY)
}

@Rule(key = "S2695")
class PreparedStatementAndResultSetCheck : CallAbstractCheck() {

    override val functionsToVisit = listOf(PREPARE_STATEMENT_SET, RESULT_SET_GET)

    override fun visitFunctionCall(
        callExpression: KtCallExpression,
        resolvedCall: KaFunctionCall<*>,
        matchedFun: FunMatcherImpl,
        kotlinFileContext: KotlinFileContext
    ) {
        val firstArgument = callExpression.valueArguments[0].getArgumentExpression()!!
        val receiver = callExpression.predictReceiverExpression() ?: return
        val firstArgumentValue = firstArgument.predictRuntimeIntValue() ?: return

        when (matchedFun) {
            RESULT_SET_GET ->
                if (firstArgumentValue == 0) kotlinFileContext.reportIssue(firstArgument, "ResultSet indices start at 1.")

            PREPARE_STATEMENT_SET -> {
                if (firstArgumentValue == 0) {
                    kotlinFileContext.reportIssue(firstArgument, "PreparedStatement indices start at 1.")
                } else {
                    getNumberOfParameters(receiver)?.let {
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

private fun getNumberOfParameters(receiver: KtExpression) = analyze {
    receiver.predictRuntimeValueExpression()
        .resolveToCall()?.successfulFunctionCallOrNull()
        ?.let {
            if (PREPARE_STATEMENT.matches(it)) {
                it.argumentMapping.keys.toList()[0]
                    .predictRuntimeStringValue()
                    ?.count { c -> c.toString() == "?" }
            } else null
        }
}
