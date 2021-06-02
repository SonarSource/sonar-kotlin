/*
 * SonarSource SLang
 * Copyright (C) 2018-2021 SonarSource SA
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

import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtBreakExpression
import org.jetbrains.kotlin.psi.KtContinueExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtThrowExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.plugin.KotlinFileContext

/**
 * Replacement for [org.sonarsource.slang.checks.CodeAfterJumpCheck]
 */
@Rule(key = "S1763")
class CodeAfterJumpCheck : AbstractCheck() {

    override fun visitBlockExpression(expression: KtBlockExpression, context: KotlinFileContext) {
        checkStatements(context, expression.statements)
    }

    private fun checkStatements(context: KotlinFileContext, statementsOrExpressions: List<KtElement>) {
        for (i in 0 until statementsOrExpressions.size - 1) {
            val current = statementsOrExpressions[i]
            getJump(current)?.let {
                context.reportIssue(current,
                    "Refactor this piece of code to not have any dead code after this \"$it\".")
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
