/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2026 SonarSource SA
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

import org.jetbrains.kotlin.analysis.api.resolution.KaCallableMemberCall
import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.analysis.api.resolution.singleCallOrNull
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.ArgumentMatcher
import org.sonarsource.kotlin.api.checks.BYTE_ARRAY_CONSTRUCTOR_SIZE_ARG_ONLY
import org.sonarsource.kotlin.api.checks.CallAbstractCheck
import org.sonarsource.kotlin.api.checks.ConstructorMatcher
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.checks.isBytesInitializedFromString
import org.sonarsource.kotlin.api.checks.isInitializedPredictably
import org.sonarsource.kotlin.api.checks.predictRuntimeValueExpression
import org.sonarsource.kotlin.api.frontend.secondaryOf
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.withKaSession

private const val MESSAGE = "Change this seed value to something unpredictable, or remove the seed."
private const val SECURE_RANDOM = "java.security.SecureRandom"

@Rule(key = "S4347")
class UnpredictableSecureRandomSaltCheck : CallAbstractCheck() {
    override val functionsToVisit = listOf(
        FunMatcher(qualifier = SECURE_RANDOM, name = "setSeed"),
        ConstructorMatcher(SECURE_RANDOM) {
            withArguments(ArgumentMatcher.ANY)
        }
    )

    override fun visitFunctionCall(callExpression: KtCallExpression, resolvedCall: KaFunctionCall<*>, kotlinFileContext: KotlinFileContext) = withKaSession {
        val saltArg = resolvedCall.argumentMapping.keys.firstOrNull() ?: return@withKaSession
        val predictedSaltValue = saltArg.predictRuntimeValueExpression()

        if (predictedSaltValue is KtConstantExpression || predictedSaltValue.isBytesInitializedFromString()) {
            kotlinFileContext.reportIssue(saltArg, MESSAGE, listOf(kotlinFileContext.secondaryOf(predictedSaltValue)))
        } else if (
            BYTE_ARRAY_CONSTRUCTOR_SIZE_ARG_ONLY.matches(predictedSaltValue.resolveToCall()?.singleCallOrNull<KaCallableMemberCall<*, *>>()) &&
            saltArg.isInitializedPredictably(predictedSaltValue)
        ) {
            kotlinFileContext.reportIssue(saltArg, MESSAGE, listOf(kotlinFileContext.secondaryOf(predictedSaltValue)))
        }
    }
}
