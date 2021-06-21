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

import org.jetbrains.kotlin.psi.KtBreakExpression
import org.jetbrains.kotlin.psi.KtContainerNodeForControlStructureBody
import org.jetbrains.kotlin.psi.KtContinueExpression
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtThrowExpression
import org.jetbrains.kotlin.psi.psiUtil.lastBlockStatementOrThis
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.plugin.KotlinFileContext

/**
 * Replacement for [org.sonarsource.slang.checks.ElseIfWithoutElseCheck]
 */
@Rule(key = "S126")
class ElseIfWithoutElseCheck : AbstractCheck() {

    override fun visitIfExpression(ifExpression: KtIfExpression, kotlinFileContext: KotlinFileContext) {
        if (ifExpression.`else` == null || !ifExpression.isTopLevelIf()) {
            return
        }

        var lastIfExpression = ifExpression
        var allTerminate = lastIfExpression.terminates()
        while (lastIfExpression.`else` is KtIfExpression) {
            lastIfExpression = lastIfExpression.`else` as KtIfExpression
            allTerminate = allTerminate && lastIfExpression.terminates()
        }

        // We raise an issue if
        //   - at least one branch does not finish with return/break/throw
        //   - no "else" is defined
        if (!allTerminate && lastIfExpression.`else` == null) {
            kotlinFileContext.reportIssue(lastIfExpression.ifKeyword, "Add the missing \"else\" clause.")
        }
    }

    private fun KtIfExpression.isTopLevelIf(): Boolean {
        /**
         * Parent of `else if` is [KtContainerNodeForControlStructureBody]
         * whose parent is [KtIfExpression]
         */
        val parent = this.parent.parent
        return if (parent is KtIfExpression) {
            // if different from the else branch of parent,
            // it means that it is a statement inside parent and so the top level "if"
            parent.`else` != this
        } else true
    }

    private fun KtIfExpression.terminates(): Boolean =
        when (then!!.lastBlockStatementOrThis()) {
            is KtReturnExpression, is KtThrowExpression, is KtBreakExpression, is KtContinueExpression -> true
            else -> false
        }

}
