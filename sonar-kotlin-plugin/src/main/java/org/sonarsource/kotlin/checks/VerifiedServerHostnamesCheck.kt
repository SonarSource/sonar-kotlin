/*
 * SonarSource SLang
 * Copyright (C) 2018-2021 SonarSource SA
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
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getParentCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.api.FunMatcher
import org.sonarsource.kotlin.plugin.KotlinFileContext

@Rule(key = "S5527")
class VerifiedServerHostnamesCheck : AbstractCheck() {

    companion object {
        val VERIFY_MATCHER = FunMatcher {
            supertype = "javax.net.ssl.HostnameVerifier"
            names = listOf("verify")
        }

        val HOSTNAME_VERIFIER_MATCHER = FunMatcher {
            type = "okhttp3.OkHttpClient.Builder"
            names = listOf("hostnameVerifier")
        }
        
        const val MESSAGE = "Enable server hostname verification on this SSL/TLS connection"
    }

    override fun visitNamedFunction(function: KtNamedFunction, kotlinFileContext: KotlinFileContext) {
        val (_, _, bindingContext) = kotlinFileContext
        if (VERIFY_MATCHER.matches(function, bindingContext)) {
            val listStatements = function.listStatements()
            if (listStatements.size == 1 && onlyReturnsTrue(listStatements[0], bindingContext)) {
                kotlinFileContext.reportIssue(function.nameIdentifier!!, MESSAGE)
            }
        }
    }

    override fun visitLambdaExpression(expression: KtLambdaExpression, kotlinFileContext: KotlinFileContext) {
        val (_, _, bindingContext) = kotlinFileContext
        expression.getParentCall(bindingContext)?.let {
            if (HOSTNAME_VERIFIER_MATCHER.matches(it, bindingContext)) {
                val listStatements = expression.bodyExpression?.statements
                if (listStatements?.size == 1 && listStatements[0].isTrueConstant(bindingContext)) {
                    kotlinFileContext.reportIssue(expression, MESSAGE)
                }
            }
        }
    }

    private fun onlyReturnsTrue(
        ktExpression: KtExpression,
        bindingContext: BindingContext,
    ): Boolean = when (ktExpression) {
        is KtReturnExpression ->
            ktExpression.returnedExpression?.isTrueConstant(bindingContext) ?: false
        else -> false
    }
    
    private fun KtExpression.isTrueConstant(
        bindingContext: BindingContext,
    ) = getType(bindingContext)?.let {
        bindingContext.get(BindingContext.COMPILE_TIME_VALUE, this)?.getValue(it) == true
    } ?: false
}
