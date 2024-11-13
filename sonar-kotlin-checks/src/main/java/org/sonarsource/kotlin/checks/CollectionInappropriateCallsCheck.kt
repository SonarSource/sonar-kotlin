/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2024 SonarSource SA
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

import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaTypeNullability
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType
import org.jetbrains.kotlin.psi.KtCallExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.ArgumentMatcher
import org.sonarsource.kotlin.api.checks.CallAbstractCheck
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.checks.FunMatcherImpl
import org.sonarsource.kotlin.api.checks.predictReceiverExpression
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.analyze

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
        resolvedCall: KaFunctionCall<*>,
        matchedFun: FunMatcherImpl,
        kotlinFileContext: KotlinFileContext
    ) {
        analyze {
            // all evaluated methods have one and one only argument
            val arg = callExpression.valueArguments.first()
            var argType = arg.getArgumentExpression()?.expressionType ?: return

            val collectionType = callExpression.predictReceiverExpression()?.expressionType as? KaClassType ?: return
            val collectionArgumentIndex = funMatcherToArgumentIndexMap[matchedFun]!!
            val collectionArgumentType = collectionType.arrayElementType?.withNullability(KaTypeNullability.NON_NULLABLE)
                ?: collectionType.typeArguments[collectionArgumentIndex]
                    .type?.withNullability(KaTypeNullability.NON_NULLABLE) ?: return


            // for methods like removeAll, containsAll etc.. we pass a collection as argument,
            // and so we want to check the type of the collection<argument> instead
            if (matchedFun == COLLECTION_ARGUMENT_EXTENSIONS_MATCHER && argType is KaClassType) {
                argType = argType.typeArguments.first().type ?: return
            }
            argType = argType.withNullability(KaTypeNullability.NON_NULLABLE)

            // We avoid raising FPs for unresolved generic types.
            if (argType is KaTypeParameterType || collectionArgumentType is KaTypeParameterType) return

            if (
                !argType.semanticallyEquals(collectionArgumentType) &&
                !argType.allSupertypes.any { it.semanticallyEquals(collectionArgumentType) } &&
                !collectionArgumentType.allSupertypes.any { it.semanticallyEquals(argType) }
            ) {
                kotlinFileContext.reportIssue(arg, ISSUE_MESSAGE)
            }
        }

    }

}
