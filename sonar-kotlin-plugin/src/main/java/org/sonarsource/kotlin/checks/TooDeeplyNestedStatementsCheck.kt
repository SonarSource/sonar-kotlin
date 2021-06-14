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

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtContainerNodeForControlStructureBody
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtLoopExpression
import org.jetbrains.kotlin.psi.KtTryExpression
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.sonar.check.Rule
import org.sonar.check.RuleProperty
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.plugin.KotlinFileContext
import org.sonarsource.slang.checks.api.SecondaryLocation

/**
 * Replacement for [org.sonarsource.slang.checks.TooDeeplyNestedStatementsCheck]
 */
@Rule(key = "S134")
class TooDeeplyNestedStatementsCheck : AbstractCheck() {
    companion object {
        const val DEFAULT_MAX_DEPTH = 3
    }

    @RuleProperty(
        key = "max",
        description = "Maximum allowed control flow statement nesting depth",
        defaultValue = "" + DEFAULT_MAX_DEPTH)
    var max: Int = DEFAULT_MAX_DEPTH

    override fun visitIfExpression(expression: KtIfExpression, kotlinFileContext: KotlinFileContext) {
        check(expression, kotlinFileContext)
    }

    override fun visitLoopExpression(loopExpression: KtLoopExpression, kotlinFileContext: KotlinFileContext) {
        check(loopExpression, kotlinFileContext)
    }

    override fun visitWhenExpression(expression: KtWhenExpression, kotlinFileContext: KotlinFileContext) {
        check(expression, kotlinFileContext)
    }

    override fun visitTryExpression(expression: KtTryExpression, kotlinFileContext: KotlinFileContext) {
        check(expression, kotlinFileContext)
    }

    private fun check(expression: KtExpression, kotlinFileContext: KotlinFileContext) {
        if (isElseIfStatement(expression.parent, expression)) {
            // Ignore 'else-if' statements since the issue would already be raised on the first 'if' statement
            return
        }
        if (isTernaryOperator(expression)) {
            return
        }

        val iterator = expression.parents.iterator()
        val nestedParentNodes = mutableListOf<KtExpression>()
        var last: PsiElement = expression
        while (iterator.hasNext()) {
            val parent = iterator.next()
            if (isElseIfStatement(parent, last) && nestedParentNodes.isNotEmpty()) {
                // Only the 'if' parent of the chained 'else-if' statements should be highlighted
                nestedParentNodes.removeLast()
            }
            if (parent is KtLoopExpression || parent is KtTryExpression || parent is KtIfExpression || parent is KtWhenExpression) {
                nestedParentNodes.add(parent as KtExpression)
            }
            if (nestedParentNodes.size > max) {
                return
            }
            last = parent
        }

        if (nestedParentNodes.size == max) {
            kotlinFileContext.reportIssue(
                nodeToHighlight(expression),
                "Refactor this code to not nest more than $max control flow statements.",
                secondaryLocations = nestedParentNodes.mapIndexed { i, psiElement ->
                    SecondaryLocation(kotlinFileContext.textRange(nodeToHighlight(psiElement)), "Nesting depth $i")
                },
            )
        }
    }

    private fun nodeToHighlight(expression: KtExpression): PsiElement =
        when (expression) {
            is KtLoopExpression -> expression.firstChild
            is KtTryExpression -> expression.tryKeyword!!
            is KtIfExpression -> expression.ifKeyword
            is KtWhenExpression -> expression.whenKeyword
            else -> expression
        }

    private fun isTernaryOperator(tree: KtExpression): Boolean {
        /** see [org.sonarsource.slang.checks.utils.ExpressionUtils.isTernaryOperator] */
        if (tree !is KtIfExpression || tree.`else` == null)
            return false
        return tree.then !is KtBlockExpression
            && tree.`else` !is KtBlockExpression
            && tree.`else` !is KtIfExpression
    }

    private fun isElseIfStatement(parent: PsiElement, tree: PsiElement): Boolean {
        /**
         * Parent of `else if` is [KtContainerNodeForControlStructureBody]
         * whose parent is [KtIfExpression]
         */
        val p = parent.parent
        return tree is KtIfExpression
            && p is KtIfExpression
            && tree == p.`else`
    }

}
