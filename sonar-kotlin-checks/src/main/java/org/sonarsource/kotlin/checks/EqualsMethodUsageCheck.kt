/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2024 SonarSource SA
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
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPrefixExpression
import org.jetbrains.kotlin.psi.KtSuperExpression
import org.jetbrains.kotlin.psi.KtThisExpression
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.ANY_TYPE
import org.sonarsource.kotlin.api.checks.CallAbstractCheck
import org.sonarsource.kotlin.api.checks.EQUALS_METHOD_NAME
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.reporting.SecondaryLocation
import org.sonarsource.kotlin.api.reporting.KotlinTextRanges.textRange
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

@Rule(key = "S6519")
class EqualsMethodUsageCheck : CallAbstractCheck() {
    override val functionsToVisit = setOf(FunMatcher { name = EQUALS_METHOD_NAME; withArguments(ANY_TYPE) })

    // TODO easy
    override fun visitFunctionCall(callExpression: KtCallExpression, resolvedCall: ResolvedCall<*>, kotlinFileContext: KotlinFileContext) {
        val parent = callExpression.parent
        if (parent is KtDotQualifiedExpression && parent.selectorExpression == callExpression && !parent.receiverExpression.isSuperOrOuterClass()) {
            val grandParent = parent.parent.skipParentParentheses()
            val callee = callExpression.calleeExpression!! // as this function was matched .calleeExpression can't be null
            if (grandParent is KtPrefixExpression && grandParent.operationToken == KtTokens.EXCL) {
                val secondaryLocations = listOf(SecondaryLocation(kotlinFileContext.textRange(grandParent.operationReference), "Negation"))
                kotlinFileContext.reportIssue(callee, """Replace "!" and "equals" with binary operator "!=".""", secondaryLocations)
            } else {
                kotlinFileContext.reportIssue(callee, """Replace "equals" with binary operator "==".""")
            }
        }
    }
}

private fun KtExpression.isSuperOrOuterClass() = this is KtSuperExpression ||
    (this is KtThisExpression && this.getTargetLabel() != null)
