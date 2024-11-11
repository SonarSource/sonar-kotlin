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
@file:OptIn(KaExperimentalApi::class)

package org.sonarsource.kotlin.checks

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.analysis.api.resolution.successfulFunctionCallOrNull
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.CallAbstractCheck
import org.sonarsource.kotlin.api.checks.ConstructorMatcher
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.checks.FunMatcherImpl
import org.sonarsource.kotlin.api.checks.GET_INSTANCE
import org.sonarsource.kotlin.api.checks.INT_TYPE
import org.sonarsource.kotlin.api.checks.STRING_TYPE
import org.sonarsource.kotlin.api.checks.predictReceiverExpression
import org.sonarsource.kotlin.api.checks.predictRuntimeStringValue
import org.sonarsource.kotlin.api.checks.predictRuntimeValueExpression

import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.analyze

private val CIPHER_INIT_MATCHER = FunMatcher(qualifier = "javax.crypto.Cipher", name = "init") {
    withArguments(INT_TYPE, "java.security.Key", "java.security.spec.AlgorithmParameterSpec")
}

private val GET_INSTANCE_MATCHER = FunMatcher(qualifier = "javax.crypto.Cipher", name = GET_INSTANCE) {
    withArguments(STRING_TYPE)
}

private val GET_BYTES_MATCHER = FunMatcher(qualifier = "kotlin.text", name = "toByteArray")
private val IV_PARAMETER_SPEC_MATCHER = ConstructorMatcher("javax.crypto.spec.IvParameterSpec")

@Rule(key = "S3329")
class CipherBlockChainingCheck : CallAbstractCheck() {
    override val functionsToVisit = listOf(CIPHER_INIT_MATCHER)

    override fun visitFunctionCall(
        callExpression: KtCallExpression,
        resolvedCall: KaFunctionCall<*>,
        matchedFun: FunMatcherImpl,
        kotlinFileContext: KotlinFileContext
    ) {
        val calleeExpression = callExpression.calleeExpression ?: return
        val receiverExpression = callExpression.predictReceiverExpression() ?: return
        val thirdArgument = callExpression.valueArguments[2].getArgumentExpression() ?: return

        if (receiverExpression.isCBC() && thirdArgument.isInitializedWithToByteArray()) {
            kotlinFileContext.reportIssue(calleeExpression, "Use a dynamically-generated, random IV.")
        }
    }
}

private fun KtExpression.isInitializedWithToByteArray() = analyze {
    firstArgumentOfInitializer(IV_PARAMETER_SPEC_MATCHER)
        ?.predictRuntimeValueExpression()
        ?.resolveToCall()
        ?.successfulFunctionCallOrNull()
        ?.let { expr ->
            GET_BYTES_MATCHER.matches(expr)
        } ?: false
}

private fun KtExpression.isCBC() =
    firstArgumentOfInitializer(GET_INSTANCE_MATCHER)
        ?.predictRuntimeStringValue()
        ?.contains("CBC", ignoreCase = true)
        ?: false

private fun KtExpression.firstArgumentOfInitializer(matcher: FunMatcherImpl) = analyze {
    predictRuntimeValueExpression()
        .resolveToCall()
        ?.successfulFunctionCallOrNull()?.let {
            if (matcher.matches(it)) {
                it.argumentMapping.keys.toList()[0]
            } else null
        }
}
