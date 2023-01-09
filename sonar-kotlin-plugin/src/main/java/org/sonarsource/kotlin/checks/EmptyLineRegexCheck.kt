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
package org.sonarsource.kotlin.checks

import java.util.regex.Pattern
import java.util.stream.Collectors
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi2ir.deparenthesize
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.util.getFirstArgumentExpression
import org.jetbrains.kotlin.resolve.calls.util.getParentResolvedCall
import org.jetbrains.kotlin.resolve.calls.util.getReceiverExpression
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.sonar.check.Rule
import org.sonarsource.analyzer.commons.regex.RegexParseResult
import org.sonarsource.analyzer.commons.regex.ast.BoundaryTree
import org.sonarsource.analyzer.commons.regex.ast.NonCapturingGroupTree
import org.sonarsource.analyzer.commons.regex.ast.RegexBaseVisitor
import org.sonarsource.analyzer.commons.regex.ast.RegexTree
import org.sonarsource.analyzer.commons.regex.ast.SequenceTree
import org.sonarsource.kotlin.api.FunMatcher
import org.sonarsource.kotlin.api.FunMatcherImpl
import org.sonarsource.kotlin.api.JAVA_UTIL_PATTERN
import org.sonarsource.kotlin.api.KOTLIN_TEXT
import org.sonarsource.kotlin.api.findUsages
import org.sonarsource.kotlin.api.matches
import org.sonarsource.kotlin.api.predictRuntimeStringValue
import org.sonarsource.kotlin.api.regex.AbstractRegexCheck
import org.sonarsource.kotlin.api.regex.PATTERN_COMPILE_MATCHER
import org.sonarsource.kotlin.api.regex.REGEX_MATCHER
import org.sonarsource.kotlin.api.regex.RegexContext
import org.sonarsource.kotlin.api.regex.TO_REGEX_MATCHER
import org.sonarsource.kotlin.api.secondaryOf
import org.sonarsource.kotlin.plugin.KotlinFileContext

private const val MESSAGE = "Remove MULTILINE mode or change the regex."

private val STRING_REPLACE = FunMatcher(qualifier = KOTLIN_TEXT, names = setOf("replace", "replaceFirst"))
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
                PATTERN_COMPILE_MATCHER -> getSecondariesForPattern(callExpression, kotlinFileContext.bindingContext)
                REGEX_MATCHER -> {
                    checkRegexInReplace(callExpression, kotlinFileContext, callExpression.parent)
                    getSecondariesForRegex(callExpression, kotlinFileContext.bindingContext)
                }
                TO_REGEX_MATCHER -> {
                    checkRegexInReplace(callExpression, kotlinFileContext, callExpression.parent.parent)
                    getSecondariesForToRegex(callExpression, kotlinFileContext.bindingContext)
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

    private fun getSecondariesForPattern(callExpression: KtCallExpression, bindingContext: BindingContext): Pair<List<KtElement>, KtElement> =
        getSecondaries(bindingContext, callExpression.parent?.parent, ::getStringInMatcherFind) to callExpression.valueArguments[0]

    private fun getSecondariesForRegex(callExpression: KtCallExpression, bindingContext: BindingContext): Pair<List<KtElement>, KtElement> =
        getSecondaries(bindingContext, callExpression.parent, ::getStringInRegexFind) to callExpression.valueArguments[0]

    private fun getSecondariesForToRegex(callExpression: KtCallExpression, bindingContext: BindingContext): Pair<List<KtElement>, KtElement?> =
        getSecondaries(bindingContext, callExpression.parent?.parent, ::getStringInRegexFind) to
            (callExpression.parent as? KtExpression)?.getResolvedCall(bindingContext)?.getReceiverExpression()

    private fun getSecondaries(
        bindingContext: BindingContext,
        parent: PsiElement?,
        findMapper: (KtElement, BindingContext) -> KtExpression?,
    ): List<KtElement> = when (parent) {
        is KtProperty -> {
            parent.findUsages()
                .mapNotNull { findMapper(it, bindingContext) }
                .filter { it.canBeEmpty(bindingContext) }
        }
        is KtDotQualifiedExpression -> {
            findMapper(parent.selectorExpression!!, bindingContext)
                ?.let { if (it.canBeEmpty(bindingContext)) listOf(it) else emptyList()} ?: emptyList()
        }
        else -> {
            emptyList()
        }
    }

    private fun checkRegexInReplace(callExpression: KtCallExpression, kotlinFileContext: KotlinFileContext, parent: PsiElement) {
        val bindingContext = kotlinFileContext.bindingContext
        when (parent) {
            is KtProperty -> {
                parent.findUsages()
                    .mapNotNull { it.getParentResolvedCall(bindingContext) }
                    .firstOrNull {
                        it matches STRING_REPLACE && it.getReceiverExpression()?.canBeEmpty(bindingContext) == true
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
                val resolvedCall = callExpression.getParentResolvedCall(bindingContext)
                if (
                    resolvedCall matches STRING_REPLACE
                    && resolvedCall?.getReceiverExpression()?.canBeEmpty(bindingContext) == true
                ) {
                    kotlinFileContext.reportIssue(resolvedCall.getFirstArgumentExpression()!!, MESSAGE)
                }
            }
        }
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

private fun getStringInMatcherFind(ref: KtElement, bindingContext: BindingContext): KtExpression? {
    val resolvedCall = (ref.parent as? KtExpression)?.getResolvedCall(bindingContext) ?: return null
    if (!(resolvedCall matches PATTERN_MATCHER)) return null

    return when (val preParent = ref.parent.parent) {
        is KtExpression -> if (preParent.getResolvedCall(bindingContext) matches PATTERN_FIND) extractArgument(resolvedCall) else null
        is KtProperty -> if (preParent.findUsages().any { it.getParentResolvedCall(bindingContext) matches PATTERN_FIND }) {
            extractArgument(resolvedCall)
        } else null
        else -> null
    }
}

private fun getStringInRegexFind(ref: KtElement, bindingContext: BindingContext): KtExpression? {
    val resolvedCall = (ref.parent as? KtExpression)?.getResolvedCall(bindingContext) ?: return null
    return if (resolvedCall matches REGEX_FIND) extractArgument(resolvedCall) else null
}

private fun extractArgument(resolvedCall: ResolvedCall<out CallableDescriptor>): KtExpression? {
    val arguments = resolvedCall.valueArgumentsByIndex
    return if (arguments == null
        || arguments.isEmpty()
        || arguments[0] == null
        || arguments[0] !is ExpressionValueArgument
    ) null
    else (arguments[0] as ExpressionValueArgument).valueArgument?.getArgumentExpression()
}

/**
 * This method checks if the expression can potentially contain an empty String value.
 *
 * This method returns true if the referenced expression is:
 *     - a variable with hardcoded (resolved) "" value
 *     - a variable, that was not tested with "isEmpty()" and could contain empty string
 *     - an empty String constant
 */
private fun KtExpression.canBeEmpty(bindingContext: BindingContext): Boolean =
    when (val deparenthesized = this.deparenthesize()) {
        is KtNameReferenceExpression -> {
            val runtimeStringValue = deparenthesized.predictRuntimeStringValue(bindingContext)
            runtimeStringValue?.isEmpty()
                ?: deparenthesized.findUsages(allUsages = true) {
                    (it.parent as? KtExpression)?.getResolvedCall(bindingContext) matches STRING_IS_EMPTY ||
                        (it.parent as? KtBinaryExpression).isEmptinessCheck(bindingContext)
                }.isEmpty()
        }
        else ->
            predictRuntimeStringValue(bindingContext)?.isEmpty() ?: false
    }

private fun KtBinaryExpression?.isEmptinessCheck(bindingContext: BindingContext) =
    this?.let {
        operationToken in setOf(KtTokens.EQEQ, KtTokens.EXCLEQ) &&
            (left?.predictRuntimeStringValue(bindingContext)?.isEmpty() ?: false
                || right?.predictRuntimeStringValue(bindingContext)?.isEmpty() ?: false)
    } ?: false
