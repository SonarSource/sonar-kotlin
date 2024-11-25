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

import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtBreakExpression
import org.jetbrains.kotlin.psi.KtContinueExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtThrowExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

@Rule(key = "S1763")
class CodeAfterJumpCheck : AbstractCheck() {

    override fun visitBlockExpression(expression: KtBlockExpression, context: KotlinFileContext) {
        checkStatements(context, expression.statements)
    }

    private fun checkStatements(context: KotlinFileContext, statementsOrExpressions: List<KtElement>) {
        for (i in 0 until statementsOrExpressions.size - 1) {
            val current = statementsOrExpressions[i]
            getJump(current)?.let {
                context.reportIssue(current, """Refactor this piece of code to not have any dead code after this "$it".""")
            }
        }
    }
}

private fun getJump(tree: KtElement) =
    when (tree) {
        is KtBreakExpression -> "break"
        is KtContinueExpression -> "continue"
        is KtReturnExpression -> "return"
        is KtThrowExpression -> "throw"
        else -> null
    }
