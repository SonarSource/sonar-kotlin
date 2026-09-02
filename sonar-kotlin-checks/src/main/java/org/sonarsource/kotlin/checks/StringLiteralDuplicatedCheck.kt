/*
 * SonarSource Kotlin
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.resolution.successfulFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtThrowExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.sonar.check.Rule
import org.sonar.check.RuleProperty
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.checks.FunMatcherImpl
import org.sonarsource.kotlin.api.checks.asString
import org.sonarsource.kotlin.api.reporting.SecondaryLocation
import org.sonarsource.kotlin.api.reporting.KotlinTextRanges.textRange
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.withKaSession

@Rule(key = "S1192")
class StringLiteralDuplicatedCheck : AbstractCheck() {

    companion object {
        private const val DEFAULT_THRESHOLD = 3
        private const val MINIMAL_LITERAL_LENGTH = 5
        private val NO_SEPARATOR_REGEXP = Regex("\\w++")

        private val LOGGING_CALL_MATCHERS = listOf(
            loggingMatcher(
                "org.slf4j.Logger",
                "trace", "debug", "info", "warn", "error",
            ),
            FunMatcher(qualifier = "java.util.logging.Logger") {
                withNames("severe", "warning", "info", "config", "fine", "finer", "finest", "log")
            },
            loggingMatcher(
                "org.apache.logging.log4j.Logger",
                "trace", "debug", "info", "warn", "error", "fatal", "log",
            ),
            loggingMatcher(
                "io.github.oshai.kotlinlogging.KLogger",
                "trace", "debug", "info", "warn", "error",
            ),
            loggingMatcher(
                "mu.KLogger",
                "trace", "debug", "info", "warn", "error",
            ),
        )

        private val KOTLIN_EXCEPTION_MESSAGE_CALLS = FunMatcher(qualifier = "kotlin") {
            withNames("error", "require", "check")
        }

        private fun loggingMatcher(type: String, vararg names: String): FunMatcherImpl =
            FunMatcher {
                withDefiningSupertypes(type)
                withNames(*names)
            }
    }

    private data class StringOccurrence(
        val expression: KtStringTemplateExpression,
        val triggersIssue: Boolean,
    )

    private data class LiteralCandidate(
        val expression: KtStringTemplateExpression,
        val text: String,
        val outermostConcatenation: KtExpression,
    )

    private data class CallArgument(
        val call: KtCallExpression,
        val isDirectValueArgument: Boolean,
    )

    @RuleProperty(
        key = "threshold",
        description = "Number of times a literal must be duplicated to trigger an issue",
        defaultValue = "" + DEFAULT_THRESHOLD
    )
    var threshold = DEFAULT_THRESHOLD

    private fun check(
        context: KotlinFileContext,
        occurrencesMap: Map<String, List<StringOccurrence>>,
    ) {
        for ((text, occurrences) in occurrencesMap) {
            val size = occurrences.size
            if (occurrences.count { it.triggersIssue } >= threshold) {
                val first = occurrences[0].expression
                context.reportIssue(
                    first,
                    """Define a constant instead of duplicating this literal "$text" $size times.""",
                    secondaryLocations = occurrences.asSequence()
                        .drop(1)
                        .map { SecondaryLocation(context.textRange(it.expression), "Duplication") }
                        .toList(),
                    gap = size - 1.0,
                )
            }
        }
    }

    override fun visitKtFile(file: KtFile, context: KotlinFileContext): Unit = withKaSession {
        if (context.inputFileContext.isTestFile) return

        val occurrences = collectStringTemplates(file)
            .mapNotNull { expression ->
                val text = expression.asString()
                if (text.length > MINIMAL_LITERAL_LENGTH && !NO_SEPARATOR_REGEXP.matches(text)) {
                    LiteralCandidate(expression, text, expression.outermostConcatenation())
                } else {
                    null
                }
            }
            .filterNot { candidate -> candidate.expression.isAdjacentLiteralFragment(candidate.outermostConcatenation) }
            .groupBy(
                { candidate -> candidate.text },
                { candidate ->
                    StringOccurrence(
                        candidate.expression,
                        triggersIssue = !candidate.outermostConcatenation.isNonTriggeringOccurrence(),
                    )
                },
            )
        check(context, occurrences)
    }

    private fun collectStringTemplates(node: PsiElement): Sequence<KtStringTemplateExpression> =
        when {
            node is KtStringTemplateExpression && !node.hasInterpolation() -> sequenceOf(node)
            node is KtAnnotationEntry -> emptySequence()
            node is KtCallExpression && node.isTodoCall() -> emptySequence()
            else -> node.children.asSequence().flatMap { collectStringTemplates(it) }
        }

    private fun KtCallExpression.isTodoCall(): Boolean =
        (calleeExpression as? KtNameReferenceExpression)?.getReferencedName() == "TODO"

    private fun KtExpression.isNonTriggeringOccurrence(): Boolean = withKaSession {
        val callArgument = containingCallArgument() ?: return false
        val resolvedCall = callArgument.call.resolveToCall()?.successfulFunctionCallOrNull() ?: return false
        return (callArgument.isDirectValueArgument &&
            callArgument.call.parent is KtThrowExpression &&
            resolvedCall.symbol is KaConstructorSymbol) ||
            LOGGING_CALL_MATCHERS.any { it.matches(resolvedCall) } ||
            KOTLIN_EXCEPTION_MESSAGE_CALLS.matches(resolvedCall)
    }

    /** Returns the call when this expression is a direct value argument or the result of a trailing lambda argument. */
    private fun KtExpression.containingCallArgument(): CallArgument? {
        val argument = parent as? KtValueArgument
        val argumentList = argument?.parent as? KtValueArgumentList
        val directCall = argumentList?.parent as? KtCallExpression
        if (directCall != null) return CallArgument(directCall, isDirectValueArgument = true)

        val block = parent as? KtBlockExpression ?: return null
        if (block.statements.lastOrNull() !== this) return null
        val functionLiteral = block.parent as? KtFunctionLiteral ?: return null
        val lambdaExpression = functionLiteral.parent as? KtLambdaExpression ?: return null
        val lambdaArgument = lambdaExpression.parent as? KtLambdaArgument ?: return null
        val lambdaCall = lambdaArgument.parent as? KtCallExpression ?: return null
        return CallArgument(lambdaCall, isDirectValueArgument = false)
    }

    private fun KtStringTemplateExpression.outermostConcatenation(): KtExpression {
        var expression: KtExpression = this
        while (expression.parent.isPlusExpression()) {
            expression = expression.parent as KtBinaryExpression
        }
        return expression
    }

    private fun KtStringTemplateExpression.isAdjacentLiteralFragment(concatenation: KtExpression): Boolean {
        if (concatenation === this) return false

        val operands = concatenation.flattenedPlusOperands()
        val index = operands.indexOfFirst { it === this }
        return (index > 0 && operands[index - 1] is KtStringTemplateExpression) ||
            (index >= 0 && index < operands.lastIndex && operands[index + 1] is KtStringTemplateExpression)
    }

    private fun KtExpression.flattenedPlusOperands(): List<KtExpression> =
        if (isPlusExpression()) {
            val binary = this as KtBinaryExpression
            listOfNotNull(binary.left).flatMap { it.flattenedPlusOperands() } +
                listOfNotNull(binary.right).flatMap { it.flattenedPlusOperands() }
        } else {
            listOf(this)
        }

    private fun PsiElement?.isPlusExpression(): Boolean =
        this is KtBinaryExpression && operationToken == KtTokens.PLUS
}
