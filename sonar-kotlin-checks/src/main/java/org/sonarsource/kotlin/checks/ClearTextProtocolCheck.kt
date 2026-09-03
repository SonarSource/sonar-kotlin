/*
 * SonarSource Kotlin
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtEscapeStringTemplateEntry
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtPsiUtil.deparenthesize
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.sonar.check.Rule
import org.sonarsource.analyzer.commons.appsec.CleartextProtocolFilter
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
    ConstructorMatcher("org.apache.commons.net.ftp.FTPClient") to msg("ftp"),
    ConstructorMatcher("org.apache.commons.net.smtp.SMTPClient") to msg("smtp"),
    ConstructorMatcher("org.apache.commons.net.telnet.TelnetClient") to msg("telnet"),
)

private val UNSAFE_CALLS_OK_HTTP = listOf(
    ConstructorMatcher("okhttp3.ConnectionSpec.Builder"),
    FunMatcher(qualifier = "okhttp3.OkHttpClient.Builder", name = "connectionSpecs")
)

private val ANDROID_SET_MIXED_CONTENT_MODE = FunMatcher(definingSupertype = "android.webkit.WebSettings",
    name = "setMixedContentMode") { withArguments("kotlin.Int") }

/**
 * [CleartextProtocolFilter] is the single source of truth for the wording, so that a scheme is phrased
 * the same way whether it was found in an API call or in a URL literal. The fallback only guards against
 * a future release dropping a scheme; every scheme used here is currently known to the filter.
 */
private fun msg(scheme: String): String = CleartextProtocolFilter.getIssueMessage(scheme)
    .orElse("Using $scheme is insecure. Use a TLS-protected alternative instead.")

private const val SCHEME_SEPARATOR = "://"

/** Characters that terminate the authority component of a URL, i.e. everything a host may be followed by. */
private const val AUTHORITY_DELIMITERS = "/?#"

/** Markers of a value substituted at runtime, e.g. "http://$HOST/". Such a host cannot be judged. */
private const val PLACEHOLDER_MARKERS = "\${}"

/**
 * Functions that inspect or rewrite the textual form of a string. A URL handed to one of them is a
 * pattern, not a connection target, so it must not raise an issue. This is the AST context that
 * [CleartextProtocolFilter] documents as being out of its reach. Matched by resolved name, so that
 * same-named functions on unrelated types (e.g. `java.nio.file.Path.startsWith`) keep reporting.
 */
private val STRING_PATTERN_FUNCTIONS = setOf(
    "startsWith", "endsWith", "removePrefix", "removeSuffix",
    "replace", "contains", "split", "substringAfter", "substringBefore",
).mapTo(HashSet()) { "kotlin.text.$it" }

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

    override fun visitStringTemplateExpression(expression: KtStringTemplateExpression, context: KotlinFileContext) {
        // A clear-text URL in test code is a fixture, not a connection an attacker can observe.
        if (context.inputFileContext.isTestFile) return
        val url = expression.constantValue() ?: return
        val scheme = cleartextSchemeOf(url) ?: return
        if (CleartextProtocolFilter.isSafeWithoutTls(url) || expression.isStringPatternArgument()) return
        CleartextProtocolFilter.getIssueMessage(scheme).ifPresent { context.reportIssue(expression, it) }
    }

    private fun analyzeOkHttpCall(kotlinFileContext: KotlinFileContext, callExpr: KtCallExpression) =
        OkHttpArgumentFinder { arg ->
            kotlinFileContext.reportIssue(arg, msg("http"))
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

/**
 * The value of this template when it is known statically, i.e. when every entry is plain text or an
 * escape sequence, or null when an interpolated entry makes the value depend on runtime state.
 */
private fun KtStringTemplateExpression.constantValue(): String? {
    val value = StringBuilder()
    for (entry in entries) {
        when (entry) {
            is KtLiteralStringTemplateEntry -> value.append(entry.text)
            is KtEscapeStringTemplateEntry -> value.append(entry.unescapedValue)
            else -> return null
        }
    }
    return value.toString()
}

/**
 * The clear-text scheme [url] starts with, without the [SCHEME_SEPARATOR], or null when [url] is not a
 * clear-text URL naming a host we can judge. URLs without an authority (e.g. the bare `"http://"` prefix
 * constant) name no endpoint, and a templated authority names one we cannot resolve; neither is reported.
 */
private fun cleartextSchemeOf(url: String): String? {
    val prefix = CleartextProtocolFilter.getCleartextProtocols().firstOrNull { url.startsWith(it, ignoreCase = true) } ?: return null
    val authority = url.drop(prefix.length).takeWhile { it !in AUTHORITY_DELIMITERS }
    if (authority.isEmpty() || authority.any { it in PLACEHOLDER_MARKERS }) return null
    return prefix.dropLast(SCHEME_SEPARATOR.length)
}

private fun KtStringTemplateExpression.isStringPatternArgument(): Boolean {
    val call = (parent as? KtValueArgument)?.parent?.parent as? KtCallExpression ?: return false
    return withKaSession {
        call.resolveToCall()?.successfulFunctionCallOrNull()?.symbol?.callableId?.asSingleFqName()?.asString()
    } in STRING_PATTERN_FUNCTIONS
}
