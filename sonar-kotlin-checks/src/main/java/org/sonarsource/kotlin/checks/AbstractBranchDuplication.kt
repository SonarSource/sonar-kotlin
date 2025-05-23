/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2025 SonarSource SA
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

import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.jetbrains.kotlin.psi.KtContainerNodeForControlStructureBody

abstract class AbstractBranchDuplication : AbstractCheck() {
    protected abstract fun checkDuplicatedBranches(ctx: KotlinFileContext, tree: KtElement, branches: List<KtElement>)
    protected abstract fun onAllIdenticalBranches(ctx: KotlinFileContext, tree: KtElement)

    override fun visitIfExpression(expression: KtIfExpression, context: KotlinFileContext) {
        /**
         * Parent of `else if` is [KtContainerNodeForControlStructureBody]
         * whose parent is [KtIfExpression]
         */
        val parent = expression.parent.parent
        if (parent !is KtIfExpression || expression == parent.then) {
            checkConditionalStructure(context, expression, ConditionalStructure(expression))
        }
    }

    override fun visitWhenExpression(expression: KtWhenExpression, context: KotlinFileContext) {
        checkConditionalStructure(context, expression, ConditionalStructure(expression))
    }

    private fun checkConditionalStructure(ctx: KotlinFileContext, tree: KtElement, conditional: ConditionalStructure) {
        if (conditional.allBranchesArePresent && !conditional.hasEmptyThen && conditional.allBranchesAreIdentical()) {
            onAllIdenticalBranches(ctx, tree)
        } else {
            checkDuplicatedBranches(ctx, tree, conditional.branches)
        }
    }
}

private class ConditionalStructure {
    var allBranchesArePresent = false
    var hasEmptyThen = false
    val branches: MutableList<KtElement> = mutableListOf()

    constructor(ifTree: KtIfExpression) {
        if (ifTree.then != null) {
            branches.add(ifTree.then!!)
        } else {
            hasEmptyThen = true
        }
        var elseBranch = ifTree.`else`
        while (elseBranch != null) {
            if (elseBranch is KtIfExpression) {
                val elseIf = elseBranch
                if (elseIf.then != null) {
                    branches.add(elseIf.then!!)
                } else {
                    hasEmptyThen = true
                }
                elseBranch = elseIf.`else`
            } else {
                branches.add(elseBranch)
                allBranchesArePresent = true
                elseBranch = null
            }
        }
    }

    constructor(tree: KtWhenExpression) {
        for (caseTree in tree.entries) {
            caseTree.expression!!.let { branches.add(it) }
            if (caseTree.isElse) {
                allBranchesArePresent = true
            }
        }
    }

    fun allBranchesAreIdentical(): Boolean {
        val firstBranch = branches[0]
        return branches.asSequence()
            .drop(1)
            .all { branch -> SyntacticEquivalence.areEquivalent(firstBranch, branch) }
    }
}
