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

import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.CallAbstractCheck
import org.sonarsource.kotlin.api.FunMatcher
import org.sonarsource.kotlin.api.FunMatcherImpl
import org.sonarsource.kotlin.api.determineType
import org.sonarsource.kotlin.api.predictReceiverExpression
import org.sonarsource.kotlin.plugin.KotlinFileContext

private val OBJECT_ARRAY_MATCHER = FunMatcher(qualifier = "kotlin.Array") {
    withNames("hashCode", "toString")
    withNoArguments()
}

private val ARRAY_CONTENT_MATCHER = FunMatcher(qualifier = "kotlin.collections") {
    withNames("contentHashCode", "contentToString")
    withNoArguments()
}

private val PRIMITIVE_ARRAY_MATCHERS = listOf(
    "BooleanArray", "ByteArray", "ShortArray", "CharArray", "IntArray", "LongArray", "FloatArray", "DoubleArray"
).map { className ->
    FunMatcher(qualifier = "kotlin.${className}") {
        withNames("hashCode", "toString")
        withNoArguments()
    }
}

private val ARRAY_QUALIFIERS: Set<String> = setOf(OBJECT_ARRAY_MATCHER.qualifier!!) + PRIMITIVE_ARRAY_MATCHERS.map { it.qualifier!! }

private val PRIMITIVE_ARRAY_REPLACEMENT = mapOf("hashCode" to "contentHashCode", "toString" to "contentToString")
private val OBJECT_ARRAY_REPLACEMENT = mapOf("hashCode" to "contentDeepHashCode", "toString" to "contentDeepToString")
private val ARRAY_OF_ARRAY_REPLACEMENT = mapOf("contentHashCode" to "contentDeepHashCode", "contentToString" to "contentDeepToString")

@Rule(key = "S2116")
class ArrayHashCodeAndToStringCheck : CallAbstractCheck() {

    override val functionsToVisit = listOf(OBJECT_ARRAY_MATCHER, ARRAY_CONTENT_MATCHER) + PRIMITIVE_ARRAY_MATCHERS

    override fun visitFunctionCall(
        callExpression: KtCallExpression,
        resolvedCall: ResolvedCall<*>,
        matchedFun: FunMatcherImpl,
        kotlinFileContext: KotlinFileContext,
    ) {
        val methodName = resolvedCall.resultingDescriptor.name.asString()
        val replacement = when (matchedFun) {
            OBJECT_ARRAY_MATCHER -> OBJECT_ARRAY_REPLACEMENT[methodName]
            ARRAY_CONTENT_MATCHER -> if (receiverIsArrayOfArray(callExpression, kotlinFileContext))
                ARRAY_OF_ARRAY_REPLACEMENT[methodName] else null

            else -> PRIMITIVE_ARRAY_REPLACEMENT[methodName]
        }
        if (replacement != null) {
            kotlinFileContext.reportIssue(callExpression, "Use \"${replacement}()\" instead.")
        }
    }

    private fun receiverIsArrayOfArray(callExpression: KtCallExpression, kotlinFileContext: KotlinFileContext): Boolean {
        val bindingContext = kotlinFileContext.bindingContext
        return callExpression.predictReceiverExpression(bindingContext)?.determineType(bindingContext)?.arguments
                ?.any { ARRAY_QUALIFIERS.contains(it.type.getJetTypeFqName(false)) }
            ?: false
    }

}
