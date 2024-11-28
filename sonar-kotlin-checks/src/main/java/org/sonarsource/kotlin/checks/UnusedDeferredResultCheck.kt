/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2024 SonarSource SA
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

import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsStatement
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.checks.DEFERRED_FQN
import org.sonarsource.kotlin.api.checks.expressionTypeFqn
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

@org.sonarsource.kotlin.api.frontend.K1only
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
