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
import org.jetbrains.kotlin.analysis.api.resolution.successfulFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.successfulVariableAccessCall
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.checks.FUNS_ACCEPTING_DISPATCHERS
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.checks.KOTLINX_COROUTINES_PACKAGE
import org.sonarsource.kotlin.api.checks.getParentCallExpr
import org.sonarsource.kotlin.api.checks.isSuspending
import org.sonarsource.kotlin.api.checks.matches
import org.sonarsource.kotlin.api.checks.predictRuntimeValueExpression
import org.sonarsource.kotlin.api.checks.suspendModifier
import org.sonarsource.kotlin.api.checks.throwsExceptions
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.withKaSession

val THREAD_SLEEP_MATCHER = FunMatcher(qualifier = "java.lang.Thread", name = "sleep")

val BLOCKING_ANNOTATIONS = setOf(
    "java.io.IOException",
    "java.io.EOFException",
    "java.io.FileNotFoundException",
    "java.io.InterruptedIOException",
    "java.io.ObjectStreamException",
    "java.awt.print.PrinterException",
    "java.awt.print.PrinterIOException",
    "java.net.HttpRetryException",
    "java.net.SocketTimeoutException",
    "java.net.SocketException",
    "java.net.http.HttpTimeoutException",
    "java.net.http.WebSocketHandshakeException",
    "java.net.http.HttpConnectTimeoutException",
    "java.sql.SQLException",
    "java.sql.BatchUpdateException",
    "java.sql.SQLTimeoutException",
    "javax.imageio.IIOException",
    "javax.imageio.metadata.IIOInvalidTreeException",
    "javax.net.ssl.SSLException",
)

@Rule(key = "S6307")
class MainSafeCoroutinesCheck : AbstractCheck() {

    override fun visitNamedFunction(function: KtNamedFunction, context: KotlinFileContext) {
        val suspendModifier = function.suspendModifier()
        if (suspendModifier != null) {
            function.reportBlockingFunctionCalls(context)
        } else {
            function.forEachDescendantOfType<KtLambdaArgument> {
                if (it.isSuspending()) it.reportBlockingFunctionCalls(context)
            }
        }
    }

    private fun KtElement.reportBlockingFunctionCalls(context: KotlinFileContext) = withKaSession {
        forEachDescendantOfType<KtCallExpression> { call ->
            val resolvedCall = call.resolveToCall()?.successfulFunctionCallOrNull()
            if (resolvedCall matches THREAD_SLEEP_MATCHER) {
                context.reportIssue(call.calleeExpression!!, """Replace this "Thread.sleep()" call with "delay()".""")
            } else {
                resolvedCall?.partiallyAppliedSymbol?.symbol?.let { descriptor ->
                    if (call.isInsideNonSafeDispatcher()
                        && descriptor.throwsExceptions(BLOCKING_ANNOTATIONS)
                    ) {
                        context.reportIssue(call.calleeExpression!!,
                            """Use "Dispatchers.IO" to run this potentially blocking operation.""")
                    }
                }
            }
        }
    }
}

private fun KtCallExpression.isInsideNonSafeDispatcher(): Boolean = withKaSession {
    var parentCallExpr: KtElement? = getParentCallExpr() ?: return true
    var resolvedCall = parentCallExpr?.resolveToCall()?.successfulFunctionCallOrNull() ?: return false

    while (!FUNS_ACCEPTING_DISPATCHERS.any { resolvedCall matches it }) {
        parentCallExpr = parentCallExpr?.getParentCallExpr()
        if (parentCallExpr == null) return true
        val newResolvedCall = parentCallExpr.resolveToCall()?.successfulFunctionCallOrNull()
        if (newResolvedCall === resolvedCall || newResolvedCall == null) return false
        resolvedCall = newResolvedCall
    }

    return resolvedCall.usesNonSafeDispatcher()
}

private fun KaFunctionCall<*>.usesNonSafeDispatcher(): Boolean = withKaSession {
    val arg = argumentMapping.entries
        .find { (_, parameter) -> parameter.name.asString() == "context" }
        ?.key

    val argValue = (arg?.predictRuntimeValueExpression() as? KtDotQualifiedExpression)
        ?.resolveToCall()?.successfulVariableAccessCall()?.symbol?.callableId?.asFqNameForDebugInfo()?.toString()

    return arg == null
        || argValue == "$KOTLINX_COROUTINES_PACKAGE.Dispatchers.Main"
        || argValue == "$KOTLINX_COROUTINES_PACKAGE.Dispatchers.Default"
}
