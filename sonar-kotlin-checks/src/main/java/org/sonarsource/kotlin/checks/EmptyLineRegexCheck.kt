/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2024 SonarSource SA
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
@file:OptIn(KaExperimentalApi::class)

package org.sonarsource.kotlin.checks

import java.util.regex.Pattern
import java.util.stream.Collectors
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.resolution.KaExplicitReceiverValue
import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.analysis.api.resolution.successfulFunctionCallOrNull
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi2ir.deparenthesize
import org.sonar.check.Rule
import org.sonarsource.analyzer.commons.regex.RegexParseResult
import org.sonarsource.analyzer.commons.regex.ast.BoundaryTree
import org.sonarsource.analyzer.commons.regex.ast.NonCapturingGroupTree
import org.sonarsource.analyzer.commons.regex.ast.RegexBaseVisitor
import org.sonarsource.analyzer.commons.regex.ast.RegexTree
import org.sonarsource.analyzer.commons.regex.ast.SequenceTree
import org.sonarsource.kotlin.api.checks.*
import org.sonarsource.kotlin.api.regex.AbstractRegexCheck
import org.sonarsource.kotlin.api.regex.PATTERN_COMPILE_MATCHER
import org.sonarsource.kotlin.api.regex.REGEX_MATCHER
import org.sonarsource.kotlin.api.regex.RegexContext
import org.sonarsource.kotlin.api.regex.TO_REGEX_MATCHER
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.frontend.secondaryOf
import org.sonarsource.kotlin.api.visiting.analyze

private const val MESSAGE = "Remove MULTILINE mode or change the regex."

private val STRING_REPLACE = FunMatcher(qualifier = KOTLIN_TEXT) { withNames("replace", "replaceFirst") }
private val PATTERN_MATCHER = FunMatcher(qualifier = JAVA_UTIL_PATTERN, name = "matcher")
private val PATTERN_FIND = FunMatcher(qualifier = "java.util.regex.Matcher", name = "find")
private val STRING_IS_EMPTY = FunMatcher(qualifier = KOTLIN_TEXT, name = "isEmpty")
private val REGEX_FIND = FunMatcher(qualifier = "kotlin.text.Regex", name = "find")

@Rule(key = "S5846")
class EmptyLineRegexCheck : AbstractRegexCheck() {

    override val functionsToVisit = setOf(PATTERN_COMPILE_MATCHER, REGEX_MATCHER, TO_REGEX_MATCHER)

    override fun visitRegex(
        regex: RegexParseResult,
        regexContext: RegexContext,
        callExpression: KtCallExpression,
        matchedFun: FunMatcherImpl,
        kotlinFileContext: KotlinFileContext,
    ) {
        val visitor = EmptyLineMultilineVisitor()
        visitor.visit(regex)
        if (visitor.containEmptyLine) {
            val (secondaries, main) = when (matchedFun) {
                PATTERN_COMPILE_MATCHER -> getSecondariesForPattern(callExpression)
                REGEX_MATCHER -> {
                    checkRegexInReplace(callExpression, kotlinFileContext, callExpression.parent)
                    getSecondariesForRegex(callExpression)
                }
                TO_REGEX_MATCHER -> {
                    checkRegexInReplace(callExpression, kotlinFileContext, callExpression.parent.parent)
                    getSecondariesForToRegex(callExpression)
                }
                else -> emptyList<KtElement>() to null
            }
            if (main != null && secondaries.isNotEmpty()) {
                kotlinFileContext.reportIssue(
                    main,
                    MESSAGE,
                    secondaries.map { kotlinFileContext.secondaryOf(it, "This string can be empty") },
                )
            }
        }
    }

    private fun getSecondariesForPattern(callExpression: KtCallExpression): Pair<List<KtElement>, KtElement> =
        getSecondaries(callExpression.parent?.parent, ::getStringInMatcherFind) to callExpression.valueArguments[0]

    private fun getSecondariesForRegex(callExpression: KtCallExpression): Pair<List<KtElement>, KtElement> =
        getSecondaries(callExpression.parent, ::getStringInRegexFind) to callExpression.valueArguments[0]

    private fun getSecondariesForToRegex(
        callExpression: KtCallExpression
    ): Pair<List<KtElement>, KtElement?> = analyze {
        getSecondaries(callExpression.parent?.parent, ::getStringInRegexFind) to
                (callExpression.parent as? KtExpression)?.resolveToCall()
                    ?.successfulFunctionCallOrNull()?.getReceiverExpression()
    }

    private fun getSecondaries(
        parent: PsiElement?,
        findMapper: (KtElement) -> KtExpression?,
    ): List<KtElement> = when (parent) {
        is KtProperty -> {
            parent.findUsages()
                .mapNotNull { findMapper(it) }
                .filter { it.canBeEmpty() }
        }
        is KtDotQualifiedExpression -> {
            findMapper(parent.selectorExpression!!)
                ?.let { if (it.canBeEmpty()) listOf(it) else emptyList()} ?: emptyList()
        }
        else -> {
            emptyList()
        }
    }

    private fun checkRegexInReplace(
        callExpression: KtCallExpression,
        kotlinFileContext: KotlinFileContext,
        parent: PsiElement,
    ) {
        when (parent) {
            is KtProperty -> {
                parent.findUsages()
                    .mapNotNull { it.getParentCall() }
                    .firstOrNull {
                        it matches STRING_REPLACE && it.getReceiverExpression()?.canBeEmpty() == true
                    }
                    ?.let {
                        kotlinFileContext.reportIssue(
                            it.getFirstArgumentExpression()!!,
                            MESSAGE,
                            listOf(kotlinFileContext.secondaryOf(callExpression))
                        )
                    }
            }
            else -> {
                val resolvedCall = callExpression.getParentCall()
                if (
                    resolvedCall matches STRING_REPLACE
                    && resolvedCall?.getReceiverExpression()?.canBeEmpty() == true
                ) {
                    kotlinFileContext.reportIssue(resolvedCall.getFirstArgumentExpression()!!, MESSAGE)
                }
            }
        }
    }
}

private fun KaFunctionCall<*>?.getReceiverExpression(): KtExpression? {
    val partiallyAppliedSymbol = this?.partiallyAppliedSymbol
    val receiverValue = partiallyAppliedSymbol?.dispatchReceiver ?: partiallyAppliedSymbol?.extensionReceiver
    return when (receiverValue) {
        is KaExplicitReceiverValue -> receiverValue.expression
        else -> return null
    }
}

private class EmptyLineMultilineVisitor : RegexBaseVisitor() {
    var visitedStart = false
    var visitedEndAfterStart = false
    var containEmptyLine = false
    override fun visitSequence(tree: SequenceTree) {
        val items = tree.items.stream()
            .filter { item: RegexTree ->
                !isNonCapturingWithoutChild(item)
            }
            .collect(Collectors.toList())
        if (items.size == 1 && items[0].`is`(RegexTree.Kind.CAPTURING_GROUP)) {
            super.visitSequence(tree)
        } else if (items.size == 2 && items[0].`is`(RegexTree.Kind.BOUNDARY) && items[1].`is`(RegexTree.Kind.BOUNDARY)) {
            super.visitSequence(tree)
            containEmptyLine = containEmptyLine or visitedEndAfterStart
        }
        visitedStart = false
    }

    override fun visitBoundary(boundaryTree: BoundaryTree) {
        if (boundaryTree.activeFlags().contains(Pattern.MULTILINE)) {
            if (boundaryTree.type() == BoundaryTree.Type.LINE_START) {
                visitedStart = true
            } else if (boundaryTree.type() == BoundaryTree.Type.LINE_END) {
                visitedEndAfterStart = visitedStart
            }
        }
    }
}

private fun isNonCapturingWithoutChild(tree: RegexTree): Boolean {
    return tree.`is`(RegexTree.Kind.NON_CAPTURING_GROUP) && (tree as NonCapturingGroupTree).element == null
}

private fun getStringInMatcherFind(ref: KtElement): KtExpression? = analyze {
    val resolvedCall =
        (ref.parent as? KtExpression)?.resolveToCall()?.successfulFunctionCallOrNull() ?: return null

    if (!(resolvedCall matches PATTERN_MATCHER)) return null

    return when (val preParent = ref.parent.parent) {
        is KtExpression ->
            if (preParent.resolveToCall()?.successfulFunctionCallOrNull() matches PATTERN_FIND)
                extractArgument(resolvedCall)
            else null
        is KtProperty ->
            if (preParent.findUsages().any { it.getParentCall() matches PATTERN_FIND })
                extractArgument(resolvedCall)
            else null
        else -> null
    }
}

private fun getStringInRegexFind(ref: KtElement): KtExpression? {
    val resolvedCall = analyze {
        (ref.parent as? KtExpression)?.resolveToCall()?.successfulFunctionCallOrNull() ?: return null
    }
    return if (resolvedCall matches REGEX_FIND) extractArgument(resolvedCall) else null
}

private fun extractArgument(resolvedCall: KaFunctionCall<*>): KtExpression? {
    val arguments = resolvedCall.argumentMapping.keys.toList()
    return if (arguments.isEmpty()) null
    else arguments[0]
}

/**
 * This method checks if the expression can potentially contain an empty String value.
 *
 * This method returns true if the referenced expression is:
 *     - a variable with hardcoded (resolved) "" value
 *     - a variable, that was not tested with "isEmpty()" and could contain empty string
 *     - an empty String constant
 */
private fun KtExpression.canBeEmpty(): Boolean =
    when (val deparenthesized = this.deparenthesize()) {
        is KtNameReferenceExpression -> {
            val runtimeStringValue = deparenthesized.predictRuntimeStringValue()
            runtimeStringValue?.isEmpty()
                ?: deparenthesized.findUsages(allUsages = true) {
                    analyze {
                        (it.parent as? KtExpression)?.resolveToCall()
                            ?.successfulFunctionCallOrNull() matches STRING_IS_EMPTY ||
                                (it.parent as? KtBinaryExpression).isEmptinessCheck()
                    }
                }.isEmpty()
        }
        else ->
            predictRuntimeStringValue()?.isEmpty() ?: false
    }

private fun KtBinaryExpression?.isEmptinessCheck() =
    this?.let {
        operationToken in setOf(KtTokens.EQEQ, KtTokens.EXCLEQ) &&
            (left?.predictRuntimeStringValue()?.isEmpty() ?: false
                || right?.predictRuntimeStringValue()?.isEmpty() ?: false)
    } ?: false

fun KtElement.getParentCall(): KaFunctionCall<*>? {
    val callExpressionTypes = arrayOf(
        KtSimpleNameExpression::class.java, KtCallElement::class.java, KtBinaryExpression::class.java,
        KtUnaryExpression::class.java, KtArrayAccessExpression::class.java
    )

    val parent = PsiTreeUtil.getParentOfType(this, *callExpressionTypes)

    return analyze { parent?.resolveToCall()?.successfulFunctionCallOrNull() }
}
