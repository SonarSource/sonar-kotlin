/*
 * SonarSource SLang
 * Copyright (C) 2018-2021 SonarSource SA
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
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.api.FunMatcher
import org.sonarsource.kotlin.api.SecondaryLocation
import org.sonarsource.kotlin.api.matches
import org.sonarsource.kotlin.converter.KotlinTextRanges.textRange
import org.sonarsource.kotlin.plugin.KotlinFileContext

private val AUTHENTICATE_FUN_MATCHERS = listOf(
    FunMatcher(qualifier = "android.hardware.biometrics.BiometricPrompt", name = "authenticate"),
    FunMatcher(qualifier = "androidx.biometric.BiometricPrompt", name = "authenticate")
)

private const val MESSAGE = "Make sure performing a biometric authentication without a CryptoObject is safe here"

@Rule(key = "S6293")
class BiometricAuthWithoutCryptoCheck : AbstractCheck() {

    override fun visitCallExpression(callExpression: KtCallExpression, ctx: KotlinFileContext) {
        if (functionCallMatches(callExpression, ctx)) {
            if (callExpression.valueArguments.size < 2) {

                // No second argument -> automatically insecure.
                ctx.reportIssue(callExpression, MESSAGE)

            } else {

                // Second argument is null? Also insecure.
                callExpression.valueArguments[1].getArgumentExpression()?.let { relevantArg ->
                    if (relevantArg.isNull()) {
                        val secondaryExpression = callExpression.getCallNameExpression() ?: callExpression
                        ctx.reportIssue(relevantArg, MESSAGE, listOf(SecondaryLocation(ctx.textRange(secondaryExpression))))
                    }
                }

            }
        }
    }

    private fun functionCallMatches(callExpression: KtCallExpression, ctx: KotlinFileContext) =
        callExpression.getResolvedCall(ctx.bindingContext)?.let { resolvedCall ->
            AUTHENTICATE_FUN_MATCHERS.any { resolvedCall matches it }
        } ?: false
}
