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
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.CallAbstractCheck
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.checks.FunMatcherImpl
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

private const val MESSAGE = "Make sure that using this pseudorandom number generator is safe here."

private val MATH_RANDOM_MATCHER = FunMatcher(qualifier = "java.lang.Math", name = "random") {
    withNoArguments()
}

private val KOTLIN_RANDOM_MATCHER = FunMatcher (qualifier = "kotlin.random", name = "Random")

private val RANDOM_TYPES_MATCHER = FunMatcher {
   qualifiers = setOf(
       "java.util.concurrent.ThreadLocalRandom",
       "org.apache.commons.lang.math.RandomUtils",
       "org.apache.commons.lang3.RandomUtils",
       "org.apache.commons.lang.RandomStringUtils",
       "org.apache.commons.lang3.RandomStringUtils",
   )
}

private val RANDOM_CONSTRUCTORS_MATCHER = FunMatcher(matchConstructor = true) {
    qualifiers = setOf(
        "java.util.Random",
        "org.apache.commons.lang.math.JVMRandom",
    )
}

@Rule(key = "S2245")
class PseudoRandomCheck : CallAbstractCheck() {
    override val functionsToVisit = listOf(
        MATH_RANDOM_MATCHER,
        KOTLIN_RANDOM_MATCHER,
        RANDOM_TYPES_MATCHER,
        RANDOM_CONSTRUCTORS_MATCHER,
    )

    override fun visitFunctionCall(
        callExpression: KtCallExpression,
        resolvedCall: KaFunctionCall<*>,
        matchedFun: FunMatcherImpl,
        kotlinFileContext: KotlinFileContext,
    ) {
        val calleeExpression = callExpression.calleeExpression ?: return
        if (matchedFun != RANDOM_TYPES_MATCHER || !callExpression.isChainedMethodInvocation()) {
            kotlinFileContext.reportIssue(calleeExpression, MESSAGE)
        }
    }
}

private fun KtCallExpression.isChainedMethodInvocation() = parent?.let {
    it is KtDotQualifiedExpression && it.receiverExpression is KtDotQualifiedExpression &&
        (it.receiverExpression as KtDotQualifiedExpression).selectorExpression is KtCallExpression
} ?: false
