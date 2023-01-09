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
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.util.getCall
import org.jetbrains.kotlin.resolve.calls.util.getType
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.CallAbstractCheck
import org.sonarsource.kotlin.api.ConstructorMatcher
import org.sonarsource.kotlin.api.FunMatcher
import org.sonarsource.kotlin.api.SecondaryLocation
import org.sonarsource.kotlin.api.predictReceiverExpression
import org.sonarsource.kotlin.converter.KotlinTextRanges.textRange
import org.sonarsource.kotlin.plugin.KotlinFileContext

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
        resolvedCall: ResolvedCall<*>,
        kotlinFileContext: KotlinFileContext,
    ) {
        val bindingContext = kotlinFileContext.bindingContext
        val call = callExpression.getCall(bindingContext) ?: return
        var receiver = call
        val secondaryLocations = mutableListOf<SecondaryLocation>()

        while (!KEY_GEN_BUILDER_MATCHER.matches(receiver, bindingContext)) {
            val callElement = receiver.callElement as? KtCallExpression ?: return

            if (KEY_GEN_BUILDER_SET_AUTH_MATCHER.matches(receiver, bindingContext)) {
                val argumentExpression = receiver.valueArguments[0]?.getArgumentExpression()!!
                argumentExpression.getType(bindingContext)?.let {
                    val argValue = bindingContext.get(BindingContext.COMPILE_TIME_VALUE, argumentExpression)
                        ?.getValue(it) as? Boolean
                    if (argValue != false) return
                    secondaryLocations.add(SecondaryLocation(kotlinFileContext.textRange(callElement.calleeExpression!!)))
                }
            }

            receiver = callElement.predictReceiverExpression(bindingContext)
                ?.getCall(bindingContext) ?: return
        }
        kotlinFileContext.reportIssue(receiver.calleeExpression!!,
            "Make sure authorizing non-authenticated users to use this key is safe here.",
            secondaryLocations)
    }

}
