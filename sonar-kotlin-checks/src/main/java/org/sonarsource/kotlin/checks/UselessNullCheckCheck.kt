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

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtSafeQualifiedExpression
import org.jetbrains.kotlin.psi.KtUnaryExpression
import org.jetbrains.kotlin.psi.psiUtil.isNull
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.util.getArgumentByParameterIndex
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.types.typeUtil.TypeNullability
import org.jetbrains.kotlin.types.typeUtil.nullability
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.checks.determineType
import org.sonarsource.kotlin.api.checks.matches
import org.sonarsource.kotlin.api.checks.predictRuntimeValueExpression
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.reporting.Message

private val NON_NULL_CHECK_FUNS = FunMatcher("kotlin") {
    withNames("requireNotNull", "checkNotNull")
}

@Rule(key = "S6619")
class UselessNullCheckCheck : AbstractCheck() {
    override fun visitBinaryExpression(binaryExpression: KtBinaryExpression, kotlinFileContext: KotlinFileContext) {
        val bc = kotlinFileContext.bindingContext

        when (binaryExpression.operationToken) {
            KtTokens.EQEQ -> binaryExpression.operandComparedToNull(bc)?.let {
                handleNullCheck(kotlinFileContext, it, binaryExpression) { +"null check" }
            }

            KtTokens.EXCLEQ -> binaryExpression.operandComparedToNull(bc)?.let {
                handleNonNullCheck(kotlinFileContext, it, binaryExpression) { +"non-null check" }
            }

            KtTokens.ELVIS -> handleNonNullCheck(
                kotlinFileContext,
                binaryExpression.left!!,
                binaryExpression.operationReference
            ) {
                +"elvis operation "
                code("?:")
            }

        }
    }

    override fun visitSafeQualifiedExpression(safeDotExpression: KtSafeQualifiedExpression, kfc: KotlinFileContext) {
        handleNonNullCheck(kfc, safeDotExpression.receiverExpression, safeDotExpression.operationTokenNode.psi) {
            +"null-safe access "
            code("?.")
        }
    }

    override fun visitUnaryExpression(unaryExpression: KtUnaryExpression, kfc: KotlinFileContext) {
        if (unaryExpression.operationToken == KtTokens.EXCLEXCL) {
            handleNonNullCheck(
                kfc,
                unaryExpression.baseExpression!!,
                unaryExpression.operationReference
            ) {
                +"non-null assertion "
                code("!!")
            }
        }
    }

    override fun visitCallExpression(callExpression: KtCallExpression, kfc: KotlinFileContext) {
        val resolvedCall = callExpression.getResolvedCall(kfc.bindingContext)
        if (resolvedCall matches NON_NULL_CHECK_FUNS) {
            // requireNotNull and checkNotNull have no implementations without parameters. The first parameter is always the value to check.
            handleNonNullCheck(
                kfc,
                callExpression.getArgumentByParameterIndex(0, kfc.bindingContext).first().getArgumentExpression()!!,
                callExpression
            ) {
                +"non-null check "
                code(resolvedCall!!.resultingDescriptor.name.asString())
            }
        }
    }

    private fun KtBinaryExpression.operandComparedToNull(bc: BindingContext): KtExpression? {
        val left = left?.predictRuntimeValueExpression(bc) ?: return null
        val right = right?.predictRuntimeValueExpression(bc) ?: return null

        return when {
            left.isNull() -> right
            right.isNull() -> left
            else -> null
        }
    }

    private fun handleNullCheck(
        kfc: KotlinFileContext,
        expression: KtExpression,
        issueLocation: PsiElement,
        nullCheckTypeForMessage: Message.() -> Unit
    ) =
        raiseIssueIfUselessCheck(kfc, expression, issueLocation, true, nullCheckTypeForMessage)

    private fun handleNonNullCheck(
        kfc: KotlinFileContext,
        expression: KtExpression,
        issueLocation: PsiElement,
        nullCheckTypeForMessage: Message.() -> Unit
    ) =
        raiseIssueIfUselessCheck(kfc, expression, issueLocation, false, nullCheckTypeForMessage)

    private fun raiseIssueIfUselessCheck(
        kfc: KotlinFileContext,
        expression: KtExpression,
        issueLocation: PsiElement,
        comparesToNull: Boolean,
        nullCheckTypeForMessage: Message.() -> Unit
    ) {
        val (nullCaseResult, nonNullCaseResult) = if (comparesToNull) {
            "succeeds" to "fails"
        } else {
            "fails" to "succeeds"
        }

        val resolvedExpression = expression.predictRuntimeValueExpression(kfc.bindingContext)

        val result = if (resolvedExpression.isNull()) {
            nullCaseResult
        } else if (
            // We are not using the resolvedExpression on purpose here, as it can cause FPs. See SONARKT-373.
            expression.determineType(kfc.bindingContext)?.nullability() == TypeNullability.NOT_NULL
        ) {
            nonNullCaseResult
        } else {
            return
        }

        kfc.reportIssue(issueLocation) {
            +"Remove this useless "
            nullCheckTypeForMessage()
            +", it always $result."
        }
    }
}