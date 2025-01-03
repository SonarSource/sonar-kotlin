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
package org.sonarsource.kotlin.gradle.checks

import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.reporting.KotlinTextRanges.textRange

private const val VERSION_ARGUMENT_INDEX = 2
private const val VERSION_ARGUMENT_IDENTIFIER = "version"

private val PATTERN_DEPENDENCY_WITH_VERSION = Regex("[^:]+:[^:]+:[^:]+")

@Rule(key = "S6624")
class DependencyVersionHardcodedCheck : AbstractCheck() {

    override fun visitCallExpression(expression: KtCallExpression, kotlinFileContext: KotlinFileContext) {
        if (getFunctionName(expression) != REF_NAME_DEPENDENCIES) return
        checkDependencyHandlerScopeLambda(expression, kotlinFileContext)
    }

    private fun checkDependencyHandlerScopeLambda(expression: KtCallExpression, kotlinFileContext: KotlinFileContext) {
        getLambdaBlock(expression)?.acceptChildren(DependencyHandlerScopeLambdaCheck(kotlinFileContext))
    }

    private fun reportIssue(
        kotlinFileContext: KotlinFileContext,
        textStartOffset: Int,
        textEndOffset: Int
    ) {
        kotlinFileContext.reportIssue(
            kotlinFileContext.textRange(textStartOffset, textEndOffset),
            """Do not hardcode version numbers."""
        )
    }

    private inner class DependencyHandlerScopeLambdaCheck(
        private val kotlinFileContext: KotlinFileContext
    ) : KtVisitorVoid() {

        override fun visitCallExpression(expression: KtCallExpression) {
            val functionName = getFunctionName(expression) ?: return
            if (!REF_NAME_DEPENDENCY_HANDLER_SCOPE_EXTENSIONS.contains(functionName)) return

            if (expression.valueArguments.size == 1) {
                val stringArgument = expression.valueArguments.first().getArgumentExpression() as? KtStringTemplateExpression ?: return
                val simpleString = stringArgument.simpleStringOrNull() ?: return

                if (simpleString.text.matches(PATTERN_DEPENDENCY_WITH_VERSION)) {
                    with(stringArgument.textRange) {
                        reportIssue(
                            kotlinFileContext,
                            startOffset + simpleString.text.lastIndexOf(':') + 2,
                            endOffset - 1
                        )
                    }
                }
            } else {
                val stringArgument = findVersionArgumentOrNull(expression.valueArguments)
                    ?.getArgumentExpression() as? KtStringTemplateExpression

                if (stringArgument?.simpleStringOrNull() != null) {
                    with(stringArgument.textRange) {
                        reportIssue(kotlinFileContext, startOffset + 1, endOffset - 1)
                    }
                }
            }
        }
    }
}

private fun KtStringTemplateExpression.simpleStringOrNull() = if (entries.size == 1) {
    entries.first() as? KtLiteralStringTemplateEntry
} else {
    null
}

private fun findVersionArgumentOrNull(valueArguments: List<KtValueArgument>) =
    valueArguments.find {
        it.getArgumentName()?.asName?.identifier == VERSION_ARGUMENT_IDENTIFIER
    } ?: if (valueArguments.size > VERSION_ARGUMENT_INDEX && !valueArguments[VERSION_ARGUMENT_INDEX].isNamed()) {
        valueArguments[VERSION_ARGUMENT_INDEX]
    } else {
        null
    }
