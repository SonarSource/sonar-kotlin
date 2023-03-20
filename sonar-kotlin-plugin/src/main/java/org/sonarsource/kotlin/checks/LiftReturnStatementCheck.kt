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

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.plugin.KotlinFileContext

@Rule(key = "S6510")
class LiftReturnStatementCheck : AbstractCheck() {

    override fun visitIfExpression(expression: KtIfExpression, context: KotlinFileContext) {
        val thenBranch = expression.then
        val elseBranch = expression.`else`
        val isAllBranchesReturn = thenBranch != null &&
            elseBranch != null &&
            isReturnOrReturnBlock(thenBranch) &&
            isReturnOrReturnBlock(elseBranch)

        if (isAllBranchesReturn) {
            context.reportIssue(expression.ifKeyword, """Lift "return" statements from both branches before "if" statement.""")
        }
    }

    override fun visitWhenExpression(expression: KtWhenExpression, context: KotlinFileContext) {
        val isAllBranchesReturn = expression.entries.all {
            // Note: there is no known case when KtWhenEntry.expression could be `null`.
            isReturnOrReturnBlock(it.expression!!)
        }

        if (isAllBranchesReturn && isExhaustive(expression, context)) {
            context.reportIssue(expression.whenKeyword, """Lift "return" statements from all branches before "when" statement.""")
        }
    }

    private fun isReturnOrReturnBlock(element: PsiElement): Boolean {
        return when (element) {
            is KtReturnExpression -> true
            is KtBlockExpression -> element.statements.lastOrNull() is KtReturnExpression
            else -> false
        }
    }

    private fun isExhaustive(expression: KtWhenExpression, context: KotlinFileContext): Boolean {
        return expression.entries.any { it.isElse } ||
            context.bindingContext.get(BindingContext.IMPLICIT_EXHAUSTIVE_WHEN, expression) == true ||
            context.bindingContext.get(BindingContext.EXHAUSTIVE_WHEN, expression) == true
    }
}
