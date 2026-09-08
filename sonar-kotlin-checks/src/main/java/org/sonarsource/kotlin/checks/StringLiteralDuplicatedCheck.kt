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
        private const val PREVIEW_ANNOTATION_NAME = "Preview"
    }

    private data class LiteralCandidate(
        val expression: KtStringTemplateExpression,
        val outermostConcatenation: KtExpression,
    )

    private object AdjacentStringConcatenationFilter {

        /**
         * Omits adjacent static fragments because they form one runtime string in a string concatenation.
         * The outermost concatenation is retained for subsequent occurrence classification.
         */
        fun filter(candidates: List<KtStringTemplateExpression>): List<LiteralCandidate> {
            val inspectedConcatenations: MutableSet<KtExpression> =
                Collections.newSetFromMap(IdentityHashMap())
            val adjacentLiteralFragments: MutableSet<KtStringTemplateExpression> =
                Collections.newSetFromMap(IdentityHashMap())

            return candidates.mapNotNull { expression ->
                if (expression in adjacentLiteralFragments) return@mapNotNull null

                val concatenation = expression.outermostConcatenation()
                if (inspectedConcatenations.add(concatenation)) {
                    concatenation.collectAdjacentLiteralFragments(adjacentLiteralFragments)
                }
                if (expression in adjacentLiteralFragments) null else LiteralCandidate(expression, concatenation)
            }
        }

        private fun KtStringTemplateExpression.outermostConcatenation(): KtExpression {
            var expression: KtExpression = this
            while (expression.parent.isPlusExpression()) {
                expression = expression.parent as KtBinaryExpression
            }
            return expression
        }

        private fun KtExpression.collectAdjacentLiteralFragments(
            destination: MutableSet<KtStringTemplateExpression>,
        ) {
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
        for ((text, candidates) in candidatesMap) {
            if (candidates.size < threshold) continue

            val relevantCandidates = AdjacentStringConcatenationFilter.filter(candidates)
            if (relevantCandidates.size < threshold) continue

            val triggeringCandidates = relevantCandidates.filterNot { candidate ->
                NonTriggeringOccurrenceClassifier.isNonTriggering(candidate.outermostConcatenation)
            }
            if (triggeringCandidates.size < threshold) continue
            val first = triggeringCandidates.firstOrNull()?.expression ?: continue

            // Non-triggering occurrences do not cause an issue, but are included once one is reported.
            val size = relevantCandidates.size
            context.reportIssue(
                first,
                """Define a constant instead of duplicating this literal "$text" $size times.""",
                secondaryLocations = relevantCandidates.asSequence()
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
            .map { expression -> expression.asString() to expression }
            .filter { (text) -> text.length > MINIMAL_LITERAL_LENGTH && !NO_SEPARATOR_REGEXP.matches(text) }
            .groupBy(
                keySelector = { it.first },
                valueTransform = { it.second },
            )
        check(context, occurrences)
    }

    private fun collectStringTemplates(node: PsiElement): Sequence<KtStringTemplateExpression> =
        when {
            node is KtStringTemplateExpression && !node.hasInterpolation() -> sequenceOf(node)
            node is KtAnnotationEntry -> emptySequence()
            node is KtCallExpression && node.isTodoCall() -> emptySequence()
            node is KtNamedFunction && node.hasComposePreviewAnnotation() -> emptySequence()
            else -> node.children.asSequence().flatMap { collectStringTemplates(it) }
        }

    /**
     * Jetpack Compose Preview functions contain design-time fixtures, rather than production string literals.
     * Reporting duplications in them has been a common source of user complaints.
     * Match annotations by name rather than resolving class IDs because analysis may run without type information.
     * Suppressing additional functions annotated with `Preview` that are not from Compose is an acceptable trade-off to reduce noise.
     */
    private fun KtNamedFunction.hasComposePreviewAnnotation(): Boolean =
        annotationEntries.any { it.shortName?.asString() == PREVIEW_ANNOTATION_NAME }

    private fun KtCallExpression.isTodoCall(): Boolean =
        (calleeExpression as? KtNameReferenceExpression)?.getReferencedName() == "TODO"

}

private object NonTriggeringOccurrenceClassifier {

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
        loggingMatcher(
            "co.touchlab.kermit.Logger",
            "v", "d", "i", "w", "e", "a",
        ),
        loggingMatcher(
            "com.github.aakira.napier.Napier",
            "v", "d", "i", "w", "e", "wtf",
        ),
        loggingMatcher("io.ktor.client.plugins.logging.Logger", "log"),
        loggingMatcher(
            "timber.log.Timber",
            "v", "d", "i", "w", "e", "wtf",
        ),
        // Timber 5 declares log methods on Tree, which is extended by the Forest companion object.
        loggingMatcher(
            "timber.log.Timber.Tree",
            "v", "d", "i", "w", "e", "wtf",
        ),
        loggingMatcher(
            "android.util.Log",
            "v", "d", "i", "w", "e", "wtf", "println",
        ),
    )

    private val KOTLIN_EXCEPTION_MESSAGE_CALLS = FunMatcher(qualifier = "kotlin") {
        withNames("error", "require", "check")
    }

    private data class CallArgument(
        val call: KtCallExpression,
        val isDirectValueArgument: Boolean,
    )

    /**
     * Returns whether [expression] is a message for a thrown exception, recognized logging call,
     * or Kotlin precondition. Such occurrences may be included in a reported duplication, but do
     * not contribute toward its triggering threshold.
     */
    fun isNonTriggering(expression: KtExpression): Boolean = withKaSession {
        val callArgument = expression.containingCallArgument() ?: return false
        val resolvedCall = callArgument.call.resolveToCall()?.successfulFunctionCallOrNull() ?: return false
        return (callArgument.isDirectValueArgument &&
            callArgument.call.directlyContainingExpression().parent is KtThrowExpression &&
            resolvedCall.symbol is KaConstructorSymbol) ||
            // Logging and Kotlin precondition messages are intentionally allowed to be repeated.
            LOGGING_CALL_MATCHERS.any { it.matches(resolvedCall) } ||
            KOTLIN_EXCEPTION_MESSAGE_CALLS.matches(resolvedCall)
    }

    /** Returns the call when this expression is a direct value argument or the result of a trailing lambda argument. */
    private fun KtExpression.containingCallArgument(): CallArgument? {
        val argument = parent as? KtValueArgument
        val argumentList = argument?.parent as? KtValueArgumentList
        val directCall = argumentList?.parent as? KtCallExpression
        if (directCall != null) return CallArgument(directCall, isDirectValueArgument = true)

        val lambdaCall = isLastInLambdaCall() ?: return null
        return CallArgument(lambdaCall, isDirectValueArgument = false)
    }

    private fun KtCallExpression.directlyContainingExpression(): KtExpression =
        (parent as? KtDotQualifiedExpression)
            ?.takeIf { it.selectorExpression === this }
            ?: this

    /** Returns the enclosing call when this is the final expression of a trailing lambda argument. */
    private fun KtExpression.isLastInLambdaCall(): KtCallExpression? {
        val block = (parent as? KtBlockExpression)?.takeIf { it.statements.lastOrNull() === this } ?: return null
        val functionLiteral = block.parent as? KtFunctionLiteral ?: return null
        val lambdaExpression = functionLiteral.parent as? KtLambdaExpression ?: return null
        val lambdaArgument = lambdaExpression.parent as? KtLambdaArgument ?: return null
        return lambdaArgument.parent as? KtCallExpression
    }

    private fun loggingMatcher(type: String, vararg names: String): FunMatcherImpl =
        FunMatcher {
            withDefiningSupertypes(type)
            withNames(*names)
        }
}
