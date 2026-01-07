/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2026 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1.0.1, as published by SonarSource Sàrl.
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
import org.jetbrains.kotlin.analysis.api.resolution.successfulFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.symbols.name
import org.jetbrains.kotlin.analysis.api.types.KaErrorType
import org.jetbrains.kotlin.analysis.api.types.KaTypeNullability
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType
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
import org.sonarsource.kotlin.api.visiting.withKaSession

private val NON_NULL_CHECK_FUNS = FunMatcher("kotlin") {
    withNames("requireNotNull", "checkNotNull")
}

@Rule(key = "S6619")
class UselessNullCheckCheck : AbstractCheck() {

    override fun visitBinaryExpression(binaryExpression: KtBinaryExpression, kotlinFileContext: KotlinFileContext) {
        when (binaryExpression.operationToken) {
            KtTokens.EQEQ -> binaryExpression.operandComparedToNull()?.let {
                raiseIssueIfUselessCheck(kotlinFileContext, it, binaryExpression, comparesToNull = true) {
                    +"null check"
                }
            }

            KtTokens.EXCLEQ -> binaryExpression.operandComparedToNull()?.let {
                raiseIssueIfUselessCheck(kotlinFileContext, it, binaryExpression, comparesToNull = false) {
                    +"non-null check"
                }
            }

            KtTokens.ELVIS -> raiseIssueIfUselessCheck(
                kotlinFileContext,
                binaryExpression.left!!,
                binaryExpression.operationReference,
                comparesToNull = false,
            ) {
                +"elvis operation "
                code("?:")
            }

        }
    }

    override fun visitSafeQualifiedExpression(safeDotExpression: KtSafeQualifiedExpression, kfc: KotlinFileContext) {
        raiseIssueIfUselessCheck(
            kfc,
            safeDotExpression.receiverExpression,
            safeDotExpression.operationTokenNode.psi,
            comparesToNull = false,
        ) {
            +"null-safe access "
            code("?.")
        }
    }

    override fun visitUnaryExpression(unaryExpression: KtUnaryExpression, kfc: KotlinFileContext) {
        if (unaryExpression.operationToken == KtTokens.EXCLEXCL) {
            raiseIssueIfUselessCheck(
                kfc,
                unaryExpression.baseExpression!!,
                unaryExpression.operationReference,
                comparesToNull = false,
            ) {
                +"non-null assertion "
                code("!!")
            }
        }
    }

    override fun visitCallExpression(callExpression: KtCallExpression, kfc: KotlinFileContext) {
        val resolvedCall = withKaSession { callExpression.resolveToCall()?.successfulFunctionCallOrNull() } ?: return
        if (resolvedCall matches NON_NULL_CHECK_FUNS) {
            // requireNotNull and checkNotNull have no implementations without parameters. The first parameter is always the value to check.
            val argExpression = resolvedCall.argumentMapping.keys.first()
            raiseIssueIfUselessCheck(
                kfc,
                argExpression,
                callExpression,
                comparesToNull = false,
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

    private fun raiseIssueIfUselessCheck(
        kfc: KotlinFileContext,
        expression: KtExpression,
        issueLocation: PsiElement,
        comparesToNull: Boolean,
        nullCheckTypeForMessage: Message.() -> Unit
    ) {
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

private fun KtExpression.isNotNullable(): Boolean =
    when (this) {
        is KtConstantExpression -> !isNull()
        is KtStringTemplateExpression -> true

        else -> withKaSession {
            this@isNotNullable.expressionType?.let { resolvedType ->
                resolvedType !is KaErrorType &&
                        resolvedType !is KaTypeParameterType &&
                        resolvedType.nullability == KaTypeNullability.NON_NULLABLE
            }
        } == true
    }
