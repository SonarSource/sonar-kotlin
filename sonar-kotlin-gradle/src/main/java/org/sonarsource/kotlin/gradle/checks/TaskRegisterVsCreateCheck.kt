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
package org.sonarsource.kotlin.gradle.checks

import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.referenceExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.reporting.message

private val MESSAGE = message {
    +"Use "
    code("tasks.register(...)")
    +" instead."
}

@Rule(key = "S6623")
class TaskRegisterVsCreateCheck : AbstractCheck() {
    /* TOOD:

    This rule would benefit from using semantics. Instead of manually matching the call to "create", we can use `CallAbstractCheck` and a
    function matcher. The function matcher could be instantiated with information such as:
        qualifier = "org.gradle.api.NamedDomainObjectContainer"
        name = "create"
        this.returnType = "org.gradle.api.Task"

    At the time of implementing this rule, we do not have semantic access in Gradle Kotlin DSL rules yet. This rule should be updated once
    we do.

     */

    override fun visitCallExpression(callExpression: KtCallExpression, kotlinFileContext: KotlinFileContext) {
        // We only look for function calls that look like "tasks.create"
        val calleeExpression = (callExpression.referenceExpression() as? KtNameReferenceExpression) ?: return
        val receiverName = ((callExpression.parent as? KtDotQualifiedExpression)?.receiverExpression as? KtNameReferenceExpression)
            ?.getReferencedName() ?: return


        if (receiverName == "tasks" && calleeExpression.getReferencedName() == "create") {
            kotlinFileContext.reportIssue(calleeExpression, MESSAGE)
        }
    }
}
