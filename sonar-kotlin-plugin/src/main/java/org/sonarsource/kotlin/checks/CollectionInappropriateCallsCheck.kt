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
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.ArgumentMatcher
import org.sonarsource.kotlin.api.CallAbstractCheck
import org.sonarsource.kotlin.api.FunMatcher
import org.sonarsource.kotlin.api.FunMatcherImpl
import org.sonarsource.kotlin.api.determineType
import org.sonarsource.kotlin.api.isSupertypeOf
import org.sonarsource.kotlin.api.predictReceiverExpression
import org.sonarsource.kotlin.plugin.KotlinFileContext

const val qualifier = "kotlin.collections"

val COLLECTION_EXTENSIONS_MATCHER = FunMatcher(
    qualifier = qualifier,
    extensionFunction = true
) {
    withNames(
        "remove", "contains", "indexOf", "lastIndexOf", "containsKey", "get"
    )
    withArguments(ArgumentMatcher.ANY)
}

val COLLECTION_ARGUMENT_EXTENSIONS_MATCHER = FunMatcher(
    qualifier = qualifier,
    extensionFunction = true
) {
    withNames("removeAll", "retainAll", "containsAll")
    withArguments("kotlin.collections.Collection")
}

val CONTAINS_VALUE_MATCHER = FunMatcher(
    qualifier = qualifier,
    extensionFunction = true
) {
    withNames("containsValue")
    withArguments(ArgumentMatcher.ANY)
}

val funMatcherToArgumentIndexMap = mapOf(
    COLLECTION_EXTENSIONS_MATCHER to 0,
    COLLECTION_ARGUMENT_EXTENSIONS_MATCHER to 0,
    CONTAINS_VALUE_MATCHER to 1
)

val ISSUE_MESSAGE = "This key/object cannot ever be present in the collection"

@Rule(key = "S2175")
class CollectionInappropriateCallsCheck : CallAbstractCheck() {

    override val functionsToVisit =
        listOf(COLLECTION_EXTENSIONS_MATCHER, COLLECTION_ARGUMENT_EXTENSIONS_MATCHER, CONTAINS_VALUE_MATCHER)

    override fun visitFunctionCall(
        callExpression: KtCallExpression,
        resolvedCall: ResolvedCall<*>,
        funMatcherImpl: FunMatcherImpl,
        kotlinFileContext: KotlinFileContext,
    ) {
        val ctx = kotlinFileContext.bindingContext

        // all evaluated methods have one and one only argument
        val arg = callExpression.valueArguments.first()
        var argType = arg.determineType(ctx) ?: return

        val collectionType = callExpression.predictReceiverExpression(ctx).determineType(ctx) ?: return
        val collectionArgumentIndex = funMatcherToArgumentIndexMap[funMatcherImpl]!!
        val collectionArgumentType = collectionType.arguments[collectionArgumentIndex].type.makeNotNullable()

        // for methods like removeAll, containsAll etc.. we pass a collection as argument,
        // and so we want to check the type of the collection<argument> instead
        if (funMatcherImpl == COLLECTION_ARGUMENT_EXTENSIONS_MATCHER) {
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
