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
package org.sonarsource.kotlin.checks

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtPrefixExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.ANY_TYPE
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.api.EQUALS_METHOD_NAME
import org.sonarsource.kotlin.api.FunMatcher
import org.sonarsource.kotlin.api.SecondaryLocation
import org.sonarsource.kotlin.converter.KotlinTextRanges.textRange
import org.sonarsource.kotlin.plugin.KotlinFileContext

private val EQUALS_MATCHER = FunMatcher {
    name = EQUALS_METHOD_NAME
    withArguments(ANY_TYPE)
}

@Rule(key = "S6519")
class EqualsMethodUsageCheck : AbstractCheck() {
    override fun visitCallExpression(expression: KtCallExpression, ctx: KotlinFileContext) {
        val bindingContext = ctx.bindingContext
        val parent = expression.parent
        if (EQUALS_MATCHER.matches(expression, bindingContext) && parent is KtDotQualifiedExpression && parent.selectorExpression == expression) {
            val grandParent = parent.parent
            if (grandParent is KtPrefixExpression && grandParent.operationToken == KtTokens.EXCL) {
                val secondaryLocations =  listOf(SecondaryLocation(ctx.textRange(grandParent.operationReference), "Negation"))
                ctx.reportIssue(expression.calleeExpression!!, "Replace \"!\" and \"equals\" with binary operator \"!=\".", secondaryLocations)
            } else {
                ctx.reportIssue(expression.calleeExpression!!, "Replace \"equals\" with binary operator \"==\".")
            }
        }
        super.visitCallExpression(expression, ctx)
    }
}
