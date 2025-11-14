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

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCatchClause
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtLoopExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtSafeQualifiedExpression
import org.jetbrains.kotlin.psi.KtThrowExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.KtTryExpression
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.withKaSession

private val javaCryptographyExtensionFunMatchers = listOf(
    FunMatcher {
        definingSupertype = "javax.net.ssl.X509TrustManager"
        withNames("checkClientTrusted", "checkServerTrusted")
    },
    FunMatcher {
        definingSupertype = "javax.net.ssl.X509ExtendedTrustManager"
        withNames("checkClientTrusted", "checkServerTrusted")
    },
)

private val androidWebViewFunMatchers = listOf(
    FunMatcher {
        definingSupertype = "android.webkit.WebViewClient"
        withNames("onReceivedSslError")
    },
)

private val androidSslErrorHandlerProceedFunMatcher = FunMatcher {
    definingSupertype = "android.webkit.SslErrorHandler"
    withNames("proceed")
}

private val androidSslErrorHandlerCancelFunMatcher = FunMatcher {
    definingSupertype = "android.webkit.SslErrorHandler"
    withNames("cancel")
}

@Rule(key = "S4830")
class ServerCertificateCheck : AbstractCheck() {

    override fun visitNamedFunction(function: KtNamedFunction, kotlinFileContext: KotlinFileContext) {
        val match = when {
            javaCryptographyExtensionFunMatchers.any { it.matches(function) } ->
                !function.callsCheckTrusted() && !function.throwsCertificateExceptionWithoutCatching()
            androidWebViewFunMatchers.any { it.matches(function) } ->
                function.callsProceedUnconditionally()
            else -> false
        }
        if (match) {
            kotlinFileContext.reportIssue(function.nameIdentifier ?: function,
                "Enable server certificate validation on this SSL/TLS connection.")
        }
    }

    private fun KtNamedFunction.callsCheckTrusted(): Boolean {
        val visitor = object : KtTreeVisitorVoid() {
            private var foundCheckTrustedCall: Boolean = false

            override fun visitCallExpression(expression: KtCallExpression) {
                foundCheckTrustedCall = foundCheckTrustedCall || javaCryptographyExtensionFunMatchers.any { it.matches(expression) }
                super.visitCallExpression(expression)
            }

            fun callsCheckTrusted(): Boolean = foundCheckTrustedCall
        }
        visitor.visitElement(this)
        return visitor.callsCheckTrusted()
    }

    /*
     * Returns true only when the function throws a CertificateException without a catch against it.
     */
    private fun KtNamedFunction.throwsCertificateExceptionWithoutCatching(): Boolean {
        val visitor = ThrowCatchVisitor()
        visitor.visitElement(this)
        return with(visitor) { throwFound && !catchFound }
    }

    /*
     * The following heuristic is used to determine if the call to "proceed" is unconditional:
     * proceed is called, cancel is never called, and there are no branching statements in the
     * function (no matter if around the proceed or not).
     */
    private fun KtNamedFunction.callsProceedUnconditionally(): Boolean {
        val visitor = AndroidWebViewCandidateVisitor()
        visitor.visitElement(this)
        return with(visitor) { proceedFound && !cancelFound && !potentialBranching }
    }

    private class ThrowCatchVisitor : KtTreeVisitorVoid() {
        private val certificateExceptionClassId = ClassId.fromString("java/security/cert/CertificateException")

        var throwFound: Boolean = false
        var catchFound: Boolean = false

        override fun visitThrowExpression(expression: KtThrowExpression) = withKaSession {
            throwFound = throwFound ||
                expression.thrownExpression?.expressionType?.isClassType(certificateExceptionClassId) == true
            super.visitThrowExpression(expression)
        }

        override fun visitCatchSection(catchClause: KtCatchClause) = withKaSession {
            catchFound = catchFound ||
                catchClause.catchParameter?.symbol?.returnType?.isClassType(certificateExceptionClassId) == true
            super.visitCatchSection(catchClause)
        }
    }

    private class AndroidWebViewCandidateVisitor : KtTreeVisitorVoid() {
        var proceedFound: Boolean = false
        var cancelFound: Boolean = false
        var potentialBranching: Boolean = false

        override fun visitCallExpression(expression: KtCallExpression) {
            when {
                androidSslErrorHandlerProceedFunMatcher.matches(expression) -> proceedFound = true
                androidSslErrorHandlerCancelFunMatcher.matches(expression) -> cancelFound = true
            }
            super.visitCallExpression(expression)
        }

        override fun visitExpression(expression: KtExpression) {
            potentialBranching = potentialBranching ||
                when (expression) {
                    is KtIfExpression,
                    is KtWhenExpression,
                    is KtTryExpression,
                    is KtLoopExpression,
                    is KtLambdaExpression,
                    is KtSafeQualifiedExpression -> true
                    is KtBinaryExpression -> expression.operationToken == KtTokens.ELVIS
                    else -> false
                }
            super.visitExpression(expression)
        }
    }
}
