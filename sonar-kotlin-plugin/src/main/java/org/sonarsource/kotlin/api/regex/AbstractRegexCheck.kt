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
package org.sonarsource.kotlin.api.regex

import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtStringTemplateEntryWithExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.util.getReceiverExpression
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.sonar.api.batch.fs.TextRange
import org.sonarsource.analyzer.commons.regex.RegexIssueLocation
import org.sonarsource.analyzer.commons.regex.RegexParseResult
import org.sonarsource.analyzer.commons.regex.ast.FlagSet
import org.sonarsource.kotlin.api.CallAbstractCheck
import org.sonarsource.kotlin.api.ConstructorMatcher
import org.sonarsource.kotlin.api.FunMatcher
import org.sonarsource.kotlin.api.FunMatcherImpl
import org.sonarsource.kotlin.api.INT_TYPE
import org.sonarsource.kotlin.api.JAVA_STRING
import org.sonarsource.kotlin.api.STRING_TYPE
import org.sonarsource.kotlin.api.SecondaryLocation
import org.sonarsource.kotlin.api.predictRuntimeValueExpression
import org.sonarsource.kotlin.converter.KotlinTextRanges.textRange
import org.sonarsource.kotlin.plugin.KotlinFileContext
import java.util.regex.Pattern
import org.sonarsource.kotlin.api.isPlus as isConcat

val PATTERN_COMPILE_MATCHER = FunMatcher(qualifier = "java.util.regex.Pattern", name = "compile")
val REGEX_MATCHER = ConstructorMatcher(typeName = "kotlin.text.Regex")
val TO_REGEX_MATCHER = FunMatcher(qualifier = "kotlin.text", name = "toRegex", extensionFunction = true)

private fun argGetter(argIndex: Int) = { resolvedCall: ResolvedCall<*> ->
    resolvedCall.valueArgumentsByIndex?.getOrNull(argIndex)?.arguments?.getOrNull(0)?.getArgumentExpression()
}

private val referenceTargetGetter = { resolvedCall: ResolvedCall<*> ->
    resolvedCall.getReceiverExpression()
}

// TODO: add org.apache.commons.lang3.RegExUtils
private val REGEX_FUNCTIONS: Map<FunMatcherImpl, Pair<(ResolvedCall<*>) -> KtExpression?, (ResolvedCall<*>) -> KtExpression?>> = mapOf(
    REGEX_MATCHER to (argGetter(0) to argGetter(1)),
    TO_REGEX_MATCHER to (referenceTargetGetter to argGetter(0)),
    PATTERN_COMPILE_MATCHER to (argGetter(0) to argGetter(1)),
    FunMatcher(qualifier = JAVA_STRING) {
        withNames("replaceAll", "replaceFirst", "split", "matches")
        withArguments(STRING_TYPE, STRING_TYPE)
        withArguments(STRING_TYPE)
        withArguments(STRING_TYPE, INT_TYPE)
    } to (argGetter(0) to { null }),
)

private val FLAGS = mapOf(
    "UNIX_LINES" to 1,
    "IGNORE_CASE" to (2 or 64),
    "CASE_INSENSITIVE" to 2,
    "COMMENTS" to 4,
    "MULTILINE" to 8,
    "LITERAL" to 16,
    "DOT_MATCHES_ALL" to 32,
    "DOTALL" to 32,
    "UNICODE_CASE" to 64,
    "CANON_EQ" to 128,
    "UNICODE_CHARACTER_CLASS" to 256,
)

private const val REGEX_CALL_LOC_MSG = "Function call of which the argument is interpreted as regular expression."

abstract class AbstractRegexCheck : CallAbstractCheck() {
    override val functionsToVisit = REGEX_FUNCTIONS.keys

    open fun visitRegex(regex: RegexParseResult, regexContext: RegexContext) = Unit

    open fun visitRegex(
        regex: RegexParseResult,
        regexContext: RegexContext,
        callExpression: KtCallExpression,
        matchedFun: FunMatcherImpl,
        kotlinFileContext: KotlinFileContext
    ) = visitRegex(regex, regexContext)

    override fun visitFunctionCall(
        callExpression: KtCallExpression,
        resolvedCall: ResolvedCall<*>,
        matchedFun: FunMatcherImpl,
        kotlinFileContext: KotlinFileContext
    ) {
        REGEX_FUNCTIONS[matchedFun]!!.let { (regexStringArgExtractor, flagsArgExtractor) ->
            val regexCtx = regexStringArgExtractor(resolvedCall)
                .collectResolvedListOfStringTemplates(kotlinFileContext.bindingContext)
                // For now, we simply don't use any sequence that contains nulls (i.e. non-resolvable parts)
                .takeIf { null !in it }?.filterNotNull()
                ?.flatMap { it.entries.asSequence() }
                // We don't handle string interpolation for now. So we filter out all string templates with expressions
                ?.takeIf { it.all { entry -> entry !is KtStringTemplateEntryWithExpression } }
                ?.let { sourceTemplates ->
                    RegexContext(sourceTemplates.asIterable(), kotlinFileContext)
                } ?: return

            flagsArgExtractor(resolvedCall).extractRegexFlags(kotlinFileContext.bindingContext)
                .takeIf { flags -> Pattern.LITERAL !in flags }
                ?.let { flags ->

                    visitRegex(regexCtx.parseRegex(flags), regexCtx, callExpression, matchedFun, kotlinFileContext)

                    regexCtx.reportedIssues.mapNotNull {
                        it.prepareForReporting(callExpression, regexCtx, kotlinFileContext)
                    }.forEach { (mainLocation, message, secondaries, gap) ->
                        kotlinFileContext.reportIssue(mainLocation, message, secondaries, gap)
                    }
                }
        }
    }

    fun RegexIssueLocation.toSecondaries(textRangeTracker: TextRangeTracker, kotlinFileContext: KotlinFileContext) =
        this.syntaxElements()
            .mapNotNull { kotlinFileContext.mergeTextRanges(textRangeTracker.textRangesBetween(it.range)) }
            .flatten()
            .map { SecondaryLocation(it, message()) }

    private fun ReportedIssue.prepareForReporting(
        regexCallExpression: KtCallExpression,
        regexCtx: RegexContext,
        kotlinFileContext: KotlinFileContext
    ): AnalyzerIssueReportInfo? {
        val (mainLocation, additionalSecondaries) = kotlinFileContext.mergeTextRanges(
            regexCtx.regexSource.textRangeTracker.textRangesBetween(regexElement.range.beginningOffset, regexElement.range.endingOffset)
        )?.let { mergedRanges ->
            mergedRanges.first() to mergedRanges.drop(1).map { SecondaryLocation(it, message) }
        } ?: return null

        // Add the method call as the last secondary to make sure it is always referenced (can sometimes be far away from the regex string)
        val allSecondaries =
            (additionalSecondaries + secondaryLocations.flatMap {
                it.toSecondaries(regexCtx.regexSource.textRangeTracker, kotlinFileContext)
            } + SecondaryLocation(kotlinFileContext.textRange(regexCallExpression.calleeExpression!!), REGEX_CALL_LOC_MSG))
                .distinct()

        return AnalyzerIssueReportInfo(mainLocation, message, allSecondaries, gap)
    }
}

private data class AnalyzerIssueReportInfo(
    val mainLocation: TextRange,
    val message: String,
    val secondaryLocations: List<SecondaryLocation>,
    val gap: Double?,
)

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
