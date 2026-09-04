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
import java.util.Collections
import java.util.IdentityHashMap
import org.jetbrains.kotlin.analysis.api.resolution.successfulFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
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
        private val COMPOSE_PREVIEW_CLASS_ID = ClassId.fromString("androidx/compose/ui/tooling/preview/Preview")

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
        candidatesMap: Map<String, List<KtStringTemplateExpression>>,
    ) {
        val inspectedConcatenations: MutableSet<KtExpression> =
            Collections.newSetFromMap(IdentityHashMap())
        val adjacentLiteralFragments: MutableSet<KtStringTemplateExpression> =
            Collections.newSetFromMap(IdentityHashMap())

        for ((text, candidates) in candidatesMap) {
            if (candidates.size < threshold) continue

            val relevantCandidates = candidates.mapNotNull { expression ->
                if (expression in adjacentLiteralFragments) return@mapNotNull null

                val concatenation = expression.outermostConcatenation()
                if (inspectedConcatenations.add(concatenation)) {
                    concatenation.collectAdjacentLiteralFragments(adjacentLiteralFragments)
                }
                if (expression in adjacentLiteralFragments) null else LiteralCandidate(expression, concatenation)
            }
            if (relevantCandidates.size < threshold) continue

            val occurrences = relevantCandidates.map { candidate ->
                StringOccurrence(
                    candidate.expression,
                    triggersIssue = !candidate.outermostConcatenation.isNonTriggeringOccurrence(),
                )
            }
            val triggeringOccurrences = occurrences.filter { it.triggersIssue }
            val first = triggeringOccurrences.firstOrNull()?.expression ?: continue
            if (triggeringOccurrences.size < threshold) continue

            val size = occurrences.size
            context.reportIssue(
                first,
                """Define a constant instead of duplicating this literal "$text" $size times.""",
                secondaryLocations = occurrences.asSequence()
                    .filterNot { it.expression === first }
                    .map { SecondaryLocation(context.textRange(it.expression), "Duplication") }
                    .toList(),
                gap = size - 1.0,
            )
        }
    }

    override fun visitKtFile(file: KtFile, context: KotlinFileContext): Unit = withKaSession {
        if (context.inputFileContext.isTestFile) return

        val occurrences = collectStringTemplates(file)
            .mapNotNull { expression ->
                val text = expression.asString()
                if (text.length > MINIMAL_LITERAL_LENGTH && !NO_SEPARATOR_REGEXP.matches(text)) {
                    text to expression
                } else {
                    null
                }
            }
            .groupBy(
                keySelector = { (text) -> text },
                valueTransform = { (_, expression) -> expression },
            )
        check(context, occurrences)
    }

    private fun collectStringTemplates(node: PsiElement): Sequence<KtStringTemplateExpression> =
        when {
            node is KtStringTemplateExpression && !node.hasInterpolation() -> sequenceOf(node)
            node is KtAnnotationEntry -> emptySequence()
            node is KtCallExpression && node.isTodoCall() -> emptySequence()
            node is KtNamedFunction && node.annotationEntries.isNotEmpty() && node.isComposePreview() -> emptySequence()
            else -> node.children.asSequence().flatMap { collectStringTemplates(it) }
        }

    private fun KtNamedFunction.isComposePreview(): Boolean = withKaSession {
        symbol.annotations.any { it.classId == COMPOSE_PREVIEW_CLASS_ID }
    }

    private fun KtCallExpression.isTodoCall(): Boolean =
        (calleeExpression as? KtNameReferenceExpression)?.getReferencedName() == "TODO"

    private fun KtExpression.isNonTriggeringOccurrence(): Boolean = withKaSession {
        val callArgument = containingCallArgument() ?: return false
        val resolvedCall = callArgument.call.resolveToCall()?.successfulFunctionCallOrNull() ?: return false
        return (callArgument.isDirectValueArgument &&
            callArgument.call.directlyContainingExpression().parent is KtThrowExpression &&
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

    private fun KtExpression.collectAdjacentLiteralFragments(destination: MutableSet<KtStringTemplateExpression>) {
        val operands = flattenedPlusOperands()
        for (index in 0 until operands.lastIndex) {
            val left = operands[index] as? KtStringTemplateExpression
            val right = operands[index + 1] as? KtStringTemplateExpression
            if (left != null && right != null && !left.hasInterpolation() && !right.hasInterpolation()) {
                destination.add(left)
                destination.add(right)
            }
        }
    }

    private fun KtCallExpression.directlyContainingExpression(): KtExpression =
        (parent as? KtDotQualifiedExpression)
            ?.takeIf { it.selectorExpression === this }
            ?: this

    private fun KtExpression.flattenedPlusOperands(): List<KtExpression> {
        val operands = mutableListOf<KtExpression>()

        fun collect(expression: KtExpression) {
            if (expression.isPlusExpression()) {
                val binary = expression as KtBinaryExpression
                binary.left?.let { collect(it) }
                binary.right?.let { collect(it) }
            } else {
                operands.add(expression)
            }
        }

        collect(this)
        return operands
    }

    private fun PsiElement?.isPlusExpression(): Boolean =
        this is KtBinaryExpression && operationToken == KtTokens.PLUS
}
