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

import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.psi.KtCallExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.CallAbstractCheck
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.checks.predictReceiverExpression
import org.sonarsource.kotlin.api.checks.predictRuntimeValueExpression
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

private val MUTABLE_COLLECTION_FUN_MATCHER = FunMatcher(definingSupertype = "kotlin.collections.MutableCollection") {
    withNames("addAll", "removeAll", "retainAll", "add")
}

private val COLLECTION_FUN_MATCHER = FunMatcher(definingSupertype = "kotlin.collections.Collection") {
    withNames("containsAll")
}

private val COLLECTIONS_FUN_MATCHER = FunMatcher(definingSupertype = "kotlin.collections") {
    withNames("containsAll", "addAll", "removeAll", "retainAll", "fill")
}

private const val MESSAGE = "Collections should not be passed as arguments to their own methods."

@Rule(key = "S2114")
class CollectionCallingItselfCheck : CallAbstractCheck() {
    override val functionsToVisit = listOf(MUTABLE_COLLECTION_FUN_MATCHER, COLLECTION_FUN_MATCHER, COLLECTIONS_FUN_MATCHER)

    override fun visitFunctionCall(
        callExpression: KtCallExpression,
        resolvedCall: KaFunctionCall<*>,
        kotlinFileContext: KotlinFileContext,
    ) {
        val receiver = callExpression.predictReceiverExpression()?.predictRuntimeValueExpression() ?: return
        val argument = callExpression.valueArguments[0].getArgumentExpression()!!

        val argumentValue = argument.predictRuntimeValueExpression()

        if (receiver === argumentValue) {
            kotlinFileContext.reportIssue(argument, MESSAGE)
        }
    }
}
