package org.sonarsource.kotlin.gradle.checks

import org.jetbrains.kotlin.psi.KtCallExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

private const val disableVerificationMetadata = "disableDependencyVerification"
private const val message = """Remove this call to "disableDependencyVerification()" so that dependencies are verified."""

// The part of the rule looking for missing verification-metadata.xml is implemented in the KotlinGradleSensor
@Rule(key = "S6474")
class MissingVerificationMetadataCheck : AbstractCheck() {
    override fun visitCallExpression(expression: KtCallExpression, kotlinFileContext: KotlinFileContext) {
        if (getFunctionName(expression) == disableVerificationMetadata) {
            kotlinFileContext.reportIssue(expression, message)
        }
    }
}

