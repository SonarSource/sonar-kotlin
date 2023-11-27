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
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtSafeQualifiedExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtUnaryExpression
import org.jetbrains.kotlin.psi.psiUtil.isNull
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.util.getArgumentByParameterIndex
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext.isError
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
        val leftResolved = left?.predictRuntimeValueExpression(bc) ?: return null
        val rightResolved = right?.predictRuntimeValueExpression(bc) ?: return null

        return when {
            leftResolved.isNull() -> right
            rightResolved.isNull() -> left
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
        if (kfc.mayBeAffectedByErrorInSemantics()) return

        val resolvedExpression = expression.predictRuntimeValueExpression(kfc.bindingContext)

        val result = if (resolvedExpression.isNull()) {
            if (comparesToNull) "succeeds" else "fails"
        } else if (
        // We are not using the resolvedExpression on purpose here, as it can cause FPs. See SONARKT-373.
            expression.isNotNullable(kfc.bindingContext)
        ) {
            if (comparesToNull) "fails" else "succeeds"
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

/**
 * [WORKAROUND]
 * In some cases, semantics may be broken, e.g. in the Ktor IT. This may be due to multiplatform, although that is unconfirmed.
 * We seem to be able to identify such cases by looking for a MISSING_BUILT_IN_DECLARATION error somewhere close to the statement.
 * Since it is not always clear where this diagnostic might be raised, we over-approximate and ignore all files where such an error is
 * found.
 */
private fun KotlinFileContext.mayBeAffectedByErrorInSemantics() = diagnostics.any { it.factory == Errors.MISSING_BUILT_IN_DECLARATION }

private fun KtExpression.isNotNullable(bc: BindingContext) =
    when (this) {
        is KtConstantExpression -> !isNull()
        is KtStringTemplateExpression -> true
        else -> determineType(bc)?.let { resolvedType ->
            !resolvedType.isError() && resolvedType.nullability() == TypeNullability.NOT_NULL
        } == true
    }
