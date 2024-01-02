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

import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtIsExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtPrefixExpression
import org.jetbrains.kotlin.psi.KtThrowExpression
import org.jetbrains.kotlin.psi.KtWhenConditionWithExpression
import org.jetbrains.kotlin.psi.KtWhenEntry
import org.jetbrains.kotlin.psi.psiUtil.isNull
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.CallAbstractCheck
import org.sonarsource.kotlin.api.checks.ConstructorMatcher
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.checks.FunMatcherImpl
import org.sonarsource.kotlin.api.reporting.message
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

private val ILLEGAL_STATE_EXCEPTION_CONSTRUCTOR_MATCH = ConstructorMatcher(typeName = "java.lang.IllegalStateException") {
    withNoArguments()
    withArguments("kotlin.String")
}

private val ILLEGAL_ARGUMENT_EXCEPTION_CONSTRUCTOR_MATCH = ConstructorMatcher(typeName = "java.lang.IllegalArgumentException") {
    withNoArguments()
    withArguments("kotlin.String")
}

@Rule(key = "S6532")
class SimplifiedPreconditionsCheck : CallAbstractCheck() {

    override val functionsToVisit: Iterable<FunMatcherImpl> = listOf(
        FunMatcher(qualifier = "kotlin", name = "check") {
            withArguments("kotlin.Boolean")
        },
        FunMatcher(qualifier = "kotlin", name = "require") {
            withArguments("kotlin.Boolean")
        }
    )

    override fun visitFunctionCall(callExpression: KtCallExpression, resolvedCall: ResolvedCall<*>, matchedFun: FunMatcherImpl, kotlinFileContext: KotlinFileContext) {
        val conditionExpression = callExpression.valueArguments.first().getArgumentExpression()

        if (conditionExpression.isNullCheckCondition(KtTokens.EXCLEQ)) {
            // this call is matched because we override functionsToVisit
            val functionCall = callExpression.calleeExpression!!
            val nullCheckVariable = (conditionExpression as KtBinaryExpression).getNullCheckVariable()
            val replaceCall = callNotNull(functionCall.text, nullCheckVariable)
            val message = buildReportMessage(statementToReplace = functionCall.text, statememntType = "function call", replacementCode = replaceCall)
            kotlinFileContext.reportIssue(psiElement = functionCall, message = message)
        }
    }

    override fun visitThrowExpression(throwExpression: KtThrowExpression, kotlinFileContext: KotlinFileContext) {
        val bindingContext = kotlinFileContext.bindingContext

        when {
            throwExpression.matchesException(bindingContext, ILLEGAL_STATE_EXCEPTION_CONSTRUCTOR_MATCH) -> {
                processException(throwExpression, kotlinFileContext, "check")
                processExceptionForError(throwExpression, kotlinFileContext)
            }

            throwExpression.matchesException(bindingContext, ILLEGAL_ARGUMENT_EXCEPTION_CONSTRUCTOR_MATCH) ->
                processException(throwExpression, kotlinFileContext, "require")
        }
    }

    private fun processException(throwExpression: KtThrowExpression, kotlinFileContext: KotlinFileContext, preconditionCall: String) {
        if (throwExpression.isOnlyStatementInThen()) {
            val ifExpression = throwExpression.findIfExpression()!!
            val replaceCall = computeReplaceCallForIfExpression(ifExpression, preconditionCall)
            val throwMessage = throwExpression.getErrorMessage().let { if (it == null) "" else " { $it }" }
            val message = buildReportMessage(statementToReplace = "if", statememntType = "expression", replacementCode = "$replaceCall$throwMessage")
            kotlinFileContext.reportIssue(psiElement = ifExpression.ifKeyword, message = message)
        }
    }

    private fun processExceptionForError(throwExpression: KtThrowExpression, kotlinFileContext: KotlinFileContext) {
        if (throwExpression.isOnlyStatementInElse() || throwExpression.isOnlyStatementInWhenEntry()) {
            val throwMessage = throwExpression.getErrorMessage() ?: "\"\""
            val message = buildReportMessage(statementToReplace = "throw", statememntType = "expression", replacementCode = "error($throwMessage)")
            kotlinFileContext.reportIssue(psiElement = throwExpression, message = message)
        }
    }
}

// KtBinaryExpression has always left and right
private fun KtExpression?.isNullCheckCondition(token: KtSingleValueToken) =
    (this as? KtBinaryExpression)?.let { it.operationToken == token && (it.left!!.isNull() || it.right!!.isNull()) }
        ?: false

private fun KtBinaryExpression.getNullCheckVariable() = with(left!!) { if (isNull()) right!!.text else text }

private fun KtThrowExpression.matchesException(bindingContext: BindingContext, funMatcher: FunMatcherImpl) =
    (thrownExpression as? KtCallExpression)?.let { funMatcher.matches(it, bindingContext) } ?: false

private fun KtThrowExpression.getErrorMessage() =
    (thrownExpression as? KtCallExpression)?.valueArguments.let { if (it?.size == 1) it[0].text else null }

private fun KtThrowExpression.isOnlyStatementInThen() =
    findIfExpression()
        ?.let {
            (it.then is KtThrowExpression && it.then == this) ||
                (it.then is KtBlockExpression
                    && (it.then as KtBlockExpression).statements.size == 1
                    && it.then == parent)
        } ?: false

private fun KtThrowExpression.isOnlyStatementInElse() =
    findIfExpression()
        ?.let {
            (it.`else` is KtThrowExpression && it.`else` == this) ||
                (it.`else` is KtBlockExpression
                    && (it.`else` as KtBlockExpression).statements.size == 1
                    && it.`else` == parent)
        } ?: false

private fun KtThrowExpression.findIfExpression(): KtIfExpression? {
    return when {
        // if() throwExpression
        parent.parent is KtIfExpression -> parent.parent as KtIfExpression
        // if() -> { throwExpression }
        parent.parent.parent is KtIfExpression -> parent.parent.parent as KtIfExpression
        // ignore
        else -> null
    }
}

private fun KtThrowExpression.isOnlyStatementInWhenEntry() =
    (parent is KtWhenEntry && parent.children.size == 1) ||
        (parent.parent is KtWhenEntry
            && parent is KtBlockExpression
            && (parent as KtBlockExpression).statements.size == 1
            && parent.parent.children[0] !is KtWhenConditionWithExpression)

private fun buildReportMessage(statementToReplace: String, statememntType: String, replacementCode: String) =
    message {
        +"Replace this "
        code(statementToReplace)
        +" $statememntType with "
        code(replacementCode)
        +"."
    }

private fun computeReplaceCallForIfExpression(ifExpression: KtIfExpression, replaceCall: String): String {
    // KtIfExpression condition is null only if not parsed: at this point it is guaranteed not to be null
    return when (val condition = ifExpression.condition!!) {
        is KtBinaryExpression -> replaceCallInBinaryExpression(condition, replaceCall)
        is KtPrefixExpression -> replaceCallInExclPrefixExpression(condition, replaceCall)
        is KtIsExpression -> replaceCallInIsExpression(condition, replaceCall)
        is KtConstantExpression -> replaceCallInConstantExpression(condition, replaceCall)
        is KtNameReferenceExpression -> callWithNegatedCondition(replaceCall, condition.text)
        is KtCallExpression -> callWithNegatedCondition(replaceCall, condition.text)
        else -> callWithNegatedConditionInBlock(replaceCall, condition.text)
    }
}

private fun replaceCallInBinaryExpression(condition: KtBinaryExpression, replaceCall: String) =
    if (condition.isNullCheckCondition(KtTokens.EQEQ))
        callNotNull(replaceCall, condition.getNullCheckVariable())
    else
        when (condition.operationToken) {
            KtTokens.EQEQ -> callWithCondition(replaceCall, condition.replaceOperationToken(KtTokens.EXCLEQ))
            KtTokens.EXCLEQ -> callWithCondition(replaceCall, condition.replaceOperationToken(KtTokens.EQEQ))
            KtTokens.LT -> callWithCondition(replaceCall, condition.replaceOperationToken(KtTokens.GTEQ))
            KtTokens.LTEQ -> callWithCondition(replaceCall, condition.replaceOperationToken(KtTokens.GT))
            KtTokens.GT -> callWithCondition(replaceCall, condition.replaceOperationToken(KtTokens.LTEQ))
            KtTokens.GTEQ -> callWithCondition(replaceCall, condition.replaceOperationToken(KtTokens.LT))
            else -> callWithNegatedConditionInBlock(replaceCall, condition.text)
        }

private fun replaceCallInExclPrefixExpression(condition: KtPrefixExpression, replaceCall: String) =
    if (condition.operationToken == KtTokens.EXCL)
        callWithCondition(replaceCall, condition.text.removePrefix(KtTokens.EXCL.value))
    else
        callWithNegatedConditionInBlock(replaceCall, condition.text)

private fun replaceCallInIsExpression(condition: KtIsExpression, replaceCall: String) =
    if (condition.isNegated)
        callWithCondition(replaceCall, condition.replaceOperationToken(KtTokens.IS_KEYWORD))
    else
        callWithCondition(replaceCall, condition.replaceOperationToken(KtTokens.NOT_IS))

private fun replaceCallInConstantExpression(condition: KtConstantExpression, replaceCall: String) =
    if (condition.isBooleanTrue())
        callWithCondition(replaceCall, KtTokens.FALSE_KEYWORD.value)
    else
        callWithCondition(replaceCall, KtTokens.TRUE_KEYWORD.value)

private fun callNotNull(call: String, condition: String) = "${call}NotNull($condition)"

private fun callWithCondition(call: String, condition: String) = "$call($condition)"

private fun callWithNegatedCondition(call: String, condition: String) = "$call(!$condition)"

private fun callWithNegatedConditionInBlock(call: String, condition: String) = "$call(!($condition))"

private fun KtBinaryExpression.replaceOperationToken(newToken: KtSingleValueToken) =
    "${this.left!!.text} ${newToken.value} ${this.right!!.text}"

private fun KtIsExpression.replaceOperationToken(newToken: KtSingleValueToken) =
    "${this.children[0].text} ${newToken.value} ${this.children[2].text}"

private fun KtConstantExpression.isBooleanTrue() = (this.firstChild as LeafPsiElement).elementType == KtTokens.TRUE_KEYWORD
