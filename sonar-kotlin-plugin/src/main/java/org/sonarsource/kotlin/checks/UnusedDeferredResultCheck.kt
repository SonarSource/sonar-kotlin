package org.sonarsource.kotlin.checks

import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsStatement
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.api.DEFERRED_FQN
import org.sonarsource.kotlin.api.expressionTypeFqn
import org.sonarsource.kotlin.plugin.KotlinFileContext

@Rule(key = "S6315")
class UnusedDeferredResultCheck : AbstractCheck() {

    override fun visitCallExpression(expression: KtCallExpression, context: KotlinFileContext) {
        val bindingContext = context.bindingContext
        if (expression.expressionTypeFqn(bindingContext) == DEFERRED_FQN
            && expression.isUsedAsStatement(bindingContext)
        ) {
            context.reportIssue(expression.calleeExpression!!, """This function returns "Deferred", but its result is never used.""")
            return
        }
    }
}
