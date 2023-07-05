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
package org.sonarsource.kotlin.gradle.checks

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.resolve.calls.util.getCalleeExpressionIfAny
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

private const val REF_NAME_GROUP = "group"
private const val REF_NAME_DESCRIPTION = "description"

@Rule(key = "S6626")
class TaskDefinitionsCheck : AbstractCheck() {

    override fun visitCallExpression(expression: KtCallExpression, kotlinFileContext: KotlinFileContext) {
        if (!isTasksRegisterCall(expression)) return
        val lambdaArg = getLastArgumentLambdaOrNull(expression) ?: return
        val block = lambdaArg.getLambdaExpression()?.bodyExpression ?: return

        val assignmentChecker = AssignmentChecker()
        block.acceptChildren(assignmentChecker)
        if (assignmentChecker.hasGroup && assignmentChecker.hasDescription) return

        val missing = listOf(
            if (assignmentChecker.hasGroup) null else "\"$REF_NAME_GROUP\"",
            if (assignmentChecker.hasDescription) null else "\"$REF_NAME_DESCRIPTION\"",
        ).filterNotNull().joinToString(" and ")

        kotlinFileContext.reportIssue(
            expression.parent,
            """Define $missing for this task"""
        )
    }

    private fun isTasksRegisterCall(expression: KtCallExpression): Boolean {
        if ((expression.getCalleeExpressionIfAny() as? KtNameReferenceExpression)?.getReferencedName() != "register") return false
        val parent = expression.parent as? KtDotQualifiedExpression ?: return false
        return (parent.receiverExpression as? KtNameReferenceExpression)?.getReferencedName() == "tasks"
    }

    private fun getLastArgumentLambdaOrNull(expression: KtCallExpression) =
        expression.valueArguments.lastOrNull() as? KtLambdaArgument
}

private class AssignmentChecker : KtVisitorVoid() {

    var hasGroup = false
    var hasDescription = false

    override fun visitBinaryExpression(expression: KtBinaryExpression) {
        if (hasGroup && hasDescription) return
        if (expression.operationToken != KtTokens.EQ) return

        val assigneeReference = expression.left as? KtNameReferenceExpression ?: return
        when (assigneeReference.getReferencedName()) {
            REF_NAME_GROUP -> hasGroup = true
            REF_NAME_DESCRIPTION -> hasDescription = true
        }
    }
}
