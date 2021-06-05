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

import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.sonar.check.Rule
import org.sonar.check.RuleProperty
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.plugin.KotlinFileContext

/**
 * Replacement for [org.sonarsource.slang.checks.TooLongFunctionCheck]
 */
@Rule(key = "S138")
class TooLongFunctionCheck : AbstractCheck() {
    companion object {
        const val DEFAULT_MAX = 100
    }

    @RuleProperty(
        key = "max",
        description = "Maximum authorized lines of code in a function",
        defaultValue = "" + DEFAULT_MAX,
    )
    var max: Int = DEFAULT_MAX

    override fun visitSecondaryConstructor(constructor: KtSecondaryConstructor, kotlinFileContext: KotlinFileContext) {
        check(constructor, kotlinFileContext)
    }

    override fun visitNamedFunction(function: KtNamedFunction, kotlinFileContext: KotlinFileContext) {
        check(function, kotlinFileContext)
    }

    override fun visitLambdaExpression(expression: KtLambdaExpression, kotlinFileContext: KotlinFileContext) {
        // TODO Slang has no notion of lambda expressions, they are treated as functions
        check(expression.functionLiteral, kotlinFileContext)
    }

    private fun check(function: KtFunction, kotlinFileContext: KotlinFileContext) {
        if (function.receiverTypeReference != null) {
            /** see [org.sonarsource.kotlin.converter.KotlinTreeVisitor.createFunctionDeclarationTree] */
            return
        }
        val expression = function.bodyBlockExpression ?: function.bodyExpression ?: return
        val numberOfLinesOfCode = expression.numberOfLinesOfCode()
        if (numberOfLinesOfCode > max) {
            kotlinFileContext.reportIssue(
                /** see [org.sonarsource.slang.impl.FunctionDeclarationTreeImpl.rangeToHighlight] */
                function.nameIdentifier ?: function.firstChild,
                "This function has $numberOfLinesOfCode lines of code, which is greater than the $max authorized. Split it into smaller functions.")
        }
    }

}
