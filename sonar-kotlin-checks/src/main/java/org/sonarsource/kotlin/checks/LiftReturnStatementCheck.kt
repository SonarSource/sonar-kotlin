/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2026 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1.0.1, as published by SonarSource Sàrl.
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

import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.checks.isExhaustive
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

@Rule(key = "S6510")
class LiftReturnStatementCheck : AbstractCheck() {

    override fun visitIfExpression(expression: KtIfExpression, context: KotlinFileContext) {
        val thenBranch = expression.then ?: return
        val elseBranch = expression.`else`?: return

        if (isReturnOrReturnBlock(thenBranch) && isReturnOrReturnBlock(elseBranch)) {
            reportIssue(expression.ifKeyword, context)
        }
    }

    override fun visitWhenExpression(expression: KtWhenExpression, context: KotlinFileContext) {
        val isAllBranchesReturn = expression.entries.all {
            // There is no known case when KtWhenEntry.expression could be `null`.
            isReturnOrReturnBlock(it.expression!!)
        }

        if (isAllBranchesReturn && expression.isExhaustive()) {
            reportIssue(expression.whenKeyword, context)
        }
    }

    private fun reportIssue(keywordElement: PsiElement, context: KotlinFileContext) {
        val keyword = (keywordElement as LeafPsiElement).chars
        context.reportIssue(keywordElement, """Move "return" statements from all branches before "$keyword" statement.""")
    }
}

private fun isReturnOrReturnBlock(element: PsiElement) =
    when (element) {
        is KtReturnExpression -> true
        is KtBlockExpression -> element.statements.lastOrNull() is KtReturnExpression
        else -> false
    }
