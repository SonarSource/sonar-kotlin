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

import org.jetbrains.kotlin.psi.KtNamedFunction
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

@Rule(key = "S1186")
class EmptyFunctionCheck : AbstractCheck() {

    override fun visitNamedFunction(function: KtNamedFunction, kotlinFileContext: KotlinFileContext) {
        val bodyBlockExpression = function.bodyBlockExpression
        if (bodyBlockExpression != null
            && bodyBlockExpression.statements.isEmpty()
            // function.lastChild is either bodyBlockExpression or trailing comment
            && !function.lastChild.hasComment()
        ) {
            kotlinFileContext.reportIssue(
                bodyBlockExpression,
                "Add a nested comment explaining why this function is empty or complete the implementation.")
        }
    }

}
