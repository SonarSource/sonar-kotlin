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

import org.jetbrains.kotlin.lexer.KtTokens.EQEQ
import org.jetbrains.kotlin.lexer.KtTokens.EXCLEQ
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtOperationExpression
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.isNull
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.CallAbstractCheck
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

/**
 * This rule reports an issue when the pattern of using `find(predicate)` or `findLast(predicate)`,
 * from the `kotlin.collections` package, combined with a null check is detected.
 *
 * The rule suggests to replace the pattern with `any(predicate)`, `none(predicate)`, and `contains(element)` depending on the case.
 */
@Rule(key = "S6528")
class UnsuitedFindFunctionWithNullComparisonCheck : CallAbstractCheck() {

    override val functionsToVisit = listOf(
        FunMatcher(qualifier = "kotlin.collections") {
            withNames("find", "findLast")
            withArguments("kotlin.Function1")
        }
    )

    override fun visitFunctionCall(callExpression: KtCallExpression, resolvedCall: ResolvedCall<*>, kotlinFileContext: KotlinFileContext) {
        callExpression.parent.parent
            ?.takeIf { it is KtOperationExpression }
            ?.let { it as KtBinaryExpression }
            ?.takeIf { it.right is KtConstantExpression && it.right!!.isNull() }
            ?.let { report(it, callExpression, kotlinFileContext) }
    }

    private fun report(nullComparisonExpr: KtBinaryExpression, findCallExpr: KtCallExpression, kotlinFileContext: KotlinFileContext) {
        // findCallExpr is a call to find or findLast, having as argument a lambda expression with a single parameter
        val lambda = findCallExpr.valueArguments[0].getArgumentExpression() as KtLambdaExpression

        // lambda has a binary expression as body
        val lambdaBinaryExpr = lambda.binaryExpression()
        val lambdaParameterName = if (lambda.valueParameters.isEmpty()) "it" else lambda.valueParameters[0].name

        // the case in which the lambda predicate checks for equality among the collection elements
        // meaning that the binary expressions has one operand that matches the lambda parameter name (or the implicit parameter "it")
        if (lambdaBinaryExpr != null
            && lambdaBinaryExpr.isSingleEqualityBinaryExpression() && lambdaBinaryExpr.references(lambdaParameterName!!)) {

            // the element to be found is the binary expression operand that is not the lambda parameter
            val element = lambdaBinaryExpr.nonParameterOperand(lambdaParameterName)
            val expressionBeforeFind = (findCallExpr.parent as KtDotQualifiedExpression).receiverExpression
            val negateContains = (lambdaBinaryExpr.operationToken == EQEQ) xor (nullComparisonExpr.operationToken == EXCLEQ)
            val replacementExpr = (if (negateContains) "!" else "") + "${expressionBeforeFind.text}.contains(${element.text})"

            kotlinFileContext.reportIssue(nullComparisonExpr, message(nullComparisonExpr, replacementExpr))
        } else {
            val expressionBeforeFind = (findCallExpr.parent as KtDotQualifiedExpression).receiverExpression
            val isAnyReplacement = nullComparisonExpr.operationToken == EXCLEQ
            val replacementExpr = "${expressionBeforeFind.text}.${if (isAnyReplacement) "any" else "none"} ${lambda.text}"

            kotlinFileContext.reportIssue(nullComparisonExpr, message(nullComparisonExpr, replacementExpr))
        }
    }

    private fun KtLambdaExpression.binaryExpression(): KtBinaryExpression? {
        return this.bodyExpression
            .takeIf { it is KtBlockExpression }
            ?.firstChild
            ?.takeIf { it is KtBinaryExpression }
            ?.let { it as KtBinaryExpression }
    }

    // checks that the binary expression has no binary expression as child, and it is an equality comparison
    private fun KtBinaryExpression.isSingleEqualityBinaryExpression(): Boolean =
        this.getChildOfType<KtBinaryExpression>() == null
            && (this.operationToken == EQEQ || this.operationToken == EXCLEQ)

    private fun KtBinaryExpression.references(reference: String): Boolean =
        this.left!!.references(reference) || this.right!!.references(reference)

    private fun KtBinaryExpression.nonParameterOperand(parameter: String): KtExpression =
        if (!this.left!!.references(parameter)) this.left!! else this.right!!

    private fun KtExpression.references(reference: String): Boolean =
        this.takeIf { it is KtNameReferenceExpression }?.let { it as KtNameReferenceExpression }?.text == reference

    private fun message(nullComparisonExpr: KtBinaryExpression, replacementExpr: String): String =
        "Replace \"${nullComparisonExpr.text}\" with \"$replacementExpr\"."
}
