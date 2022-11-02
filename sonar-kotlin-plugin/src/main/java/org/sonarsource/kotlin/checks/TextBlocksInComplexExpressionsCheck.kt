/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2022 SonarSource SA
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

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.resolve.BindingContext
import org.sonar.check.Rule
import org.sonar.check.RuleProperty
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.api.predictRuntimeValueExpression
import org.sonarsource.kotlin.plugin.KotlinFileContext
import org.sonarsource.kotlin.api.isPlus as isConcat

@Rule(key = "S6203")
class TextBlocksInComplexExpressionsCheck : AbstractCheck() {

    val MESSAGE = "Move this text block out of the lambda body and refactor it to a local variable or a static final field."

    companion object {
        const val DEFAULT_LINES_NUMBER = 5
    }

    @RuleProperty(
        key = "MaximumNumberOfLines",
        description = "The maximum number of lines in a text block that can be nested into a complex expression.",
        defaultValue = "" + DEFAULT_LINES_NUMBER
    )
    var linesNumber = DEFAULT_LINES_NUMBER

    override fun visitLambdaExpression(expression: KtLambdaExpression, ctx: KotlinFileContext) {
        if (expression.parent is KtValueArgument) {
            expression.forEachDescendantOfType<KtStringTemplateExpression>(
                canGoInside = { elem -> elem !is KtLambdaExpression || elem === expression })
            { stringTemplate ->
                evaluateStringTemplateLines(stringTemplate, ctx)
            }
        }
    }

    private fun evaluateStringTemplateLines(stringTemplate: KtStringTemplateExpression, ctx: KotlinFileContext) {
        if (stringTemplate.firstChild.text.startsWith("\"\"\"")) {
            if (stringTemplate.numberOfLinesOfCode() > linesNumber) {
                ctx.reportIssue(stringTemplate, MESSAGE)
            }
        }
    }

}
