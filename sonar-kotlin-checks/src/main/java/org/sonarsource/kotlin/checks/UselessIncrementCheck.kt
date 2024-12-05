/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2024 SonarSource SA
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

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtPostfixExpression
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.checks.isLocalVariable
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

private val POSTFIX_INCREMENT_OPERATORS = listOf(KtTokens.PLUSPLUS, KtTokens.MINUSMINUS)
private const val MESSAGE = "Remove this increment or correct the code not to waste it."

@Rule(key = "S2123")
class UselessIncrementCheck : AbstractCheck() {

    override fun visitReturnExpression(returnExpression: KtReturnExpression, ctx: KotlinFileContext) {
        returnExpression.returnedExpression.asPostfixIncrement()?.let {
            if (it.baseExpression.isLocalVariable()) {
                ctx.reportIssue(it, MESSAGE)
            }
        }
    }

    override fun visitBinaryExpression(expression: KtBinaryExpression, ctx: KotlinFileContext) {
        if (expression.operationToken == KtTokens.EQ) {
            expression.right.asPostfixIncrement()?.let {
                if (matchSameNameReference(expression.left, it.baseExpression)) {
                    ctx.reportIssue(expression, MESSAGE)
                }
            }
        }
    }

    private fun KtExpression?.asPostfixIncrement(): KtPostfixExpression? = when {
        (this is KtPostfixExpression) && (operationToken in POSTFIX_INCREMENT_OPERATORS) -> this
        else -> null
    }

    private fun matchSameNameReference(a: KtExpression?, b: KtExpression?): Boolean =
        (a is KtNameReferenceExpression) && (b is KtNameReferenceExpression) && (a.getReferencedName() == b.getReferencedName())

}
