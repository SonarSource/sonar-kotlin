/*
 * SonarSource SLang
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
package org.sonarsource.kotlin.checks

import java.net.URI
import java.net.URISyntaxException
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.sonar.check.Rule
import org.sonar.check.RuleProperty
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.plugin.KotlinFileContext

/**
 * Replacement for [org.sonarsource.slang.checks.HardcodedCredentialsCheck]
 */
@Rule(key = "S2068")
class HardcodedCredentialsCheck : AbstractCheck() {
    @RuleProperty(key = "credentialWords",
        description = "Comma separated list of words identifying potential credentials",
        defaultValue = DEFAULT_VALUE)
    var credentialWords = DEFAULT_VALUE
    private var variablePatterns: Sequence<Regex>? = null
    private var literalPatterns: Sequence<Regex>? = null

    companion object {
        private const val DEFAULT_VALUE = "password,passwd,pwd,passphrase"
        private val URI_PREFIX = Regex("^\\w{1,8}://")

        private fun isURIWithCredentials(stringLiteral: String): Boolean {
            if (URI_PREFIX.containsMatchIn(stringLiteral)) {
                try {
                    val userInfo = URI(stringLiteral).userInfo
                    if (userInfo != null) {
                        val parts = userInfo.split(":").toTypedArray()
                        return (parts.size > 1 && parts[0] != parts[1]) && !(parts.size == 2 && parts[1].isEmpty())
                    }
                } catch (e: URISyntaxException) {
                    // ignore, stringLiteral is not a valid URI
                }
            }
            return false
        }

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
        if (isURIWithCredentials(content)) {
            context.reportIssue(expression, "Review this hard-coded URL, which may contain a credential.")
        } else {
            literalPatterns()
                .mapNotNull { regex -> regex.find(content) }
                .map { matchResult -> matchResult.groups[1]!!.value }
                .filter { match: String -> !isQuery(content, match) }
                .forEach { credential: String -> context.report(expression, credential) }
        }
    }

    private fun KtElement.isNotEmptyString() = this is KtStringTemplateExpression
        && !this.hasInterpolation()
        && this.asConstant().isNotEmpty()

    private fun KotlinFileContext.report(tree: PsiElement, matchName: String) {
        reportIssue(tree, "\"$matchName\" detected here, make sure this is not a hard-coded credential.")
    }

    private fun KotlinFileContext.checkAssignedValue(matchResult: MatchResult, regex: Regex, leftHand: PsiElement, value: String) {
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
        if (value.isNotEmptyString()) {
            variablePatterns()
                .mapNotNull { regex -> regex.find(variableName)?.let { it to regex } }
                .forEach { (matcher, regex) ->
                    ctx.checkAssignedValue(
                        matcher,
                        regex,
                        variable,
                        (value as KtStringTemplateExpression).asConstant())
                }
        }
    }

    private fun variablePatterns() = variablePatterns ?: toPatterns("")

    private fun literalPatterns() = literalPatterns ?: toPatterns("""=\S""")

    private fun toPatterns(suffix: String): Sequence<Regex> {
        return credentialWords.split(",").toTypedArray()
            .asSequence()
            .map { obj: String -> obj.trim { it <= ' ' } }
            .map { word: String -> Regex("($word)$suffix", RegexOption.IGNORE_CASE) }
    }
}
