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

import org.jetbrains.kotlin.analysis.api.KaIdeApi
import org.jetbrains.kotlin.analysis.api.resolution.KaCallableMemberCall
import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.analysis.api.resolution.successfulCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.successfulFunctionCallOrNull
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPsiUtil.deparenthesize
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.CallAbstractCheck
import org.sonarsource.kotlin.api.checks.ConstructorMatcher
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.checks.FunMatcherImpl
import org.sonarsource.kotlin.api.checks.getFirstArgumentExpression
import org.sonarsource.kotlin.api.checks.predictRuntimeIntValue
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.KtTreeVisitor
import org.sonarsource.kotlin.api.visiting.withKaSession

private const val CLEARTEXT_FQN = "okhttp3.ConnectionSpec.Companion.CLEARTEXT"

private const val MESSAGE_ANDROID_MIXED_CONTENT = "Using a relaxed mixed content policy is security-sensitive."
private const val MIXED_CONTENT_ALWAYS_ALLOW = 0

private val UNSAFE_CALLS_GENERAL = mapOf(
    ConstructorMatcher("org.apache.commons.net.ftp.FTPClient") to msg("FTP", "SFTP, SCP or FTPS"),
    ConstructorMatcher("org.apache.commons.net.smtp.SMTPClient")
        to msg("clear-text SMTP", "SMTP over SSL/TLS or SMTP with STARTTLS"),
    ConstructorMatcher("org.apache.commons.net.telnet.TelnetClient") to msg("Telnet", "SSH"),
)

private val UNSAFE_CALLS_OK_HTTP = listOf(
    ConstructorMatcher("okhttp3.ConnectionSpec.Builder"),
    FunMatcher(qualifier = "okhttp3.OkHttpClient.Builder", name = "connectionSpecs")
)

private val ANDROID_SET_MIXED_CONTENT_MODE = FunMatcher(definingSupertype = "android.webkit.WebSettings",
    name = "setMixedContentMode") { withArguments("kotlin.Int") }

private fun msg(insecure: String, replaceWith: String) = "Using $insecure is insecure. Use $replaceWith instead."

@Rule(key = "S5332")
class ClearTextProtocolCheck : CallAbstractCheck() {

    override val functionsToVisit = UNSAFE_CALLS_GENERAL.keys + UNSAFE_CALLS_OK_HTTP + listOf(ANDROID_SET_MIXED_CONTENT_MODE)

    override fun visitFunctionCall(
        callExpression: KtCallExpression,
        resolvedCall: KaFunctionCall<*>,
        matchedFun: FunMatcherImpl,
        kotlinFileContext: KotlinFileContext,
    ) = withKaSession {
        UNSAFE_CALLS_GENERAL[matchedFun]?.let { msg ->
            kotlinFileContext.reportIssue(callExpression, msg)
            return
        }

        if (matchedFun in UNSAFE_CALLS_OK_HTTP) {
            analyzeOkHttpCall(kotlinFileContext, callExpression)
        } else if (matchedFun == ANDROID_SET_MIXED_CONTENT_MODE) {
            checkAndroidMixedContentArgument(kotlinFileContext,
                deparenthesize(callExpression.resolveToCall()?.successfulFunctionCallOrNull()
                    ?.getFirstArgumentExpression())
            )
        }
    }

    override fun visitBinaryExpression(expression: KtBinaryExpression, ctx: KotlinFileContext) = withKaSession {
        if (expression.operationToken == KtTokens.EQ &&
            ANDROID_SET_MIXED_CONTENT_MODE.matches(expression.resolveToCall()?.successfulCallOrNull<KaCallableMemberCall<*, *>>())
        ) {
            checkAndroidMixedContentArgument(ctx, deparenthesize(expression.right))
        }
    }

    private fun checkAndroidMixedContentArgument(ctx: KotlinFileContext, argument: KtExpression?) {
        if (argument != null && argument.predictRuntimeIntValue() == MIXED_CONTENT_ALWAYS_ALLOW) {
            ctx.reportIssue(argument, MESSAGE_ANDROID_MIXED_CONTENT)
        }
    }

    private fun analyzeOkHttpCall(kotlinFileContext: KotlinFileContext, callExpr: KtCallExpression) =
        OkHttpArgumentFinder { arg ->
            kotlinFileContext.reportIssue(arg, msg("HTTP", "HTTPS"))
        }.visitTree(callExpr)
}

private class OkHttpArgumentFinder(
    private val issueReporter: (KtSimpleNameExpression) -> Unit,
) : KtTreeVisitor() {
    @OptIn(KaIdeApi::class)
    override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) = withKaSession {
        if (expression.mainReference.resolveToSymbol()?.importableFqName?.asString() == CLEARTEXT_FQN) issueReporter(expression)
    }
}
