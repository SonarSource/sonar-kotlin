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

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtCallExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.checks.DEFERRED_FQN
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

@Rule(key = "S6315")
class UnusedDeferredResultCheck : AbstractCheck() {

    override fun visitCallExpression(expression: KtCallExpression, context: KotlinFileContext) {
        analyze(expression) {
            // FIXME avoid repeated construction of ClassId
            if (expression.expressionType!!.isClassType(ClassId.fromString(DEFERRED_FQN.replace('.', '/')))
                && !expression.isUsedAsExpression) {
                context.reportIssue(expression.calleeExpression!!, """This function returns "Deferred", but its result is never used.""")
            }
        }
    }
}
