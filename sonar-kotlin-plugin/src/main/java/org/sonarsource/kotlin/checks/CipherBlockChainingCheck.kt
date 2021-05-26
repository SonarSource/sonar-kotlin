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

import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.api.ConstructorMatcher
import org.sonarsource.kotlin.api.FunMatcher
import org.sonarsource.kotlin.api.GET_INSTANCE
import org.sonarsource.kotlin.api.INT_TYPE
import org.sonarsource.kotlin.api.STRING_TYPE
import org.sonarsource.kotlin.api.predictReceiverExpression
import org.sonarsource.kotlin.api.predictRuntimeStringValue
import org.sonarsource.kotlin.api.predictRuntimeValueExpression
import org.sonarsource.kotlin.plugin.KotlinFileContext

private val CIPHER_INIT_MATCHER = FunMatcher(qualifier = "javax.crypto.Cipher", name = "init") {
    withArguments(INT_TYPE, "java.security.Key", "java.security.spec.AlgorithmParameterSpec")
}

private val GET_INSTANCE_MATCHER = FunMatcher(qualifier = "javax.crypto.Cipher", name = GET_INSTANCE) {
    withArguments(STRING_TYPE)
}

private val GET_BYTES_MATCHER = FunMatcher(qualifier = "kotlin.text", name = "toByteArray")
private val IV_PARAMETER_SPEC_MATCHER = ConstructorMatcher("javax.crypto.spec.IvParameterSpec")

@Rule(key = "S3329")
class CipherBlockChainingCheck : AbstractCheck() {

    override fun visitCallExpression(expression: KtCallExpression, kotlinFileContext: KotlinFileContext) {
        val bindingContext = kotlinFileContext.bindingContext
        if (CIPHER_INIT_MATCHER.matches(expression, bindingContext)) {

            val calleeExpression = expression.calleeExpression ?: return
            val receiverExpression = expression.predictReceiverExpression(bindingContext) ?: return
            val thirdArgument = expression.valueArguments[2].getArgumentExpression() ?: return

            if (receiverExpression.isCBC(bindingContext) && thirdArgument.isInitializedWithToByteArray(bindingContext)) {
                kotlinFileContext.reportIssue(calleeExpression, "Use a dynamically-generated, random IV.")
            }
        }
    }
}

private fun KtExpression.isInitializedWithToByteArray(bindingContext: BindingContext) =
    firstArgumentOfInitializer(bindingContext, IV_PARAMETER_SPEC_MATCHER)
        ?.predictRuntimeValueExpression(bindingContext)
        ?.getCall(bindingContext)?.let { expr ->
            GET_BYTES_MATCHER.matches(expr, bindingContext)
        } ?: false

private fun KtExpression.isCBC(bindingContext: BindingContext) =
    firstArgumentOfInitializer(bindingContext, GET_INSTANCE_MATCHER)
        ?.predictRuntimeStringValue(bindingContext)
        ?.contains("CBC", ignoreCase = true)
        ?: false

private fun KtExpression.firstArgumentOfInitializer(bindingContext: BindingContext, matcher: FunMatcher) =
    predictRuntimeValueExpression(bindingContext)
        .getCall(bindingContext)?.let {
            if (matcher.matches(it, bindingContext)) it.valueArguments[0].getArgumentExpression()
            else null
        }
