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

import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
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
import org.sonarsource.kotlin.api.checks.simpleArgExpressionOrNull
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.frontend.secondaryOf

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

@org.sonarsource.kotlin.api.frontend.K1only
@Rule(key = "S2053")
class UnpredictableHashSaltCheck : CallAbstractCheck() {
    override val functionsToVisit = matcherSaltIndexMap.keys

    override fun visitFunctionCall(
        callExpression: KtCallExpression,
        resolvedCall: ResolvedCall<*>,
        matchedFun: FunMatcherImpl,
        kotlinFileContext: KotlinFileContext
    ) {
        val saltArgIndex = matcherSaltIndexMap[matchedFun]!!

        if (callExpression.valueArguments.size < 2) {
            kotlinFileContext.reportIssue(callExpression, MSG_ADD_SALT)
            return
        }

        val bindingContext = kotlinFileContext.bindingContext
        val saltArg = resolvedCall.valueArgumentsByIndex?.elementAtOrNull(saltArgIndex)
            ?.arguments?.elementAtOrNull(0)?.getArgumentExpression() ?: return

        val predictedSaltValue = saltArg.predictRuntimeValueExpression(bindingContext)

        if (predictedSaltValue.isBytesInitializedFromString(bindingContext)) {
            kotlinFileContext.reportIssue(
                saltArg,
                MSG_MAKE_UNPREDICTABLE,
                listOf(kotlinFileContext.secondaryOf(predictedSaltValue, SMSG_PREDICTABLE_SALT))
            )
            return
        }

        val saltInitializer = predictedSaltValue.getResolvedCall(bindingContext)
            ?.takeIf { it matches BYTE_ARRAY_CONSTRUCTOR } ?: return

        if (saltInitializer.byteArrayInitSizeTooSmall(bindingContext)) {
            kotlinFileContext.reportIssue(saltArg, MSG_MIN_LEN, listOf(kotlinFileContext.secondaryOf(predictedSaltValue)))
        }

        if (saltInitializer matches BYTE_ARRAY_CONSTRUCTOR_SIZE_ARG_ONLY &&
            saltArg.isInitializedPredictably(predictedSaltValue, bindingContext)
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
    private fun ResolvedCall<*>.byteArrayInitSizeTooSmall(bindingContext: BindingContext) =
        (this.simpleArgExpressionOrNull(0)?.predictRuntimeIntValue(bindingContext) ?: 16) < 16
}
