/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2022 SonarSource SA
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
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.util.getCall
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.CallAbstractCheck
import org.sonarsource.kotlin.api.ConstructorMatcher
import org.sonarsource.kotlin.api.FunMatcher
import org.sonarsource.kotlin.api.INT_TYPE
import org.sonarsource.kotlin.api.SecondaryLocation
import org.sonarsource.kotlin.api.isBytesInitializedFromString
import org.sonarsource.kotlin.api.predictRuntimeIntValue
import org.sonarsource.kotlin.api.predictRuntimeValueExpression
import org.sonarsource.kotlin.converter.KotlinTextRanges.textRange
import org.sonarsource.kotlin.plugin.KotlinFileContext

private val CIPHER_INIT_MATCHER = FunMatcher(qualifier = "javax.crypto.Cipher", name = "init") {
    withArguments(INT_TYPE, "java.security.Key", "java.security.spec.AlgorithmParameterSpec")
}

private val GCM_PARAMETER_SPEC_MATCHER = ConstructorMatcher("javax.crypto.spec.GCMParameterSpec") {
    withArguments(INT_TYPE, "kotlin.ByteArray")
}

private val GET_BYTES_MATCHER = FunMatcher(qualifier = "kotlin.text", name = "toByteArray")

@Rule(key = "S6432")
class CipherModeOperationCheck : CallAbstractCheck() {
    override val functionsToVisit = listOf(CIPHER_INIT_MATCHER)

    override fun visitFunctionCall(
        callExpression: KtCallExpression,
        resolvedCall: ResolvedCall<*>,
        kotlinFileContext: KotlinFileContext,
    ) {
        val bindingContext = kotlinFileContext.bindingContext
        val firstArgument = callExpression.valueArguments[0].getArgumentExpression() ?: return
        val thirdArgument = callExpression.valueArguments[2].getArgumentExpression() ?: return

        val secondaries = mutableListOf<PsiElement>()

        val gcmExpr = thirdArgument.getGCMExpression(bindingContext, secondaries)
        val byteExpression = gcmExpr?.getByteExpression(bindingContext, secondaries)

        if (firstArgument.predictRuntimeIntValue(bindingContext) == 1 && byteExpression?.isBytesInitializedFromString(bindingContext) == true) {
            val locations =
                secondaries.map { SecondaryLocation(kotlinFileContext.textRange(it), "Initialization vector is configured here.") }
                    .groupBy { location -> location.textRange.start().line() }
                    .map { it.value[it.value.size - 1] }

            kotlinFileContext.reportIssue(thirdArgument, "The initialization vector is a static value.", locations)
        }

    }
}

private fun KtExpression.getByteExpression(bindingContext: BindingContext, secondaries: MutableList<PsiElement>): KtExpression? {
    val expression = predictRuntimeValueExpression(bindingContext, secondaries)
    expression.getCall(bindingContext)?.let {
        if (GET_BYTES_MATCHER.matches(it, bindingContext))
            return expression
    }
    return null
}


private fun KtExpression.getGCMExpression(bindingContext: BindingContext, secondaries: MutableList<PsiElement>): KtExpression? =
    predictRuntimeValueExpression(bindingContext)
        .getCall(bindingContext)?.let {
            if (GCM_PARAMETER_SPEC_MATCHER.matches(it, bindingContext)) {
                secondaries.add(it.valueArguments[1].asElement())
                return it.valueArguments[1].getArgumentExpression()
            } else null
        }