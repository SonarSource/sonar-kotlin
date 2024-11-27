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

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtUnaryExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

@Rule(key = "S1125")
class BooleanLiteralCheck : AbstractCheck() {

    companion object {
        private val CONDITIONAL_BINARY_OPERATORS = setOf(KtTokens.ANDAND, KtTokens.OROR)
        private val BOOLEAN_LITERALS = setOf("true", "false")
        private const val MESSAGE = "Remove the unnecessary Boolean literal."

        private fun isIfWithMaxTwoBranches(ifTree: KtIfExpression): Boolean {
            /**
             * Parent of `else if` is [org.jetbrains.kotlin.psi.KtContainerNodeForControlStructureBody]
             * whose parent is [KtIfExpression]
             */
            val grandParent = ifTree.parent.parent
            val isElseIf = grandParent is KtIfExpression && grandParent.`else` === ifTree
            val isIfElseIf = ifTree.`else` is KtIfExpression
            return !isElseIf && !isIfElseIf
        }

        private fun hasBlockBranch(ifTree: KtIfExpression): Boolean {
            return ifTree.then is KtBlockExpression || ifTree.`else` is KtBlockExpression
        }
    }

    override fun visitIfExpression(expression: KtIfExpression, context: KotlinFileContext) {
        if (isIfWithMaxTwoBranches(expression) && !hasBlockBranch(expression)) {
            getBooleanLiteral(expression.then, expression.`else`)
                ?.let { context.reportIssue(it, MESSAGE) }
        }
    }

    override fun visitBinaryExpression(expression: KtBinaryExpression, context: KotlinFileContext) {
        if (CONDITIONAL_BINARY_OPERATORS.contains(expression.operationToken)) {
            getBooleanLiteral(expression.left, expression.right)
                ?.let { context.reportIssue(it, MESSAGE) }
        }
    }

    override fun visitUnaryExpression(expression: KtUnaryExpression, context: KotlinFileContext) {
        if (KtTokens.EXCL == expression.operationToken) {
            getBooleanLiteral(expression.baseExpression)
                ?.let { context.reportIssue(it, MESSAGE) }
        }
    }

    private fun getBooleanLiteral(vararg trees: KtExpression?) = trees.asSequence()
        .mapNotNull { it?.skipParentheses() }
        .find { it is KtConstantExpression && BOOLEAN_LITERALS.contains(it.text) }
}
