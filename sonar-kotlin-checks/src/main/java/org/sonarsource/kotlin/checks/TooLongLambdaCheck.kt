/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2026 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1.0.1, as published by SonarSource Sàrl.
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

import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.sonar.check.Rule
import org.sonar.check.RuleProperty
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

private const val DEFAULT_MAX = 20

@Rule(key = "S5612")
class TooLongLambdaCheck : AbstractTooLongFunctionRule() {

    @RuleProperty(
        key = "max",
        description = "Maximum authorized lines of code in a lambda expression",
        defaultValue = "" + DEFAULT_MAX,
    )
    override var max: Int = DEFAULT_MAX

    override val elementName = "lambda"

    override fun visitLambdaExpression(expression: KtLambdaExpression, kotlinFileContext: KotlinFileContext) {
        check(expression.functionLiteral, kotlinFileContext)
    }

}
