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

import org.jetbrains.kotlin.analysis.api.resolution.singleFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.signatures.KaCallableSignature
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.codegen.optimization.common.analyze
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.analyze

private const val MESSAGE = "Make sure that using this pseudorandom number generator is safe here."

private val MATH_RANDOM_MATCHER = FunMatcher(qualifier = "java.lang.Math", name = "random") {
    withNoArguments()
}

private val KOTLIN_RANDOM_MATCHER = FunMatcher (qualifier = "kotlin.random", name = "Random")

private val RANDOM_STATIC_TYPES = setOf(
    "java.util.concurrent.ThreadLocalRandom",
    "org.apache.commons.lang.math.RandomUtils",
    "org.apache.commons.lang3.RandomUtils",
    "org.apache.commons.lang.RandomStringUtils",
    "org.apache.commons.lang3.RandomStringUtils",
)

private val RANDOM_CONSTRUCTOR_TYPES = setOf(
    "java.util.Random",
    "org.apache.commons.lang.math.JVMRandom"
)

@Rule(key = "S2245")
class PseudoRandomCheck : AbstractCheck() {

    override fun visitCallExpression(expression: KtCallExpression, kotlinFileContext: KotlinFileContext) = analyze {
        val calleeExpression = expression.calleeExpression ?: return@analyze
        
        if (MATH_RANDOM_MATCHER.matches(expression) || KOTLIN_RANDOM_MATCHER.matches(expression))
            kotlinFileContext.reportIssue(calleeExpression, MESSAGE)

        // TODO see similar code in FunMatcher
        when (val symbol = expression.resolveToCall()?.singleFunctionCallOrNull()?.partiallyAppliedSymbol?.symbol) {
            null -> {
            }
            is KaConstructorSymbol -> {
                symbol.containingClassId?.asFqNameString()?.let {
                    if (RANDOM_CONSTRUCTOR_TYPES.contains(it)) kotlinFileContext.reportIssue(calleeExpression, MESSAGE)
                }
            }
            else -> {
                symbol.callableId?.asSingleFqName()?.parent()?.asString()?.let {
                    if (RANDOM_STATIC_TYPES.contains(it) && !expression.isChainedMethodInvocation())
                        kotlinFileContext.reportIssue(calleeExpression, MESSAGE)
                }
            }
        }
    }
}

private fun KtCallExpression.isChainedMethodInvocation() = parent?.let {
    it is KtDotQualifiedExpression && it.receiverExpression is KtDotQualifiedExpression &&
        (it.receiverExpression as KtDotQualifiedExpression).selectorExpression is KtCallExpression
} ?: false
