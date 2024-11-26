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

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.util.getCall
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.CallAbstractCheck
import org.sonarsource.kotlin.api.checks.ConstructorMatcher
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.checks.INT_TYPE
import org.sonarsource.kotlin.api.checks.isBytesInitializedFromString
import org.sonarsource.kotlin.api.checks.predictRuntimeIntValue
import org.sonarsource.kotlin.api.reporting.SecondaryLocation
import org.sonarsource.kotlin.api.checks.predictRuntimeValueExpression
import org.sonarsource.kotlin.api.reporting.KotlinTextRanges.textRange
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

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
        resolvedCall: ResolvedCall<*>,
        kotlinFileContext: KotlinFileContext,
    ) {
        val bindingContext = kotlinFileContext.bindingContext

        // Call expression already matched three arguments
        val firstArgument = callExpression.valueArguments[0].getArgumentExpression()!!
        val thirdArgument = callExpression.valueArguments[2].getArgumentExpression()!!
        val calleeExpression = callExpression.calleeExpression!!

        val secondaries = mutableListOf<PsiElement>()
        secondaries.add(thirdArgument)
        val byteExpression = thirdArgument.getGCMExpression(bindingContext, secondaries)
            ?.getByteExpression(bindingContext, secondaries) ?: return

        if (firstArgument.predictRuntimeIntValue(bindingContext) == 1 && byteExpression.isBytesInitializedFromString(bindingContext)) {
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

private fun KtExpression.getByteExpression(bindingContext: BindingContext, secondaries: MutableList<PsiElement>) =
    with(predictRuntimeValueExpression(bindingContext, secondaries)) {
        getCall(bindingContext)?.let {
            if (GET_BYTES_MATCHER.matches(it, bindingContext)) this
            else null
        }
    }

private fun KtExpression.getGCMExpression(bindingContext: BindingContext, secondaries: MutableList<PsiElement>) =
    predictRuntimeValueExpression(bindingContext)
        .getCall(bindingContext)?.let {
            if (GCM_PARAMETER_SPEC_MATCHER.matches(it, bindingContext)) {
                secondaries.add(it.valueArguments[1].asElement())
                it.valueArguments[1].getArgumentExpression()
            } else null
        }
