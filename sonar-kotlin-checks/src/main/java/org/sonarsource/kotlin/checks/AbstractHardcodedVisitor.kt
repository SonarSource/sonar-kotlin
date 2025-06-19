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
package org.sonarsource.kotlin.checks

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinFileContext


abstract class AbstractHardcodedVisitor : AbstractCheck() {

    abstract val sensitiveVariableKind: String

    abstract val sensitiveWords: String

    private var variablePatterns: Sequence<Regex>? = null
    private var literalPatterns: Sequence<Regex>? = null

    companion object {
        private fun isQuery(value: String, match: String): Boolean {
            val followingString = value.substring(value.indexOf(match) + match.length)
            return (followingString.startsWith("=?")
                    || followingString.startsWith("=%")
                    || followingString.startsWith("=:")
                    || followingString.startsWith("={") // string format
                    || followingString == "='")
        }
    }

    override fun visitBinaryExpression(expression: KtBinaryExpression, context: KotlinFileContext) {
        if (expression.operationToken == KtTokens.EQ || expression.operationToken == KtTokens.PLUSEQ) {
            val left = expression.left
            left?.identifier()?.let { checkVariable(context, left, it, expression.right!!) }
        }
    }

    override fun visitProperty(property: KtProperty, context: KotlinFileContext) {
        property.initializer?.let {
            checkVariable(context, property.nameIdentifier!!, property.name!!, it)
        }
    }

    override fun visitStringTemplateExpression(expression: KtStringTemplateExpression, context: KotlinFileContext) {
        val content = if (!expression.hasInterpolation()) expression.asConstant() else ""
        literalPatterns()
            .mapNotNull { regex -> regex.find(content) }
            .filter { matchResult -> matchResult.groups.size > 2 }
            .filter { matchResult -> isSensitiveStringLiteral(matchResult.groups[2]!!.value) }
            .map { matchResult -> matchResult.groups[1]!!.value }
            .filter { match: String -> !isQuery(content, match) }
            .forEach { credential: String ->
                context.report(expression, credential)
            }
    }

    private fun KtElement.isSensitive() = this is KtStringTemplateExpression
            && !this.hasInterpolation()
            && isSensitiveStringLiteral(this.asConstant())

    open fun isSensitiveStringLiteral(value: String): Boolean {
        return value.isNotEmpty()
    }

    private fun KotlinFileContext.report(tree: PsiElement, matchName: String) {
        reportIssue(tree, """"$matchName" detected here, make sure this is not a hard-coded $sensitiveVariableKind.""")
    }

    private fun KotlinFileContext.checkAssignedValue(
        matchResult: MatchResult,
        regex: Regex,
        leftHand: PsiElement,
        value: String
    ) {
        if (!regex.containsMatchIn(value)) {
            report(leftHand, matchResult.groups[1]!!.value)
        }
    }

    private fun KtExpression.identifier(): String? = when (this) {
        is KtNameReferenceExpression -> getReferencedName()
        is KtDotQualifiedExpression -> selectorExpression?.identifier()
        else -> null
    }

    private fun checkVariable(ctx: KotlinFileContext, variable: PsiElement, variableName: String, value: KtElement) {
        if (value.isSensitive()) {
            variablePatterns()
                .mapNotNull { regex -> regex.find(variableName)?.let { it to regex } }
                .forEach { (matcher, regex) ->
                    ctx.checkAssignedValue(
                        matcher,
                        regex,
                        variable,
                        (value as KtStringTemplateExpression).asConstant()
                    )
                }
        }
    }

    private fun variablePatterns() = variablePatterns ?: toPatterns("").also { variablePatterns = it }

    private fun literalPatterns() = literalPatterns ?: toPatterns("""=([^\s&]+)""").also { literalPatterns = it }

    private fun toPatterns(suffix: String): Sequence<Regex> {
        return sensitiveWords.split(",").toTypedArray()
            .asSequence()
            .map { obj: String -> obj.trim { it <= ' ' } }
            .map { word: String -> Regex("($word)$suffix", RegexOption.IGNORE_CASE) }
    }
}
