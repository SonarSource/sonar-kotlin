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
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.BYTE_ARRAY_CONSTRUCTOR
import org.sonarsource.kotlin.api.checks.BYTE_ARRAY_CONSTRUCTOR_SIZE_ARG_ONLY
import org.sonarsource.kotlin.api.checks.CallAbstractCheck
import org.sonarsource.kotlin.api.checks.ConstructorMatcher
import org.sonarsource.kotlin.api.checks.FunMatcherImpl
import org.sonarsource.kotlin.api.checks.isBytesInitializedFromString
import org.sonarsource.kotlin.api.checks.isInitializedPredictably
import org.sonarsource.kotlin.api.checks.matches
import org.sonarsource.kotlin.api.checks.predictRuntimeIntValue
import org.sonarsource.kotlin.api.checks.predictRuntimeValueExpression
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.frontend.secondaryOf
import org.sonarsource.kotlin.api.visiting.analyze

private const val SPECS_PACKAGE = "javax.crypto.spec"
private const val KEY_SPEC_FUN_NAME = "PBEKeySpec"
private const val PARAMETER_SPEC_FUN_NAME = "PBEParameterSpec"

private const val MSG_MAKE_UNPREDICTABLE = "Make this salt unpredictable."
private const val MSG_ADD_SALT = "Add an unpredictable salt value to this hash."
private const val MSG_MIN_LEN = "Make this salt at least 16 bytes."
private const val SMSG_PREDICTABLE_SALT = "Predictable salt value"

private val matcherSaltIndexMap = mapOf(
    ConstructorMatcher("$SPECS_PACKAGE.$KEY_SPEC_FUN_NAME") to 1,
    ConstructorMatcher("$SPECS_PACKAGE.$PARAMETER_SPEC_FUN_NAME") to 0,
)

@Rule(key = "S2053")
class UnpredictableHashSaltCheck : CallAbstractCheck() {
    override val functionsToVisit = matcherSaltIndexMap.keys

    override fun visitFunctionCall(
        callExpression: KtCallExpression,
        resolvedCall: KaFunctionCall<*>,
        matchedFun: FunMatcherImpl,
        kotlinFileContext: KotlinFileContext
    ) {
        val saltArgIndex = matcherSaltIndexMap[matchedFun]!!

        if (callExpression.valueArguments.size < 2) {
            kotlinFileContext.reportIssue(callExpression, MSG_ADD_SALT)
            return
        }

        val saltArg = resolvedCall.argumentMapping.keys.toList().elementAtOrNull(saltArgIndex) ?: return

        val predictedSaltValue = saltArg.predictRuntimeValueExpression()

        if (predictedSaltValue.isBytesInitializedFromString()) {
            kotlinFileContext.reportIssue(
                saltArg,
                MSG_MAKE_UNPREDICTABLE,
                listOf(kotlinFileContext.secondaryOf(predictedSaltValue, SMSG_PREDICTABLE_SALT))
            )
            return
        }

        val saltInitializer = analyze { predictedSaltValue.resolveToCall()?.successfulFunctionCallOrNull()
            ?.takeIf { it matches BYTE_ARRAY_CONSTRUCTOR } ?: return }

        if (saltInitializer.byteArrayInitSizeTooSmall()) {
            kotlinFileContext.reportIssue(saltArg, MSG_MIN_LEN, listOf(kotlinFileContext.secondaryOf(predictedSaltValue)))
        }

        if (saltInitializer matches BYTE_ARRAY_CONSTRUCTOR_SIZE_ARG_ONLY &&
            saltArg.isInitializedPredictably(predictedSaltValue)
        ) {
            kotlinFileContext.reportIssue(
                saltArg,
                MSG_MAKE_UNPREDICTABLE,
                listOf(kotlinFileContext.secondaryOf(predictedSaltValue, SMSG_PREDICTABLE_SALT))
            )
        }
    }

    /**
     * Checks whether the first argument of the call is an integer that is at least 16
     */
    private fun KaFunctionCall<*>.byteArrayInitSizeTooSmall(): Boolean {
        return (argumentMapping.keys.toList().firstOrNull()?.predictRuntimeIntValue() ?: 16) < 16
    }
}
