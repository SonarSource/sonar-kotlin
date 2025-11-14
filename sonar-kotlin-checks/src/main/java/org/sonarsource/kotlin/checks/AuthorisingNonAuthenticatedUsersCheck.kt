/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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
import org.jetbrains.kotlin.analysis.api.resolution.singleFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.successfulFunctionCallOrNull
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.CallAbstractCheck
import org.sonarsource.kotlin.api.checks.ConstructorMatcher
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.checks.predictReceiverExpression
import org.sonarsource.kotlin.api.checks.predictRuntimeBooleanValue
import org.sonarsource.kotlin.api.reporting.SecondaryLocation
import org.sonarsource.kotlin.api.reporting.KotlinTextRanges.textRange
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.withKaSession

private const val BUILDER = "android.security.keystore.KeyGenParameterSpec.Builder"

private val KEY_GEN_BUILDER_MATCHER = ConstructorMatcher(typeName = BUILDER)
private val KEY_GEN_BUILDER_BUILD_MATCHER = FunMatcher(qualifier = BUILDER, name = "build")

private val KEY_GEN_BUILDER_SET_AUTH_MATCHER = FunMatcher(qualifier = BUILDER, name = "setUserAuthenticationRequired") {
    withArguments("kotlin.Boolean")
}

@Rule(key = "S6288")
class AuthorisingNonAuthenticatedUsersCheck : CallAbstractCheck() {
    override val functionsToVisit = listOf(KEY_GEN_BUILDER_BUILD_MATCHER)

    override fun visitFunctionCall(
        callExpression: KtCallExpression,
        resolvedCall: KaFunctionCall<*>,
        kotlinFileContext: KotlinFileContext,
    ) = withKaSession {
        var receiver = callExpression.resolveToCall()?.successfulFunctionCallOrNull() ?: return
        var callElement = callExpression
        val secondaryLocations = mutableListOf<SecondaryLocation>()

        while (!KEY_GEN_BUILDER_MATCHER.matches(receiver)) {

            if (KEY_GEN_BUILDER_SET_AUTH_MATCHER.matches(receiver)) {
               if (receiver.argumentMapping.keys.first().predictRuntimeBooleanValue() != false)
                   return
               secondaryLocations.add(SecondaryLocation(kotlinFileContext.textRange(callElement.calleeExpression!!)))
            }
            val receiverExpression = callElement.predictReceiverExpression()
            callElement = when(receiverExpression) {
                is KtCallExpression -> receiverExpression
                is KtDotQualifiedExpression -> receiverExpression.selectorExpression as? KtCallExpression
                else -> null
            } ?: return

            receiver = receiverExpression?.resolveToCall()?.singleFunctionCallOrNull() ?: return
        }
        kotlinFileContext.reportIssue(callElement.calleeExpression!!,
            "Make sure authorizing non-authenticated users to use this key is safe here.",
            secondaryLocations)
    }

}
