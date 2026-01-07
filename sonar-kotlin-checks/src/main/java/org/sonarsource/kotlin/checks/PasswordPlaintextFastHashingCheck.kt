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
import org.jetbrains.kotlin.analysis.api.resolution.successfulConstructorCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.successfulFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.successfulVariableAccessCall
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtParameter
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.BYTE_ARRAY_TYPE
import org.sonarsource.kotlin.api.checks.CHAR_ARRAY_TYPE
import org.sonarsource.kotlin.api.checks.CallAbstractCheck
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.checks.INT_TYPE
import org.sonarsource.kotlin.api.checks.STRING_TYPE
import org.sonarsource.kotlin.api.checks.predictReceiverExpression
import org.sonarsource.kotlin.api.checks.predictRuntimeIntValue
import org.sonarsource.kotlin.api.checks.predictRuntimeValueExpression
import org.sonarsource.kotlin.api.checks.stringValue
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.reporting.KotlinTextRanges.textRange
import org.sonarsource.kotlin.api.reporting.SecondaryLocation
import org.sonarsource.kotlin.api.visiting.withKaSession

private const val MESSAGE = """Use at least %d PBKDF2 iterations."""

private val minIterations = mapOf(
    "PBKDF2withHmacSHA1" to 1_300_000,
    "PBKDF2withHmacSHA256" to 600_000,
    "PBKDF2withHmacSHA512" to 210_000,
)

private val generateSecretFunMatcher = FunMatcher {
    qualifier = "javax.crypto.SecretKeyFactory"
    name = "generateSecret"
    withArguments("java.security.spec.KeySpec")
}
private val getInstanceFunMatcher = FunMatcher {
    qualifier = "javax.crypto.SecretKeyFactory"
    name = "getInstance"
    withArguments(STRING_TYPE)
}
private val pbeKeySpecConstructorMatchers = listOf(
    FunMatcher {
        qualifier = "javax.crypto.spec.PBEKeySpec"
        matchConstructor = true
        withArguments(CHAR_ARRAY_TYPE, BYTE_ARRAY_TYPE, INT_TYPE)
    },
    FunMatcher {
        qualifier = "javax.crypto.spec.PBEKeySpec"
        matchConstructor = true
        withArguments(CHAR_ARRAY_TYPE, BYTE_ARRAY_TYPE, INT_TYPE, INT_TYPE)
    },
)

@Rule(key = "S5344")
class PasswordPlaintextFastHashingCheck : CallAbstractCheck() {

    override val functionsToVisit = listOf(generateSecretFunMatcher)

    override fun visitFunctionCall(
        callExpression: KtCallExpression,
        resolvedCall: KaFunctionCall<*>,
        kotlinFileContext: KotlinFileContext,
    ) = withKaSession {
        val secretKeyFactoryCall = callExpression.predictReceiverExpression()
            ?.resolveToCall()
            ?.successfulFunctionCallOrNull()
            ?.takeIf { getInstanceFunMatcher.matches(it) }
            ?: return@withKaSession
        val keySpecCall = resolvedCall.argumentMapping.keys.single().predictRuntimeValueExpression()
            .resolveToCall()
            ?.successfulConstructorCallOrNull()
            ?.takeIf { call -> pbeKeySpecConstructorMatchers.any { matcher -> matcher.matches(call) } }
            ?: return@withKaSession

        val iterationCountExpression = keySpecCall.argumentMapping.keys.elementAtOrNull(2) ?: return@withKaSession
        val iterationCountValueExpression = iterationCountExpression
            .predictRuntimeValueFromParameterDefault()
            .predictRuntimeValueExpression()
        val iterationCount = iterationCountValueExpression.predictRuntimeIntValue() ?: return@withKaSession

        val algorithmValueExpression = secretKeyFactoryCall.argumentMapping.keys.single()
            .predictRuntimeValueFromParameterDefault()
            .predictRuntimeValueExpression()
        val algorithm = algorithmValueExpression.stringValue() ?: return@withKaSession
        val minIteration = minIterations[algorithm] ?: return@withKaSession

        if (iterationCount < minIteration) {
            val secondaryLocations = mutableListOf(SecondaryLocation(kotlinFileContext.textRange(algorithmValueExpression)))
            if (iterationCountValueExpression.textRange != iterationCountExpression.textRange) {
                secondaryLocations.add(SecondaryLocation(kotlinFileContext.textRange(iterationCountValueExpression)))
            }

            kotlinFileContext.reportIssue(iterationCountExpression, MESSAGE.format(minIteration), secondaryLocations)
        }
    }

    private fun KtExpression.predictRuntimeValueFromParameterDefault(): KtExpression = withKaSession {
        resolveToCall()
            ?.successfulVariableAccessCall()
            ?.symbol
            ?.psi
            ?.let { it as? KtParameter }
            ?.defaultValue
            ?: this@predictRuntimeValueFromParameterDefault
    }
}
