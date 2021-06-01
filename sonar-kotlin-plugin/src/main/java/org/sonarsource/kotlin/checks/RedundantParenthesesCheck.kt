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

import org.jetbrains.kotlin.psi.KtParenthesizedExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.plugin.KotlinFileContext

/**
 * Replacement for [org.sonarsource.slang.checks.RedundantParenthesesCheck]
 */
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
