package org.sonarsource.kotlin.checks

import org.jetbrains.kotlin.psi.KtCallExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.api.FunMatcher
import org.sonarsource.kotlin.plugin.KotlinFileContext

private const val MESSAGE = "Make sure using unencrypted files is safe here."
private val MATCHER = FunMatcher(qualifier = "kotlin.io") { withNames("writeText", "appendBytes") }

@Rule(key = "S6300")
class UnencryptedFilesInMobileApplicationsCheck : AbstractCheck() {

    override fun visitCallExpression(callExpression: KtCallExpression, kotlinFileContext: KotlinFileContext) {
        if (kotlinFileContext.isInAndroid() && MATCHER.matches(callExpression, kotlinFileContext.bindingContext)) {
            kotlinFileContext.reportIssue(callExpression.calleeExpression!!, MESSAGE)
        }
    } 
} 
