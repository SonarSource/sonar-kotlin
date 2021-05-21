package org.sonarsource.kotlin.checks

import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContext.REFERENCE_TARGET
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.api.ConstructorMatcher
import org.sonarsource.kotlin.api.FunMatcher
import org.sonarsource.kotlin.plugin.KotlinFileContext
import org.sonarsource.kotlin.visiting.KtTreeVisitor

private const val CLEARTEXT_FQN = "okhttp3.ConnectionSpec.Companion.CLEARTEXT"

@Rule(key = "S5332")
class ClearTextProtocolCheck : AbstractCheck() {

    companion object {
        private val UNSAFE_CALLS_GENERAL = listOf(
            ConstructorMatcher("org.apache.commons.net.ftp.FTPClient") to msg("FTP", "SFTP, SCP or FTPS"),
            ConstructorMatcher("org.apache.commons.net.smtp.SMTPClient")
                to msg("clear-text SMTP", "SMTP over SSL/TLS or SMTP with STARTTLS"),
            ConstructorMatcher("org.apache.commons.net.telnet.TelnetClient") to msg("Telnet", "SSH"),
        )

        private val UNSAFE_CALLS_OK_HTTP = listOf(
            ConstructorMatcher("okhttp3.ConnectionSpec.Builder"),
            FunMatcher(qualifier = "okhttp3.OkHttpClient.Builder", name = "connectionSpecs")
        )

        private fun msg(insecure: String, replaceWith: String) = "Using $insecure is insecure. Use $replaceWith instead."
    }

    override fun visitCallExpression(callExpr: KtCallExpression, kotlinFileContext: KotlinFileContext) {
        val (_, _, bindingContext) = kotlinFileContext

        UNSAFE_CALLS_GENERAL
            .firstOrNull { (matcher, _) -> matcher.matches(callExpr, bindingContext) }
            ?.let { (_, msg) ->
                kotlinFileContext.reportIssue(callExpr, msg)
                return
            }

        UNSAFE_CALLS_OK_HTTP
            .firstOrNull { it.matches(callExpr, bindingContext) }
            ?.run {
                analyzeOkHttpCall(kotlinFileContext, callExpr)
                return
            }
    }

    private fun analyzeOkHttpCall(kotlinFileContext: KotlinFileContext, callExpr: KtCallExpression) =
        OkHttpArgumentFinder(kotlinFileContext.bindingContext) { arg ->
            kotlinFileContext.reportIssue(arg, msg("HTTP", "HTTPS"))
        }.visitTree(callExpr)
}

private class OkHttpArgumentFinder(
    private val bindingContext: BindingContext,
    private val issueReporter: (KtSimpleNameExpression) -> Unit,
) : KtTreeVisitor() {
    override fun visitSimpleNameExpression(expression: KtSimpleNameExpression, data: Unit?) {
        if (bindingContext.get(REFERENCE_TARGET, expression)?.fqNameOrNull()?.asString() == CLEARTEXT_FQN) issueReporter(expression)
    }
}
