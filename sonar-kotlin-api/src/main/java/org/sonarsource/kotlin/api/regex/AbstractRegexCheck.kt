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
package org.sonarsource.kotlin.api.regex

import org.jetbrains.kotlin.analysis.api.resolution.KaExplicitReceiverValue
import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.analysis.api.symbols.name
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtStringTemplateEntryWithExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.sonar.api.batch.fs.TextRange
import org.sonarsource.analyzer.commons.regex.RegexIssueLocation
import org.sonarsource.analyzer.commons.regex.RegexParseResult
import org.sonarsource.analyzer.commons.regex.ast.FlagSet
import org.sonarsource.kotlin.api.checks.CallAbstractCheck
import org.sonarsource.kotlin.api.checks.ConstructorMatcher
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.checks.FunMatcherImpl
import org.sonarsource.kotlin.api.checks.INT_TYPE
import org.sonarsource.kotlin.api.checks.JAVA_STRING
import org.sonarsource.kotlin.api.checks.STRING_TYPE
import org.sonarsource.kotlin.api.checks.predictRuntimeValueExpression
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.frontend.TextRangeTracker
import org.sonarsource.kotlin.api.reporting.KotlinTextRanges.textRange
import org.sonarsource.kotlin.api.reporting.SecondaryLocation
import org.sonarsource.kotlin.api.visiting.withKaSession
import java.util.regex.Pattern
import org.sonarsource.kotlin.api.checks.isPlus as isConcat

val PATTERN_COMPILE_MATCHER = FunMatcher(qualifier = "java.util.regex.Pattern", name = "compile")
val REGEX_MATCHER = ConstructorMatcher(typeName = "kotlin.text.Regex")
val TO_REGEX_MATCHER = FunMatcher(qualifier = "kotlin.text", name = "toRegex", isExtensionFunction = true)

private fun argGetter(argIndex: Int) = { resolvedCall: KaFunctionCall<*> ->
    resolvedCall.argumentMapping.keys.elementAtOrNull(argIndex)
}

private val referenceTargetGetter = { resolvedCall: KaFunctionCall<*> ->
    when (val receiver = resolvedCall.partiallyAppliedSymbol.extensionReceiver) {
        is KaExplicitReceiverValue -> receiver.expression
        else -> null
    }
}

// TODO: add org.apache.commons.lang3.RegExUtils
private val REGEX_FUNCTIONS: Map<FunMatcherImpl, Pair<(KaFunctionCall<*>) -> KtExpression?, (KaFunctionCall<*>) -> KtExpression?>> = mapOf(
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
        resolvedCall: KaFunctionCall<*>,
        matchedFun: FunMatcherImpl,
        kotlinFileContext: KotlinFileContext
    ) {

        REGEX_FUNCTIONS[matchedFun]!!.let { (regexStringArgExtractor, flagsArgExtractor) ->
            val regexCtx = regexStringArgExtractor(resolvedCall)
                .collectResolvedListOfStringTemplates()
                // For now, we simply don't use any sequence that contains nulls (i.e. non-resolvable parts)
                .takeIf { null !in it }?.filterNotNull()
                ?.flatMap { it.entries.asSequence() }
                // We don't handle string interpolation for now. So we filter out all string templates with expressions
                ?.takeIf { it.all { entry -> entry !is KtStringTemplateEntryWithExpression } }
                ?.let { sourceTemplates ->
                    RegexContext(sourceTemplates.asIterable(), kotlinFileContext)
                } ?: return

            flagsArgExtractor(resolvedCall).extractRegexFlags()
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

private fun KtExpression?.extractRegexFlags(): FlagSet =
    FlagSet(
        this?.collectDescendantsOfType<KtReferenceExpression>()
            ?.map { it.predictRuntimeValueExpression() }
            ?.flatMap { it.collectDescendantsOfType<KtNameReferenceExpression>() }
            ?.mapNotNull { withKaSession { it.mainReference.resolveToSymbol()?.name?.asString() } }
            ?.mapNotNull { FLAGS[it] }
            ?.fold(0, Int::or)
            ?: 0
    )

private fun KtExpression?.collectResolvedListOfStringTemplates(): Sequence<KtStringTemplateExpression?> =
    this?.predictRuntimeValueExpression().let { predictedValue ->
        when {
            predictedValue is KtStringTemplateExpression -> sequenceOf(predictedValue)
            predictedValue is KtBinaryExpression && predictedValue.isConcat() ->
                predictedValue.left.collectResolvedListOfStringTemplates() +
                    predictedValue.right.collectResolvedListOfStringTemplates()
            else -> sequenceOf(null)
        }
    }
