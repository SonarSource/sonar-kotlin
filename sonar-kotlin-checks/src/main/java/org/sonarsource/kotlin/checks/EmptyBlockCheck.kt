/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2025 SonarSource SA
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
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.psi.KtWhileExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

@Rule(key = "S108")
class EmptyBlockCheck : AbstractCheck() {

    private val message = "Either remove or fill this block of code."

    override fun visitWhenExpression(expression: KtWhenExpression, kotlinFileContext: KotlinFileContext) {
        if (expression.entries.isEmpty() && !expression.hasComment()) {
            kotlinFileContext.reportIssue(expression, message)
        }
    }

    override fun visitBlockExpression(expression: KtBlockExpression, kotlinFileContext: KotlinFileContext) {
        if (expression.statements.isEmpty()
            && !expression.hasComment()
            && expression.parent !is KtFunction
            /** Between [KtWhileExpression] and [KtBlockExpression] there is [org.jetbrains.kotlin.psi.KtContainerNodeForControlStructureBody] */
            && expression.parent.parent !is KtWhileExpression
        ) {
            kotlinFileContext.reportIssue(expression, message)
        }
    }

}
