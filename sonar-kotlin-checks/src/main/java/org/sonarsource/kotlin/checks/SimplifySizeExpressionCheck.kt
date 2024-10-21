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

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.lexer.KtTokens.ANDAND_Id
import org.jetbrains.kotlin.lexer.KtTokens.EQEQ_Id
import org.jetbrains.kotlin.lexer.KtTokens.EXCLEQ_Id
import org.jetbrains.kotlin.lexer.KtTokens.EXCL_Id
import org.jetbrains.kotlin.lexer.KtTokens.GT_Id
import org.jetbrains.kotlin.lexer.KtTokens.LT_Id
import org.jetbrains.kotlin.lexer.KtTokens.OROR_Id
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtPrefixExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtUnaryExpression
import org.jetbrains.kotlin.psi.psiUtil.isNull
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.CallAbstractCheck
import org.sonarsource.kotlin.api.checks.FieldMatcher
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.checks.FunMatcherImpl
import org.sonarsource.kotlin.api.checks.predictRuntimeIntValue
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

private const val PACKAGE_KOTLIN_COLLECTION = "kotlin.collections"
private const val PACKAGE_KOTLIN_TEXT = "kotlin.text"

private const val INTERFACE_KOTLIN_COLLECTION = "$PACKAGE_KOTLIN_COLLECTION.Collection"
private const val INTERFACE_KOTLIN_MAP = "$PACKAGE_KOTLIN_COLLECTION.Map"

private val countMatcher = FunMatcher {
    withNames("count")
    withDefiningSupertypes(PACKAGE_KOTLIN_COLLECTION, PACKAGE_KOTLIN_TEXT)
    withNoArguments()
}

private val isEmptyMatcher = FunMatcher {
    withNames("isEmpty")
    withDefiningSupertypes(INTERFACE_KOTLIN_COLLECTION, INTERFACE_KOTLIN_MAP, PACKAGE_KOTLIN_TEXT)
    withNoArguments()
}

private val isNotEmptyMatcher = FunMatcher {
    withNames("isNotEmpty")
    withDefiningSupertypes(PACKAGE_KOTLIN_COLLECTION, PACKAGE_KOTLIN_TEXT)
    withNoArguments()
}

private val sizeFieldMatcher = FieldMatcher {
    withNames("size")
    withDefiningTypes(INTERFACE_KOTLIN_COLLECTION, INTERFACE_KOTLIN_MAP)
}

private val lengthFieldMatcher = FieldMatcher {
    withNames("length")
    withQualifiers("kotlin.String")
}

@org.sonarsource.kotlin.api.frontend.K1only("predict")
@Rule(key = "S6529")
class SimplifySizeExpressionCheck : CallAbstractCheck() {

    override val functionsToVisit = listOf(
        countMatcher, isEmptyMatcher, isNotEmptyMatcher
    )

    // TODO easy?
    override fun visitFunctionCall(
        callExpression: KtCallExpression,
        resolvedCall: ResolvedCall<*>,
        matchedFun: FunMatcherImpl,
        kotlinFileContext: KotlinFileContext,
    ) {
        if (matchedFun == countMatcher) {
            checkSizeTest(callExpression, kotlinFileContext)
        } else {
            checkIsEmptyTest(callExpression, matchedFun == isEmptyMatcher, kotlinFileContext)
        }
    }

    override fun visitReferenceExpression(
        expression: KtReferenceExpression,
        kotlinFileContext: KotlinFileContext,
    ) {
        if (expression is KtNameReferenceExpression && (sizeFieldMatcher.matches(expression, kotlinFileContext.bindingContext) ||
                lengthFieldMatcher.matches(expression, kotlinFileContext.bindingContext))
        ) {
            checkSizeTest(expression, kotlinFileContext)
        }
    }

    private fun checkSizeTest(
        expression: KtExpression,
        kotlinFileContext: KotlinFileContext,
    ) {
        val sizeTest = expandOptionalQualifier(expression.parent).skipParentParentheses() as? KtBinaryExpression ?: return
        val operationTokenId = sizeTest.getOperationTokenId()
        val isSizeEquals = when (operationTokenId) {
            EQEQ_Id -> true
            EXCLEQ_Id, GT_Id, LT_Id -> false
            else -> return
        }

        val bindingContext = kotlinFileContext.bindingContext
        if (((operationTokenId != LT_Id) && isIntZeroLiteral(sizeTest.right, bindingContext)) ||
            ((operationTokenId != GT_Id) && isIntZeroLiteral(sizeTest.left, bindingContext))
        ) {
            checkNullTestOrReportSize(sizeTest, isSizeEquals, true, expression.parent as? KtDotQualifiedExpression, kotlinFileContext)
        }
    }

    private fun checkIsEmptyTest(expression: KtCallExpression, isIsEmpty: Boolean, kotlinFileContext: KotlinFileContext) {
        var isEmptyTest = expandOptionalQualifier(expression.parent).skipParentParentheses() ?: return
        val isNegated = if (isEmptyTest is KtPrefixExpression) {
            val operationTokenId = isEmptyTest.getOperationTokenId()
            operationTokenId == EXCL_Id
        } else {
            val endExclusive = isEmptyTest
            isEmptyTest = expression
            while (isEmptyTest.parent != endExclusive) isEmptyTest = isEmptyTest.parent
            false
        }
        checkNullTestOrReportSize(
            isEmptyTest,
            isIsEmpty != isNegated,
            isNegated,
            expression.parent as? KtDotQualifiedExpression,
            kotlinFileContext
        )
    }

    private fun checkNullTestOrReportSize(
        sizeTest: PsiElement,
        useIsEmpty: Boolean,
        doReportSize: Boolean,
        sizeTestQualifier: KtDotQualifiedExpression?,
        kotlinFileContext: KotlinFileContext,
    ) {
        if (checkNullTest(sizeTest, useIsEmpty, sizeTestQualifier, kotlinFileContext) || !doReportSize) return

        val replaceFunction = if (useIsEmpty) "isEmpty()" else "isNotEmpty()"
        kotlinFileContext.reportIssue(sizeTest, "Replace collection size check with \"$replaceFunction\"")
    }

    private fun checkNullTest(
        sizeTest: PsiElement,
        useIsEmpty: Boolean,
        sizeTestQualifier: KtDotQualifiedExpression?,
        kotlinFileContext: KotlinFileContext,
    ): Boolean {
        val sizeTestReceiver = sizeTestQualifier?.receiverExpression as? KtNameReferenceExpression ?: return false

        val connectorExpression = sizeTest.parent.skipParentParentheses() as? KtBinaryExpression ?: return false
        val isOrConnector = when (connectorExpression.getOperationTokenId()) {
            ANDAND_Id -> false
            OROR_Id -> true
            else -> return false
        }
        if (isOrConnector != useIsEmpty) return false

        val nullTest = connectorExpression.left?.skipParentheses() as? KtBinaryExpression ?: return false
        val isEqualsNull = when (nullTest.getOperationTokenId()) {
            EQEQ_Id -> true
            EXCLEQ_Id -> false
            else -> return false
        }
        if (isEqualsNull != useIsEmpty) return false

        if (getNullTestReference(nullTest)?.getReferencedName() != sizeTestReceiver.getReferencedName()) return false

        val replaceFunction = if (useIsEmpty) "isNullOrEmpty()" else "!isNullOrEmpty()"
        kotlinFileContext.reportIssue(connectorExpression, "Replace null check and collection size check with \"$replaceFunction\"")
        return true
    }
}

private fun getNullTestReference(expression: KtBinaryExpression): KtNameReferenceExpression? =
    getNullTestReference(expression.left, expression.right) ?: getNullTestReference(expression.right, expression.left)

private fun getNullTestReference(referenceExpression: KtExpression?, nullExpression: KtExpression?): KtNameReferenceExpression? =
    if (nullExpression?.isNull() == true) referenceExpression as? KtNameReferenceExpression else null

private fun expandOptionalQualifier(element: PsiElement): PsiElement =
    if (element is KtDotQualifiedExpression) element.parent else element

private fun isIntZeroLiteral(expression: KtExpression?, bindingContext: BindingContext) =
    (expression as? KtConstantExpression)?.predictRuntimeIntValue(bindingContext) == 0

private fun KtExpression.getOperationTokenId() = ((when (this) {
    is KtUnaryExpression -> operationToken
    is KtBinaryExpression -> operationToken
    else -> null
}) as? KtToken)?.tokenId
