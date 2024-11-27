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
package org.sonarsource.kotlin.gradle.checks

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.calls.util.getCalleeExpressionIfAny
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.reporting.KotlinTextRanges.textRange
import org.sonarsource.kotlin.api.reporting.message

private const val REF_NAME_GROUP = "group"
private const val REF_NAME_DESCRIPTION = "description"

@Rule(key = "S6626")
class TaskDefinitionsCheck : AbstractCheck() {

    // TODO: Improve this rule once semantics Gradle DSL semantics is available in the BindingContext:
    //       Use FunMatcher with receiver type `org.gradle.api.tasks.TaskContainer` instead of checking for receiver name `tasks`.

    override fun visitCallExpression(expression: KtCallExpression, kotlinFileContext: KotlinFileContext) {
        if (!isTasksRegisterCall(expression)) return
        val lambdaArg = getLastArgumentLambdaOrNull(expression) ?: return
        val block = lambdaArg.getLambdaExpression()?.bodyExpression ?: return

        val assignmentChecker = AssignmentChecker()
        block.acceptChildren(assignmentChecker)

        with(assignmentChecker) {
            if (hasGroup && hasDescription) return
            val highlightEndElement = expression.valueArgumentList ?: expression.getCalleeExpressionIfAny()!!
            kotlinFileContext.reportIssue(
                kotlinFileContext.textRange(expression.parent.startOffset, highlightEndElement.endOffset),
                message {
                    +"Define "
                    if (!hasGroup) code(REF_NAME_GROUP)
                    if (!(hasGroup || hasDescription)) +" and "
                    if (!hasDescription) code(REF_NAME_DESCRIPTION)
                    +" for this task"
                }
            )
        }
    }

    private fun isTasksRegisterCall(expression: KtCallExpression): Boolean {
        val functionName = (expression.getCalleeExpressionIfAny() as? KtNameReferenceExpression)?.getReferencedName()
        if (functionName != "register" && functionName != "create") return false
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
