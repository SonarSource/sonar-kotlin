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

import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.checks.predictRuntimeBooleanValue
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.analyze

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

    private fun KtExpression.isTrueConstant(): Boolean =
        analyze { predictRuntimeBooleanValue() ?: false }
}
