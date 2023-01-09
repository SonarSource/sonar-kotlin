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

import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.sonar.check.Rule
import org.sonar.check.RuleProperty
import org.sonarsource.kotlin.plugin.KotlinFileContext

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
