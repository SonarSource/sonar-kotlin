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

import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

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

    override fun visitCallExpression(expression: KtCallExpression, kotlinFileContext: KotlinFileContext) {
        val bindingContext = kotlinFileContext.bindingContext
        val calleeExpression = expression.calleeExpression ?: return
        
        if (MATH_RANDOM_MATCHER.matches(expression, bindingContext) || KOTLIN_RANDOM_MATCHER.matches(expression, bindingContext))
            kotlinFileContext.reportIssue(calleeExpression, MESSAGE)

        when(val resultingDescriptor = expression.getResolvedCall(bindingContext)?.resultingDescriptor) {
            is ConstructorDescriptor -> {
                resultingDescriptor.constructedClass.fqNameOrNull()?.asString()?.let { 
                    if (RANDOM_CONSTRUCTOR_TYPES.contains(it)) kotlinFileContext.reportIssue(calleeExpression, MESSAGE)
                }
            }
            else -> {
                resultingDescriptor?.fqNameOrNull()?.asString()?.substringBeforeLast(".")?.let {
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
