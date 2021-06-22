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
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtBreakExpression
import org.jetbrains.kotlin.psi.KtCatchClause
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtContinueExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtLoopExpression
import org.jetbrains.kotlin.psi.KtOperationReferenceExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.psi.KtWhileExpression

/**
 * Replacement for [org.sonarsource.slang.checks.complexity.CognitiveComplexity]
 */
class CognitiveComplexity(val root: KtElement) {
    private val increments: MutableList<Increment> = ArrayList()

    init {
        val visitor = CognitiveComplexityVisitor()
        root.accept(visitor)
    }

    fun value(): Int {
        var total = 0
        for (increment in increments) {
            total += increment.nestingLevel + 1
        }
        return total
    }

    fun increments(): List<Increment> {
        return increments
    }

    class Increment(val token: PsiElement, val nestingLevel: Int)

    private inner class CognitiveComplexityVisitor : KtTreeVisitorVoid() {
        private val alreadyConsideredOperators: MutableSet<KtOperationReferenceExpression> = HashSet()

        override fun visitExpression(expression: KtExpression) {
            when (expression) {
                is KtForExpression -> incrementWithNesting(expression.forKeyword, expression)
                is KtWhileExpression -> incrementWithNesting(expression.firstChild, expression)
                is KtWhenExpression -> incrementWithNesting(expression.whenKeyword, expression)
                is KtBreakExpression -> expression.labelQualifier?.let { incrementWithoutNesting(expression.firstChild) }
                is KtContinueExpression -> expression.labelQualifier?.let { incrementWithoutNesting(expression.firstChild) }
                is KtIfExpression -> handleIfExpression(expression)
                is KtBinaryExpression -> handleBinaryExpressions(expression)
            }
            super.visitExpression(expression)
        }

        override fun visitCatchSection(catchClause: KtCatchClause) {
            incrementWithNesting(catchClause.firstChild, catchClause)
            super.visitCatchSection(catchClause)
        }

        private fun handleBinaryExpressions(tree: KtBinaryExpression) {
            if (!tree.isLogicalBinaryExpression() || alreadyConsideredOperators.contains(tree.operationReference)) {
                return
            }
            val operators: MutableList<KtOperationReferenceExpression> = ArrayList()
            flattenOperators(tree, operators)
            var previous: KtOperationReferenceExpression? = null
            for (operator in operators) {
                if (previous == null || previous.operationSignTokenType?.value != operator.operationSignTokenType?.value) {
                    incrementWithoutNesting(operator)
                }
                previous = operator
                alreadyConsideredOperators.add(operator)
            }
        }

        private fun flattenOperators(tree: KtBinaryExpression, operators: MutableList<KtOperationReferenceExpression>) {
            if (tree.left.isLogicalBinaryExpression()) {
                flattenOperators(tree.left as KtBinaryExpression, operators)
            }
            operators.add(tree.operationReference)
            if (tree.right.isLogicalBinaryExpression()) {
                flattenOperators(tree.right as KtBinaryExpression, operators)
            }
        }

        private fun incrementWithNesting(token: PsiElement, tree: KtElement) {
            increment(token, nestingLevel(tree))
        }

        private fun incrementWithoutNesting(token: PsiElement) {
            increment(token, 0)
        }

        private fun increment(token: PsiElement, nestingLevel: Int) {
            increments.add(Increment(token, nestingLevel))
        }

        private fun nestingLevel(ktElement: KtElement): Int {
            var nestingLevel = 0
            var parent: PsiElement? = ktElement.parent
            var isInsideFunction = root is KtFunction

            val ancestors = ArrayDeque<PsiElement>()
            while (parent != null && parent != root) {
                ancestors.addFirst(parent)
                parent = parent.parent
            }

           for (element in ancestors) {
               if (element is KtFunction) {
                   if (isInsideFunction) nestingLevel++
                   isInsideFunction = true
               } else if (element is KtIfExpression && !isElseIfBranch(element.parent?.parent, element) ||
                   element is KtWhenExpression || element is KtLoopExpression || element is KtCatchClause) {
                   nestingLevel++
               } else if (element is KtClass) {
                   nestingLevel = 0
                   isInsideFunction = false
               }
           }
            return nestingLevel
        }

        private fun isElseIfBranch(parent: PsiElement?, t: KtElement): Boolean {
            return parent is KtIfExpression && parent.`else` === t
        }

        private fun handleIfExpression(ifExpression: KtIfExpression) {
            val parent = ifExpression.parent
            val preParent = parent?.parent
            val isElseIf = preParent is KtIfExpression && ifExpression === preParent.`else`
            if (!isElseIf) {
                incrementWithNesting(ifExpression.ifKeyword, ifExpression)
            }
            ifExpression.`else`?.let {
                if (it is KtBlockExpression || it is KtIfExpression)
                    incrementWithoutNesting(ifExpression.elseKeyword!!)
            }
        }
    }

    private fun KtExpression?.isLogicalBinaryExpression(): Boolean =
        this is KtBinaryExpression && (operationToken == KtTokens.ANDAND || operationToken == KtTokens.OROR)
}
