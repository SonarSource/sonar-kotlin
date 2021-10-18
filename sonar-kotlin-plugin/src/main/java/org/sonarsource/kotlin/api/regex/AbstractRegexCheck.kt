/*
 * SonarSource Kotlin
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
package org.sonarsource.kotlin.api.regex

import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getReceiverExpression
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.sonar.api.batch.fs.TextRange
import org.sonarsource.analyzer.commons.regex.RegexParseResult
import org.sonarsource.analyzer.commons.regex.ast.FlagSet
import org.sonarsource.analyzer.commons.regex.ast.RegexSyntaxElement
import org.sonarsource.kotlin.api.CallAbstractCheck
import org.sonarsource.kotlin.api.ConstructorMatcher
import org.sonarsource.kotlin.api.FunMatcher
import org.sonarsource.kotlin.api.FunMatcherImpl
import org.sonarsource.kotlin.api.INT_TYPE
import org.sonarsource.kotlin.api.JAVA_STRING
import org.sonarsource.kotlin.api.STRING_TYPE
import org.sonarsource.kotlin.api.SecondaryLocation
import org.sonarsource.kotlin.api.predictRuntimeValueExpression
import org.sonarsource.kotlin.converter.KotlinTextRanges.textPointerAtOffset
import org.sonarsource.kotlin.converter.KotlinTextRanges.textRange
import org.sonarsource.kotlin.plugin.KotlinFileContext
import org.sonarsource.kotlin.api.isPlus as isConcat

private fun argGetter(argIndex: Int) = { resolvedCall: ResolvedCall<*> ->
    resolvedCall.valueArgumentsByIndex?.getOrNull(argIndex)?.arguments?.getOrNull(0)?.getArgumentExpression()
}

private val referenceTargetGetter = { resolvedCall: ResolvedCall<*> ->
    resolvedCall.getReceiverExpression()
}

// TODO: add org.apache.commons.lang3.RegExUtils
private val REGEX_FUNCTIONS: Map<FunMatcherImpl, Pair<(ResolvedCall<*>) -> KtExpression?, (ResolvedCall<*>) -> KtExpression?>> = mapOf(
    ConstructorMatcher(typeName = "kotlin.text.Regex") to (argGetter(0) to argGetter(1)),
    FunMatcher(qualifier = "kotlin.text", name = "toRegex", extensionFunction = true) to (referenceTargetGetter to argGetter(0)),
    FunMatcher(qualifier = "java.util.regex.Pattern", name = "compile") to (argGetter(0) to argGetter(1)),
    FunMatcher(qualifier = JAVA_STRING) {
        withNames("replaceAll", "replaceFirst", "split", "matches")
        withArguments(STRING_TYPE, STRING_TYPE)
        withArguments(STRING_TYPE)
        withArguments(STRING_TYPE, INT_TYPE)
    } to (argGetter(0) to { null }),
)

private val FLAGS = mapOf(
    "UNIX_LINES" to 1,
    "IGNORE_CASE" to 2,
    "CASE_INSENSITIVE" to 2,
    "COMMENTS" to 4,
    "MULTILINE" to 8,
    "LITERAL" to 16,
    "DOT_MATCHES_ALL" to 32,
    "DOTALL" to 32,
    "UNICODE_CASE" to 64,
    "CANON_EQ" to 128,
)

abstract class AbstractRegexCheck : CallAbstractCheck() {
    override val functionsToVisit = REGEX_FUNCTIONS.keys

    abstract fun visitRegex(regex: RegexParseResult, regexSource: KotlinAnalyzerRegexSource)

    override fun visitFunctionCall(
        callExpression: KtCallExpression,
        resolvedCall: ResolvedCall<*>,
        matchedFun: FunMatcherImpl,
        kotlinFileContext: KotlinFileContext
    ) {
        REGEX_FUNCTIONS[matchedFun]!!.let { (regexStringArgExtractor, flagsArgExtractor) ->
            regexStringArgExtractor(resolvedCall)
                .collectResolvedListOfStringTemplates(kotlinFileContext.bindingContext)
                // For now, we simply don't use any sequence that contains nulls (i.e. non-resolvable parts)
                .takeIf { null !in it }?.filterNotNull()
                ?.let { sourceTemplates ->
                    val (regexParseResult, regexSource) =
                        kotlinFileContext.regexCache.get(
                            sourceTemplates.toList(),
                            flagsArgExtractor(resolvedCall).extractRegexFlags(kotlinFileContext.bindingContext),
                            callExpression
                        )
                    visitRegex(regexParseResult, regexSource)
                }
        }
    }

    protected fun KotlinAnalyzerRegexSource.reportIssue(
        regexElement: RegexSyntaxElement,
        message: String,
        secondaryLocations: List<SecondaryLocation> = emptyList(),
        gap: Double? = null
    ) {
        val range = regexElement.range
        val (mainLocation, additionalSecondaries) =
            kotlinFileContext.mergeTextRanges(textRangeTracker.textRangesBetween(range.beginningOffset, range.endingOffset), message)
                ?: return

        if (regexCallExpression != null && !kotlinFileContext.textRange(regexCallExpression).overlap(mainLocation)) {
            kotlinFileContext.reportIssue(
                regexCallExpression.calleeExpression!!,
                message,
                listOf(SecondaryLocation(mainLocation, message)) + additionalSecondaries + secondaryLocations,
                gap
            )
        } else {
            kotlinFileContext.reportIssue(mainLocation, message, additionalSecondaries + secondaryLocations, gap)
        }
    }
}

private fun KtExpression?.extractRegexFlags(bindingContext: BindingContext): FlagSet =
    FlagSet(
        this?.collectDescendantsOfType<KtReferenceExpression>()
            ?.map { it.predictRuntimeValueExpression(bindingContext) }
            ?.flatMap { it.collectDescendantsOfType<KtNameReferenceExpression>() }
            ?.mapNotNull { bindingContext.get(BindingContext.REFERENCE_TARGET, it) }
            ?.mapNotNull { FLAGS[it.name.asString()] }
            ?.fold(0, Int::or)
            ?: 0
    )

private data class MutableOffsetRange(var start: Int, var end: Int)

private fun KotlinFileContext.mergeTextRanges(ranges: Iterable<TextRange>, message: String) =
    ktFile.viewProvider.document!!.let { doc ->
        ranges.map {
            MutableOffsetRange(
                doc.getLineStartOffset(it.start().line() - 1) + it.start().lineOffset(),
                doc.getLineStartOffset(it.end().line() - 1) + it.end().lineOffset()
            )
        }.fold(mutableListOf<MutableOffsetRange>()) { acc, curOffsets ->
            acc.apply {
                lastOrNull()?.takeIf { prevOffsets ->
                    // Does current range overlap with previous one?
                    curOffsets.end >= prevOffsets.start && curOffsets.start <= prevOffsets.end
                }?.let { prevOffsets ->
                    // This range overlaps with the previous one => merge
                    prevOffsets.end = curOffsets.end
                }
                    ?: add(curOffsets) // This range does not overlap with the previous one (or there is no previous one) => add as separate range
            }
        }.map { (startOffset, endOffset) ->
            with(inputFileContext.inputFile) {
                newRange(textPointerAtOffset(doc, startOffset), textPointerAtOffset(doc, endOffset))
            }
        }.takeIf {
            it.isNotEmpty()
        }?.let { mergedRanges ->
            mergedRanges.first() to mergedRanges.drop(1).map { SecondaryLocation(it, message) }
        }
    }

private fun KtExpression?.collectResolvedListOfStringTemplates(bindingContext: BindingContext): Sequence<KtStringTemplateExpression?> =
    this?.predictRuntimeValueExpression(bindingContext).let { predictedValue ->
        when {
            predictedValue is KtStringTemplateExpression -> sequenceOf(predictedValue)
            predictedValue is KtBinaryExpression && predictedValue.isConcat() ->
                predictedValue.left.collectResolvedListOfStringTemplates(bindingContext) +
                    predictedValue.right.collectResolvedListOfStringTemplates(bindingContext)
            else -> sequenceOf(null)
        }
    }
