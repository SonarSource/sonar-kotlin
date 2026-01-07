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
package org.sonarsource.kotlin.gradle.checks

import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.resolve.calls.util.getCalleeExpressionIfAny
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.reporting.KotlinTextRanges.textRange
import org.sonarsource.kotlin.api.reporting.SecondaryLocation

private const val REF_NAME_CONSTRAINTS = "constraints"

@Rule(key = "S6629")
class DependencyGroupedCheck : AbstractCheck() {

    override fun visitCallExpression(expression: KtCallExpression, kotlinFileContext: KotlinFileContext) {
        if (getFunctionName(expression) != REF_NAME_DEPENDENCIES) return
        checkDependencyHandlerScopeLambda(expression, kotlinFileContext)
    }

    private fun checkDependencyHandlerScopeLambda(expression: KtCallExpression, kotlinFileContext: KotlinFileContext) {
        val visitor = DependencyHandlerScopeLambdaCheck(kotlinFileContext)
        getLambdaBlock(expression)?.acceptChildren(visitor)

        if (visitor.secondaryLocations.isNotEmpty()) {
            kotlinFileContext.reportIssue(
                expression.getCalleeExpressionIfAny()!!,
                "Group dependencies by their destination.",
                visitor.secondaryLocations
            )
        }
    }

    private inner class DependencyHandlerScopeLambdaCheck(
        private val kotlinFileContext: KotlinFileContext
    ) : KtVisitorVoid() {

        private var lastNameInOrder: String? = null

        private val visitedNames = mutableSetOf<String>()

        val secondaryLocations = mutableListOf<SecondaryLocation>()

        override fun visitCallExpression(expression: KtCallExpression) {
            val functionReference = expression.getCalleeExpressionIfAny() as? KtNameReferenceExpression ?: return
            val functionName = functionReference.getReferencedName()

            if (functionName == REF_NAME_CONSTRAINTS) {
                checkDependencyHandlerScopeLambda(expression, kotlinFileContext)
            } else if (REF_NAME_DEPENDENCY_HANDLER_SCOPE_EXTENSIONS.contains(functionName)) {
                checkFunctionReferenceOrder(functionReference)
            }
        }

        fun checkFunctionReferenceOrder(functionReference: KtNameReferenceExpression) {
            val functionName = functionReference.getReferencedName()
            if (lastNameInOrder != null && functionName != lastNameInOrder && visitedNames.contains(functionName)) {
                secondaryLocations.add(SecondaryLocation(kotlinFileContext.textRange(functionReference)))
            } else {
                lastNameInOrder = functionName
            }
            visitedNames.add(functionName)
        }
    }
}
