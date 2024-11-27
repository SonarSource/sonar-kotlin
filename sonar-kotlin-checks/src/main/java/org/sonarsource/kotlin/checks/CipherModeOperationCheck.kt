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
@file:OptIn(KaExperimentalApi::class)

package org.sonarsource.kotlin.checks

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.analysis.api.resolution.successfulFunctionCallOrNull
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.*
import org.sonarsource.kotlin.api.reporting.SecondaryLocation
import org.sonarsource.kotlin.api.reporting.KotlinTextRanges.textRange
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.analyze

private val CIPHER_INIT_MATCHER = FunMatcher(qualifier = "javax.crypto.Cipher", name = "init") {
    withArguments(INT_TYPE, "java.security.Key", "java.security.spec.AlgorithmParameterSpec")
}

private val GCM_PARAMETER_SPEC_MATCHER = ConstructorMatcher("javax.crypto.spec.GCMParameterSpec") {
    withArguments(INT_TYPE, "kotlin.ByteArray")
}

private val GET_BYTES_MATCHER = FunMatcher(qualifier = "kotlin.text", name = "toByteArray")

@Rule(key = "S6432")
class CipherModeOperationCheck : CallAbstractCheck() {
    override val functionsToVisit = listOf(CIPHER_INIT_MATCHER)

    override fun visitFunctionCall(
        callExpression: KtCallExpression,
        resolvedCall: KaFunctionCall<*>,
        matchedFun: FunMatcherImpl,
        kotlinFileContext: KotlinFileContext
    ) {
        // Call expression already matched three arguments
        val firstArgument = callExpression.valueArguments[0].getArgumentExpression()!!
        val thirdArgument = callExpression.valueArguments[2].getArgumentExpression()!!
        val calleeExpression = callExpression.calleeExpression!!

        val secondaries = mutableListOf<PsiElement>()
        secondaries.add(thirdArgument)
        val byteExpression = thirdArgument.getGCMExpression(secondaries)
            ?.getByteExpression(secondaries) ?: return

        if (firstArgument.predictRuntimeIntValue() == 1 && byteExpression.isBytesInitializedFromString()) {
            kotlinFileContext.reportIssue(
                calleeExpression,
                "Use a dynamically-generated initialization vector (IV) to avoid IV-key pair reuse.",
                generateSecondaryLocations(secondaries, kotlinFileContext)
            )
        }
    }
}

private fun generateSecondaryLocations(secondaries: List<PsiElement>, kotlinFileContext: KotlinFileContext) =
    secondaries.mapIndexed { i, secondary ->
        if (i < secondaries.size - 1) {
            SecondaryLocation(kotlinFileContext.textRange(secondary), "Initialization vector is configured here.")
        } else {
            SecondaryLocation(kotlinFileContext.textRange(secondary), "The initialization vector is a static value.")
        }
    }

private fun KtExpression.getByteExpression(secondaries: MutableList<PsiElement>) =
    with(predictRuntimeValueExpression(secondaries)) {
        analyze {
            this@with.resolveToCall()
                ?.successfulFunctionCallOrNull()
                ?.let {
                    if (GET_BYTES_MATCHER.matches(it)) this@with
                    else null
                }
        }
    }

private fun KtExpression.getGCMExpression(secondaries: MutableList<PsiElement>) = analyze {
    predictRuntimeValueExpression()
        .resolveToCall()
        ?.successfulFunctionCallOrNull()
        ?.let {
            if (GCM_PARAMETER_SPEC_MATCHER.matches(it)) {
                val expression = it.argumentMapping.keys.toList()[1]
                secondaries.add(expression)
                expression
            } else null
        }
}
