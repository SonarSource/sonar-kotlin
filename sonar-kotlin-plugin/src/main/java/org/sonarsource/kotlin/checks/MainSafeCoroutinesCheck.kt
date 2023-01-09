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

import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.util.getParentCall
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.api.FUNS_ACCEPTING_DISPATCHERS
import org.sonarsource.kotlin.api.FunMatcher
import org.sonarsource.kotlin.api.KOTLINX_COROUTINES_PACKAGE
import org.sonarsource.kotlin.api.isSuspending
import org.sonarsource.kotlin.api.matches
import org.sonarsource.kotlin.api.predictRuntimeValueExpression
import org.sonarsource.kotlin.api.resolveReferenceTarget
import org.sonarsource.kotlin.api.suspendModifier
import org.sonarsource.kotlin.api.throwsExceptions
import org.sonarsource.kotlin.plugin.KotlinFileContext

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
        val bindingContext = context.bindingContext
        val suspendModifier = function.suspendModifier()
        if (suspendModifier != null) {
            function.reportBlockingFunctionCalls(context)
        } else {
            function.forEachDescendantOfType<KtLambdaArgument> {
                if (it.isSuspending(bindingContext)) it.reportBlockingFunctionCalls(context)
            }
        }
    }

    private fun KtElement.reportBlockingFunctionCalls(context: KotlinFileContext) {
        val bindingContext = context.bindingContext
        forEachDescendantOfType<KtCallExpression> { call ->
            val resolvedCall = call.getResolvedCall(bindingContext)
            if (resolvedCall matches THREAD_SLEEP_MATCHER) {
                context.reportIssue(call.calleeExpression!!, """Replace this "Thread.sleep()" call with "delay()".""")
            } else {
                resolvedCall?.resultingDescriptor?.let { descriptor ->
                    if (isInsideNonSafeDispatcher(call, bindingContext)
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

private fun isInsideNonSafeDispatcher(
    callExpr: KtCallExpression,
    bindingContext: BindingContext,
): Boolean {
    var parentCall: Call? = callExpr.getParentCall(bindingContext) ?: return true
    var resolvedCall = parentCall.getResolvedCall(bindingContext) ?: return false

    while (!FUNS_ACCEPTING_DISPATCHERS.any { resolvedCall matches it }) {
        parentCall = parentCall?.callElement?.getParentCall(bindingContext)
        if (parentCall == null) return true
        val newResolvedCall = parentCall.getResolvedCall(bindingContext)
        if (newResolvedCall === resolvedCall || newResolvedCall == null) return false
        resolvedCall = newResolvedCall
    }

    return resolvedCall.usesNonSafeDispatcher(bindingContext)
}

private fun ResolvedCall<*>.usesNonSafeDispatcher(bindingContext: BindingContext): Boolean {
    val arg = valueArgumentsByIndex?.get(0)
    val argValue = ((arg as? ExpressionValueArgument)
        ?.valueArgument
        ?.getArgumentExpression()
        ?.predictRuntimeValueExpression(bindingContext) as? KtDotQualifiedExpression)
        ?.resolveReferenceTarget(bindingContext)
        ?.fqNameOrNull()
        ?.asString()

    return arg == null
        || arg is DefaultValueArgument
        || argValue == "$KOTLINX_COROUTINES_PACKAGE.Dispatchers.Main"
        || argValue == "$KOTLINX_COROUTINES_PACKAGE.Dispatchers.Default"
}
