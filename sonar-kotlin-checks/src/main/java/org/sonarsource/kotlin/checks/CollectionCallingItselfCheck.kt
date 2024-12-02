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
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.*
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

private val MUTABLE_COLLECTION_FUN_MATCHER = FunMatcher(definingSupertype = "kotlin.collections.MutableCollection") {
    withNames("containsAll", "addAll", "removeAll", "retainAll", "add")
}

private val COLLECTIONS_FUN_MATCHER = FunMatcher(definingSupertype = "kotlin.collections") {
    withNames("containsAll", "addAll", "removeAll", "retainAll", "fill")
}

/*
* In K1 symbol.allOverriddenSymbols returns [kotlin.collections.List.containsAll, kotlin.collections.Collection.containsAll]
* In K2 it only returns [kotlin.collections.List.containsAll]
* This sounds like a regression as according to documentation it is supposed to return all
* Requires further investigation */
private val LIST_CONTAINS_ALL_FUN_MATCHER = FunMatcher(definingSupertype = "kotlin.collections.List") {
    withNames("containsAll")
}

private const val MESSAGE = "Collections should not be passed as arguments to their own methods."

@org.sonarsource.kotlin.api.frontend.K1only("incorrect result")
@Rule(key = "S2114")
class CollectionCallingItselfCheck : CallAbstractCheck() {
    override val functionsToVisit = listOf(MUTABLE_COLLECTION_FUN_MATCHER, COLLECTIONS_FUN_MATCHER, LIST_CONTAINS_ALL_FUN_MATCHER)

    override fun visitFunctionCall(
        callExpression: KtCallExpression,
        resolvedCall: KaFunctionCall<*>,
        matchedFun: FunMatcherImpl,
        kotlinFileContext: KotlinFileContext
    ) {
        val receiver = callExpression.predictReceiverExpression()?.predictRuntimeValueExpression() ?: return
        val argument = callExpression.valueArguments[0].getArgumentExpression()!!

        val argumentValue = argument.predictRuntimeValueExpression()

        if (receiver === argumentValue) {
            kotlinFileContext.reportIssue(argument, MESSAGE)
        }
    }
}
