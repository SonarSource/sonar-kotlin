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
import org.jetbrains.kotlin.analysis.api.resolution.successfulFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.symbols.name
import org.jetbrains.kotlin.analysis.api.types.KaErrorType
import org.jetbrains.kotlin.analysis.api.types.KaTypeNullability
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
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.checks.matches
import org.sonarsource.kotlin.api.checks.predictRuntimeValueExpression
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.reporting.Message
import org.sonarsource.kotlin.api.visiting.analyze

private val NON_NULL_CHECK_FUNS = FunMatcher("kotlin") {
    withNames("requireNotNull", "checkNotNull")
}

@org.sonarsource.kotlin.api.frontend.K1only("result differs in fixing 1 FN")
@Rule(key = "S6619")
class UselessNullCheckCheck : AbstractCheck() {

    override fun visitBinaryExpression(binaryExpression: KtBinaryExpression, kotlinFileContext: KotlinFileContext) {
        when (binaryExpression.operationToken) {
            KtTokens.EQEQ -> binaryExpression.operandComparedToNull()?.let {
                handleNullCheck(kotlinFileContext, it, binaryExpression) { +"null check" }
            }

            KtTokens.EXCLEQ -> binaryExpression.operandComparedToNull()?.let {
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
        val resolvedCall = analyze { callExpression.resolveToCall()?.successfulFunctionCallOrNull() } ?: return
        if (resolvedCall matches NON_NULL_CHECK_FUNS) {
            val argExpression = resolvedCall.argumentMapping.keys.toList().first()
            // requireNotNull and checkNotNull have no implementations without parameters. The first parameter is always the value to check.
            handleNonNullCheck(
                kfc,
                argExpression,
                callExpression
            ) {
                +"non-null check "
                code(resolvedCall.partiallyAppliedSymbol.symbol.name.toString())
            }
        }
    }

    private fun KtBinaryExpression.operandComparedToNull(): KtExpression? {
        val leftResolved = left?.predictRuntimeValueExpression() ?: return null
        val rightResolved = right?.predictRuntimeValueExpression() ?: return null

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

        val resolvedExpression = expression.predictRuntimeValueExpression()

        val result = if (resolvedExpression.isNull()) {
            if (comparesToNull) "succeeds" else "fails"
        } else if (
        // We are not using the resolvedExpression on purpose here, as it can cause FPs. See SONARKT-373.
            expression.isNotNullable()
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

private fun KtExpression.isNotNullable() =
    when (this) {
        is KtConstantExpression -> !isNull()
        is KtStringTemplateExpression -> true
        else -> analyze {
            this@isNotNullable.expressionType
        }?.let { resolvedType ->
            resolvedType !is KaErrorType && resolvedType.nullability == KaTypeNullability.NON_NULLABLE
        } == true
    }
