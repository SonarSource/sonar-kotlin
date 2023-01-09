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
import org.jetbrains.kotlin.resolve.calls.util.getFirstArgumentExpression
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.CallAbstractCheck
import org.sonarsource.kotlin.api.FunMatcher
import org.sonarsource.kotlin.api.predictRuntimeBooleanValue
import org.sonarsource.kotlin.plugin.KotlinFileContext

private const val MESSAGE = "Make sure this debug feature is deactivated before delivering the code in production."

@Rule(key = "S4507")
class DebugFeatureEnabledCheck : CallAbstractCheck() {

    override val functionsToVisit = listOf(
        FunMatcher(definingSupertype = "android.webkit.WebView", name = "setWebContentsDebuggingEnabled")
        { withArguments("kotlin.Boolean") },
        FunMatcher(definingSupertype = "android.webkit.WebViewFactoryProvider.Statics", name = "setWebContentsDebuggingEnabled")
        { withArguments("kotlin.Boolean") },
    )

    override fun visitFunctionCall(callExpression: KtCallExpression, resolvedCall: ResolvedCall<*>, kotlinFileContext: KotlinFileContext) {
        if (resolvedCall.getFirstArgumentExpression()?.predictRuntimeBooleanValue(kotlinFileContext.bindingContext) == true) {
            kotlinFileContext.reportIssue(callExpression, MESSAGE)
        }
    }

}
