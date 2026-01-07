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

import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.checks.getParentCall
import org.sonarsource.kotlin.api.checks.predictRuntimeBooleanValue
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.withKaSession

@Rule(key = "S5527")
class VerifiedServerHostnamesCheck : AbstractCheck() {

    companion object {
        val VERIFY_MATCHER = FunMatcher {
            definingSupertype = "javax.net.ssl.HostnameVerifier"
            name = "verify"
        }

        val HOSTNAME_VERIFIER_MATCHER = FunMatcher {
            qualifier = "okhttp3.OkHttpClient.Builder"
            name = "hostnameVerifier"
        }

        const val MESSAGE = "Enable server hostname verification on this SSL/TLS connection."
    }

    override fun visitNamedFunction(function: KtNamedFunction, kotlinFileContext: KotlinFileContext) {
        if (VERIFY_MATCHER.matches(function)) {
            val listStatements = function.listStatements()
            if (listStatements.size == 1 && onlyReturnsTrue(listStatements[0])) {
                kotlinFileContext.reportIssue(function.nameIdentifier!!, MESSAGE)
            }
        }
    }

    override fun visitLambdaExpression(expression: KtLambdaExpression, kotlinFileContext: KotlinFileContext) {
        expression.getParentCall()?.let {
            if (HOSTNAME_VERIFIER_MATCHER.matches(it)) {
                val listStatements = expression.bodyExpression?.statements
                if (listStatements?.size == 1 && listStatements[0].isTrueConstant()) {
                    kotlinFileContext.reportIssue(expression, MESSAGE)
                }
            }
        }
    }

    private fun onlyReturnsTrue(
        ktExpression: KtExpression,
    ): Boolean = when (ktExpression) {
        is KtReturnExpression ->
            ktExpression.returnedExpression?.isTrueConstant() ?: false
        else -> false
    }

    private fun KtExpression.isTrueConstant() = withKaSession {
        predictRuntimeBooleanValue() ?: false
    }
}
