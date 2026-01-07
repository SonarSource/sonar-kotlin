/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2026 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1.0.1, as published by SonarSource Sàrl.
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
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.symbols.name
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.psi.KtCallExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.CallAbstractCheck
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.checks.FunMatcherImpl
import org.sonarsource.kotlin.api.checks.predictReceiverExpression
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.withKaSession

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

private val ARRAY_QUALIFIERS: Set<String> = OBJECT_ARRAY_MATCHER.qualifiers + PRIMITIVE_ARRAY_MATCHERS.flatMap { it.qualifiers }

private val PRIMITIVE_ARRAY_REPLACEMENT = mapOf("hashCode" to "contentHashCode", "toString" to "contentToString")
private val OBJECT_ARRAY_REPLACEMENT = mapOf("hashCode" to "contentDeepHashCode", "toString" to "contentDeepToString")
private val ARRAY_OF_ARRAY_REPLACEMENT = mapOf("contentHashCode" to "contentDeepHashCode", "contentToString" to "contentDeepToString")

@Rule(key = "S2116")
class ArrayHashCodeAndToStringCheck : CallAbstractCheck() {

    override val functionsToVisit = listOf(OBJECT_ARRAY_MATCHER, ARRAY_CONTENT_MATCHER) + PRIMITIVE_ARRAY_MATCHERS

    override fun visitFunctionCall(
        callExpression: KtCallExpression,
        resolvedCall: KaFunctionCall<*>,
        matchedFun: FunMatcherImpl,
        kotlinFileContext: KotlinFileContext
    ) {
        val methodName = resolvedCall.partiallyAppliedSymbol.symbol.name?.asString()
        val replacement = when (matchedFun) {
            OBJECT_ARRAY_MATCHER -> OBJECT_ARRAY_REPLACEMENT[methodName]
            ARRAY_CONTENT_MATCHER -> if (receiverIsArrayOfArray(callExpression))
                ARRAY_OF_ARRAY_REPLACEMENT[methodName] else null

            else -> PRIMITIVE_ARRAY_REPLACEMENT[methodName]
        }
        if (replacement != null) {
            kotlinFileContext.reportIssue(callExpression, "Use \"${replacement}()\" instead.")
        }
    }

    private fun receiverIsArrayOfArray(callExpression: KtCallExpression): Boolean = withKaSession {
        val argument = callExpression.predictReceiverExpression()?.expressionType?.arrayElementType
        return ARRAY_QUALIFIERS.contains(argument?.symbol?.classId?.asFqNameString())
    }

}
