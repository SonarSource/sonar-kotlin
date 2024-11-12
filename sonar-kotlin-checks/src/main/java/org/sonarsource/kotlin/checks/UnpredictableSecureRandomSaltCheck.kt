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
package org.sonarsource.kotlin.checks

import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.analysis.api.resolution.successfulFunctionCallOrNull
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.*
import org.sonarsource.kotlin.api.frontend.secondaryOf
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.analyze

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

    override fun visitFunctionCall(
        callExpression: KtCallExpression,
        resolvedCall: KaFunctionCall<*>,
        matchedFun: FunMatcherImpl,
        kotlinFileContext: KotlinFileContext
    ) {
        val saltArg = resolvedCall.argumentMapping.keys.toList().firstOrNull() ?: return
        val predictedSaltValue = saltArg.predictRuntimeValueExpression()

        analyze {
            if (
                (predictedSaltValue is KtConstantExpression || predictedSaltValue.isBytesInitializedFromString()) ||
                (predictedSaltValue.resolveToCall()
                    ?.successfulFunctionCallOrNull() matches BYTE_ARRAY_CONSTRUCTOR_SIZE_ARG_ONLY &&
                                saltArg.isInitializedPredictably(predictedSaltValue))
                ) {
                kotlinFileContext.reportIssue(
                    saltArg,
                    MESSAGE,
                    listOf(kotlinFileContext.secondaryOf(predictedSaltValue))
                )
            }
        }
    }
}
