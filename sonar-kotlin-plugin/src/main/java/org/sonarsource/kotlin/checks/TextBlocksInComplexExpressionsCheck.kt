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

import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtTreeVisitor
import org.jetbrains.kotlin.psi.KtValueArgument
import org.sonar.check.Rule
import org.sonar.check.RuleProperty
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.plugin.KotlinFileContext
import java.util.regex.Pattern

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

    override fun visitLambdaExpression(expression: KtLambdaExpression, ctx: KotlinFileContext?) {
        if (expression.parent is KtValueArgument) {
            var finder = TextBlockFinder(linesNumber)
            expression.bodyExpression?.accept(finder)
            finder.misusedTextBlocks.forEach { block ->
                ctx?.reportIssue(block, MESSAGE)
            }
        }
    }

    class TextBlockFinder : KtTreeVisitor<KtStringTemplateExpression> {

        var maxLines = DEFAULT_LINES_NUMBER

        constructor(maxLines: Int) {
            this.maxLines = maxLines
        }

        val misusedTextBlocks = mutableListOf<KtStringTemplateExpression>()

        // We visit all string templates in the lambda body, and check if they start with triple quote.
        // If they do, we check that they do not exceed the maximum amount of allowed lines
        override fun visitStringTemplateExpression(expression: KtStringTemplateExpression, data: KtStringTemplateExpression?): Void? {
            if (expression.firstChild.text.startsWith("\"\"\"")) {
                var lines = expression.text.split(Pattern.compile("\r?\n|\r"))?.size
                if (lines != null && lines > maxLines) {
                    misusedTextBlocks.add(expression)
                }
            }
            return null
        }

    }

}
