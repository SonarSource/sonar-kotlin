/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2023 SonarSource SA
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

import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression
import org.jetbrains.kotlin.psi.psiUtil.isNull
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.CallAbstractCheck
import org.sonarsource.kotlin.api.FunMatcher
import org.sonarsource.kotlin.api.FunMatcherImpl
import org.sonarsource.kotlin.api.SecondaryLocation
import org.sonarsource.kotlin.converter.KotlinTextRanges.textRange
import org.sonarsource.kotlin.plugin.KotlinFileContext

private val ANDROID_HARDWARE_AUTH = FunMatcher(qualifier = "android.hardware.biometrics.BiometricPrompt", name = "authenticate")
private val ANDROIDX_AUTH = FunMatcher(qualifier = "androidx.biometric.BiometricPrompt", name = "authenticate")

private const val MESSAGE = """Make sure performing a biometric authentication without a "CryptoObject" is safe here."""

@Rule(key = "S6293")
class BiometricAuthWithoutCryptoCheck : CallAbstractCheck() {
    override val functionsToVisit = setOf(ANDROID_HARDWARE_AUTH, ANDROIDX_AUTH)

    override fun visitFunctionCall(
        callExpression: KtCallExpression,
        resolvedCall: ResolvedCall<*>,
        matchedFun: FunMatcherImpl,
        kotlinFileContext: KotlinFileContext,
    ) {
        when(matchedFun) {
            ANDROID_HARDWARE_AUTH -> checkCall(callExpression, kotlinFileContext, 4, 0)
            ANDROIDX_AUTH -> checkCall(callExpression, kotlinFileContext, 2, 1)
        }
    }

    private fun checkCall(callExpression: KtCallExpression, ctx: KotlinFileContext, numberOfSafeArgs: Int, argumentIndex: Int) {
        if (callExpression.valueArguments.size < numberOfSafeArgs) {
            // No CryptoObject -> automatically insecure.
            ctx.reportIssue(callExpression, MESSAGE)
        } else {
            // CryptoObject is null -> also insecure.
            callExpression.valueArguments[argumentIndex].getArgumentExpression()?.let { relevantArg ->
                if (relevantArg.isNull()) {
                    val secondaryExpression = callExpression.getCallNameExpression() ?: callExpression
                    ctx.reportIssue(relevantArg, MESSAGE, listOf(SecondaryLocation(ctx.textRange(secondaryExpression))))
                }
            }
        }
    }
}
