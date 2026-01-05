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
import org.jetbrains.kotlin.analysis.api.resolution.successfulCallOrNull
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.CallAbstractCheck
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.checks.getFirstArgumentExpression
import org.sonarsource.kotlin.api.checks.predictRuntimeBooleanValue
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.withKaSession

private const val MESSAGE = "Make sure that enabling file access is safe here."

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

    override fun visitFunctionCall(callExpression: KtCallExpression, resolvedCall: KaFunctionCall<*>, kotlinFileContext: KotlinFileContext) {
        checkFileAccessArgument(kotlinFileContext, resolvedCall.getFirstArgumentExpression())
    }

    override fun visitBinaryExpression(expression: KtBinaryExpression, ctx: KotlinFileContext) = withKaSession {
        if (expression.operationToken == KtTokens.EQ
            && ANDROID_FILE_ACCESS_MATCHER.matches(
                expression.resolveToCall()?.successfulCallOrNull<KaCallableMemberCall<*, *>>()
            )
        ) {
            checkFileAccessArgument(ctx, expression.right)
        }
    }

    private fun checkFileAccessArgument(ctx: KotlinFileContext, argument: KtExpression?) {
        if (argument?.predictRuntimeBooleanValue() == true) {
            ctx.reportIssue(argument, MESSAGE)
        }
    }

}
