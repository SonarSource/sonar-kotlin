/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2024 SonarSource SA
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

import org.jetbrains.kotlin.lexer.KtTokens.EQEQ
import org.jetbrains.kotlin.lexer.KtTokens.EXCLEQ
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtSafeQualifiedExpression
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.isNull
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.CallAbstractCheck
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.checks.findClosestAncestorOfType
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.reporting.Message
import org.sonarsource.kotlin.api.reporting.message

/**
 * This rule reports an issue when the pattern of using `find(predicate)`, `findLast(predicate)`, `firstOrNull(predicate)`,
 * and `lastOrNull(predicate)` from the `kotlin.collections` package, combined with a null check is detected.
 *
 * The rule suggests to replace the pattern with `any(predicate)`, `none(predicate)`, and `contains(element)` depending on the case.
 */
@Rule(key = "S6528")
class UnsuitedFindFunctionWithNullComparisonCheck : CallAbstractCheck() {

    override val functionsToVisit = listOf(FunMatcher(qualifier = "kotlin.collections") {
        withNames("find", "findLast", "firstOrNull", "lastOrNull")
        withArguments("kotlin.Function1")
    })

    override fun visitFunctionCall(callExpression: KtCallExpression, resolvedCall: ResolvedCall<*>, kotlinFileContext: KotlinFileContext) {
        callExpression.findClosestAncestorOfType<KtBinaryExpression>()?.takeIf { it.right!!.isNull() || it.left!!.isNull() }
            ?.let { report(it, callExpression, kotlinFileContext) }
    }

    private fun report(nullComparisonExpr: KtBinaryExpression, callExpression: KtCallExpression, kotlinFileContext: KotlinFileContext) {
        // callExpression has argument a lambda expression with a single parameter, due to the functionsToVisit FunMatchers
        val lambda = callExpression.valueArguments[0].getArgumentExpression() as KtLambdaExpression

        // Note that lambda.bodyExpression!!.children[0] and lambda.bodyExpression!!.firstChild behave differently in case of a block body
        // when there is a comment before the expression, the firstChild is the comment, while children[0] is the expression
        val lambdaBinaryExpr = lambda.bodyExpression!!.children[0] as? KtBinaryExpression
        // the lambda either has the implicit "it" parameter or a single parameter
        val lambdaParameterName = if (lambda.valueParameters.isEmpty()) "it" else lambda.valueParameters[0].name!!

        // the functionsToVisit can be applied directly on the collection, or indirectly, for example using "with(collection)"
        val qualifiedExprBeforeFind =
            callExpression.findClosestAncestorOfType<KtQualifiedExpression>(stopCondition = { it is KtBinaryExpression })
        val qualifiedExpressionAccessor = if (qualifiedExprBeforeFind is KtSafeQualifiedExpression) "?." else "."
        // in case the function is called on the collection directly, we build the replacement on the qualified expression before the call
        val beforeFindTxt = qualifiedExprBeforeFind?.receiverExpression?.text?.plus(qualifiedExpressionAccessor) ?: ""

        // case in which the lambda predicate is an equality check: "it == something" or "x -> x == something"
        if (lambdaBinaryExpr != null && lambdaBinaryExpr.isSingleEquality() && lambdaBinaryExpr.references(lambdaParameterName)) {
            val negateContains = (lambdaBinaryExpr.operationToken == EQEQ) xor (nullComparisonExpr.operationToken == EXCLEQ)
            val elementTxt = lambdaBinaryExpr.nonParameterOperand(lambdaParameterName).text
            val replacementExpr = (if (negateContains) "!" else "") + "${beforeFindTxt}contains($elementTxt)"

            // suggest the replacement only if the message is not too long and does not contain a line break
            val message = if (replacementExpr.isVerbose()) message("contains") else message(replacementExpr)

            kotlinFileContext.reportIssue(nullComparisonExpr, message)
        } else {
            val isAnyReplacement = nullComparisonExpr.operationToken == EXCLEQ
            val anyOrNone = if (isAnyReplacement) "any" else "none"
            val replacementExpr = "${beforeFindTxt}${anyOrNone} ${lambda.text}"

            // suggest the replacement only if the message is not too long and does not contain a line break
            val message = if (replacementExpr.isVerbose()) message(anyOrNone) else message(replacementExpr)

            kotlinFileContext.reportIssue(nullComparisonExpr, message)
        }
    }

    private fun message(replacementExpr: String): Message = message {
        +"Replace with "
        code(replacementExpr)
        +"."
    }
}

// checks that the binary expression has no binary expression as child, and it is an equality comparison
private fun KtBinaryExpression.isSingleEquality(): Boolean =
    this.getChildOfType<KtBinaryExpression>() == null && this.operationToken == EQEQ

private fun KtBinaryExpression.references(reference: String): Boolean =
    this.left!!.references(reference) || this.right!!.references(reference)

private fun KtBinaryExpression.nonParameterOperand(parameter: String): KtExpression =
    if (!this.left!!.references(parameter)) this.left!! else this.right!!

private fun KtExpression.references(reference: String): Boolean =
    this.let { it as? KtNameReferenceExpression }?.getReferencedName() == reference

private fun String.isVerbose(): Boolean = this.length >= 40 || this.contains("\\n")