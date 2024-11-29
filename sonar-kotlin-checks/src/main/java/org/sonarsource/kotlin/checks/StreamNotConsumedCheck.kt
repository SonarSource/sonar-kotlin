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
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsStatement
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.CallAbstractCheck
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.checks.FunMatcherImpl
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

private const val STREAM_MESSAGE = "Refactor the code so this stream pipeline is used."

private const val SEQUENCE_MESSAGE = "Refactor the code so this sequence pipeline is used."

private val COMMON_NON_TERM = arrayOf("distinct", "dropWhile", "filter", "flatMap", "map", "sorted", "takeWhile")

private val COMMON_STREAM_NON_TERM = arrayOf("limit", "parallel", "peek", "sequential", "skip", "unordered").plus(COMMON_NON_TERM)

private val COMMON_PRIMITIVE_STREAM_NON_TERM = arrayOf("boxed", "mapToObj").plus(COMMON_STREAM_NON_TERM)

private val SEQUENCE_MATCHER = FunMatcher(qualifier = "kotlin.sequences") {
    withNames(*COMMON_NON_TERM)
    withNames(
        "chunked", "distinctBy", "drop", "filterIndexed", "filterIsInstance", "filterNot", "filterNotNull", "flatMapIndexed",
        "mapIndexed", "mapIndexedNotNull", "mapNotNull", "plus", "plusElement", "sortedBy", "sortedByDescending",
        "sortedDescending", "sortedWith", "take", "zip", "zipWithNext"
    )
}

@org.sonarsource.kotlin.api.frontend.K1only
@Rule(key = "S3958")
class StreamNotConsumedCheck : CallAbstractCheck() {

    override val functionsToVisit = listOf(
        SEQUENCE_MATCHER,
        FunMatcher(definingSupertype = "kotlin.collections.List") {
            withNames("parallelStream", "stream")
        },
        FunMatcher(qualifier = "java.util.stream.Stream") {
            withNames(*COMMON_STREAM_NON_TERM)
            withNames("flatMapToDouble", "flatMapToInt", "flatMapToLong", "mapToDouble", "mapToInt", "mapToLong")
        },
        FunMatcher(qualifier = "java.util.stream.IntStream") {
            withNames(*COMMON_PRIMITIVE_STREAM_NON_TERM)
            withNames("asDoubleStream", "asLongStream", "mapToDouble", "mapToLong")
        },
        FunMatcher(qualifier = "java.util.stream.LongStream") {
            withNames(*COMMON_PRIMITIVE_STREAM_NON_TERM)
            withNames("asDoubleStream", "mapToDouble", "mapToInt")
        },
        FunMatcher(qualifier = "java.util.stream.DoubleStream") {
            withNames(*COMMON_PRIMITIVE_STREAM_NON_TERM)
            withNames("mapToInt", "mapToLong")
        },
    )

    override fun visitFunctionCall(
        callExpression: KtCallExpression,
        resolvedCall: ResolvedCall<*>,
        matchedFun: FunMatcherImpl,
        kotlinFileContext: KotlinFileContext,
    ) {
        if (callExpression.isUsedAsStatement(kotlinFileContext.bindingContext)) {
            val message = if (matchedFun == SEQUENCE_MATCHER) SEQUENCE_MESSAGE else STREAM_MESSAGE;
            kotlinFileContext.reportIssue(callExpression.calleeExpression!!, message)
        }
    }
}
