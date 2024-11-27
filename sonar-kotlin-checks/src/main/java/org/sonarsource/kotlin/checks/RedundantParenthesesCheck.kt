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

import org.jetbrains.kotlin.psi.KtParenthesizedExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

@Rule(key = "S1110")
class RedundantParenthesesCheck : AbstractCheck() {

    override fun visitParenthesizedExpression(expression: KtParenthesizedExpression, context: KotlinFileContext) {
        if (expression.parent is KtParenthesizedExpression) {
            val leftParenthesis = expression.firstChild
            val rightParenthesis = expression.lastChild
            context.reportIssue(
                leftParenthesis,
                "Remove these useless parentheses.",
                secondaryLocations = context.locationListOf(rightParenthesis to ""),
            )
        }
    }

}
