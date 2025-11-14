/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import org.jetbrains.kotlin.psi.KtFunction
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

abstract class AbstractTooLongFunctionRule : AbstractCheck() {
    abstract var max: Int

    abstract val elementName: String

    protected fun check(function: KtFunction, kotlinFileContext: KotlinFileContext) {
        val expression = function.bodyBlockExpression ?: function.bodyExpression ?: return
        val numberOfLinesOfCode = expression.numberOfLinesOfCode()
        if (numberOfLinesOfCode > max) {
            kotlinFileContext.reportIssue(
                function.nameIdentifier ?: function.firstChild,
                "This $elementName has $numberOfLinesOfCode lines of code, which is greater than the $max authorized. Split it into smaller functions.")
        }
    }

}
