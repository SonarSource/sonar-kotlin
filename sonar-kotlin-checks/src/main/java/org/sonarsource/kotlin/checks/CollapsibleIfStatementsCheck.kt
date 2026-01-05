/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2026 SonarSource SA
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

import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtIfExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

@Rule(key = "S1066")
class CollapsibleIfStatementsCheck : AbstractCheck() {

    override fun visitIfExpression(expression: KtIfExpression, kotlinFileContext: KotlinFileContext) {
        if (expression.elseKeyword != null) return
        // It is safe to use ".then!!" here as we filtered out the cases with "else"
        val collapsibleIfStatement = getCollapsibleIfStatement(expression.then!!)
        if (collapsibleIfStatement != null) {
            kotlinFileContext.reportIssue(
                expression.ifKeyword,
                """Merge this "if" statement with the nested one.""",
                secondaryLocations = kotlinFileContext.locationListOf(collapsibleIfStatement.ifKeyword to ""),
            )
        }
    }

    private fun getCollapsibleIfStatement(expression: KtExpression): KtIfExpression? =
        if (expression is KtBlockExpression)
            if (expression.statements.size == 1)
                getIfStatementWithoutElse(expression.firstStatement)
            else
                null
        else
            getIfStatementWithoutElse(expression)

    private fun getIfStatementWithoutElse(expression: KtExpression?): KtIfExpression? =
        if (expression is KtIfExpression && expression.elseKeyword == null)
            expression
        else
            null

}
