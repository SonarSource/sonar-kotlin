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

import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.ArgumentMatcher
import org.sonarsource.kotlin.api.checks.CallAbstractCheck
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.checks.FunMatcherImpl
import org.sonarsource.kotlin.api.checks.determineType
import org.sonarsource.kotlin.api.checks.isSupertypeOf
import org.sonarsource.kotlin.api.checks.predictReceiverExpression
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

const val qualifier = "kotlin.collections"

val COLLECTION_EXTENSIONS_MATCHER = FunMatcher(
    qualifier = qualifier,
    isExtensionFunction = true
) {
    withNames(
        "remove", "contains", "indexOf", "lastIndexOf", "containsKey", "get"
    )
    withArguments(ArgumentMatcher.ANY)
}

val COLLECTION_ARGUMENT_EXTENSIONS_MATCHER = FunMatcher(
    qualifier = qualifier,
    isExtensionFunction = true
) {
    withNames("removeAll", "retainAll", "containsAll")
    withArguments("kotlin.collections.Collection")
}

val CONTAINS_VALUE_MATCHER = FunMatcher(
    qualifier = qualifier,
    isExtensionFunction = true
) {
    withNames("containsValue")
    withArguments(ArgumentMatcher.ANY)
}

val funMatcherToArgumentIndexMap = mapOf(
    COLLECTION_EXTENSIONS_MATCHER to 0,
    COLLECTION_ARGUMENT_EXTENSIONS_MATCHER to 0,
    CONTAINS_VALUE_MATCHER to 1
)

const val ISSUE_MESSAGE = "This key/object cannot ever be present in the collection"

@Rule(key = "S2175")
class CollectionInappropriateCallsCheck : CallAbstractCheck() {

    override val functionsToVisit =
        listOf(COLLECTION_EXTENSIONS_MATCHER, COLLECTION_ARGUMENT_EXTENSIONS_MATCHER, CONTAINS_VALUE_MATCHER)

    override fun visitFunctionCall(
        callExpression: KtCallExpression,
        resolvedCall: ResolvedCall<*>,
        matchedFun: FunMatcherImpl,
        kotlinFileContext: KotlinFileContext,
    ) {
        val ctx = kotlinFileContext.bindingContext

        // all evaluated methods have one and one only argument
        val arg = callExpression.valueArguments.first()
        var argType = arg.determineType(ctx) ?: return

        val collectionType = callExpression.predictReceiverExpression(ctx).determineType(ctx) ?: return
        // If collection type arguments aren't present, this rule shouldn't be triggered
        if (collectionType.arguments.isEmpty()) return

        val collectionArgumentIndex = funMatcherToArgumentIndexMap[matchedFun]!!
        val collectionArgumentType = collectionType.arguments[collectionArgumentIndex].type.makeNotNullable()

        // for methods like removeAll, containsAll etc.. we pass a collection as argument,
        // and so we want to check the type of the collection<argument> instead
        if (matchedFun == COLLECTION_ARGUMENT_EXTENSIONS_MATCHER) {
            argType = argType.arguments.first().type
        }

        argType = argType.makeNotNullable()

        // We avoid raising FPs for unresolved generic types.
        if (argType.isTypeParameter() || collectionArgumentType.isTypeParameter()) return

        if (argType != collectionArgumentType
            && !collectionArgumentType.isSupertypeOf(argType)
            && !argType.isSupertypeOf(collectionArgumentType)
        ) {
            kotlinFileContext.reportIssue(arg, ISSUE_MESSAGE)
        }

    }

}
