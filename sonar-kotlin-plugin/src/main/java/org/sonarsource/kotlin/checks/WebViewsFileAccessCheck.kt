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

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.resolve.calls.util.getFirstArgumentExpression
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.CallAbstractCheck
import org.sonarsource.kotlin.api.FunMatcher
import org.sonarsource.kotlin.api.predictRuntimeBooleanValue
import org.sonarsource.kotlin.api.setterMatches
import org.sonarsource.kotlin.plugin.KotlinFileContext

private const val MESSAGE = "Make sure that enabling file access is safe here."

private val PROPERTY_NAMES = arrayOf(
    "allowFileAccess",
    "allowFileAccessFromFileURLs",
    "allowContentAccess",
    "allowUniversalAccessFromFileURLs",
).asSequence()

private val ANDROID_FILE_ACCESS_MATCHER = FunMatcher(definingSupertype = "android.webkit.WebSettings") {
    withNames(
        "setAllowFileAccess",
        "setAllowFileAccessFromFileURLs",
        "setAllowContentAccess",
        "setAllowUniversalAccessFromFileURLs",
    )
    withArguments("kotlin.Boolean")
}

@Rule(key = "S6363")
class WebViewsFileAccessCheck : CallAbstractCheck() {

    override val functionsToVisit = listOf(ANDROID_FILE_ACCESS_MATCHER)

    override fun visitFunctionCall(callExpression: KtCallExpression, resolvedCall: ResolvedCall<*>, kotlinFileContext: KotlinFileContext) {
        checkFileAccessArgument(kotlinFileContext, resolvedCall.getFirstArgumentExpression())
    }

    override fun visitBinaryExpression(expression: KtBinaryExpression, ctx: KotlinFileContext) {
        val left = KtPsiUtil.deparenthesize(expression.left) ?: return
        if (expression.operationToken == KtTokens.EQ) {
            PROPERTY_NAMES
                .firstOrNull { propertyName -> left.setterMatches(ctx.bindingContext, propertyName, ANDROID_FILE_ACCESS_MATCHER) }
                ?.let { checkFileAccessArgument(ctx, expression.right) }
        }
    }

    private fun checkFileAccessArgument(ctx: KotlinFileContext, argument: KtExpression?) {
        if (argument?.predictRuntimeBooleanValue(ctx.bindingContext) == true) {
            ctx.reportIssue(argument, MESSAGE)
        }
    }

}
