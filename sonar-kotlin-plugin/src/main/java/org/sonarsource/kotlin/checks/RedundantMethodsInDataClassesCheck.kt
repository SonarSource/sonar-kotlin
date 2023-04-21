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

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.ANY_TYPE
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.api.EQUALS_METHOD_NAME
import org.sonarsource.kotlin.api.FunMatcher
import org.sonarsource.kotlin.api.HASHCODE_METHOD_NAME
import org.sonarsource.kotlin.plugin.KotlinFileContext

private val equalsMatcher = FunMatcher {
    name = EQUALS_METHOD_NAME
    withArguments(ANY_TYPE)
}
private val hashCodeMatcher = FunMatcher {
    name = HASHCODE_METHOD_NAME
    withNoArguments()
}

@Rule(key = "S6207")
class RedundantMethodsInDataClassesCheck : AbstractCheck() {

    private val issueMessage = "Remove this redundant method which is the same as a default one."

    override fun visitClass(klass: KtClass, context: KotlinFileContext) {
        if (!klass.isData()) {
            return
        }
        val klassParameters = klass.primaryConstructor!!.valueParameters
        var equalsMethod: KtNamedFunction? = null
        var hashCodeMethod: KtNamedFunction? = null

        klass.body?.functions?.forEach {
            when {
                hashCodeMethod == null && hashCodeMatcher.matches(it, context.bindingContext) -> hashCodeMethod = it
                equalsMethod == null && equalsMatcher.matches(it, context.bindingContext) -> equalsMethod = it
            }
        }

        equalsMethod?.let {
            if (it.equalsHasDefaultImpl(klassParameters, it.valueParameters)) {
                context.reportIssue(it, issueMessage)
            }
        }

        hashCodeMethod?.let {
            if (it.hashCodeHasDefaultImpl(klassParameters)) {
                context.reportIssue(it, issueMessage)
            }
        }
    }

    private fun KtNamedFunction.hashCodeHasDefaultImpl(klassParameters: List<KtParameter>): Boolean {
        val hashExpression =
            this.findDescendantOfType<KtCallExpression> { (it.calleeExpression as KtNameReferenceExpression).getReferencedName() == "hash" }
        val valueArguments = hashExpression?.valueArgumentList?.collectDescendantsOfType<KtNameReferenceExpression>() ?: return false
        if (klassParameters.size != valueArguments.size) {
            return false
        }
        valueArguments.forEach { findParameter(it, klassParameters) ?: return false }
        return true
    }

    private fun KtNamedFunction.equalsHasDefaultImpl(
        klassParameters: List<KtParameter>,
        valueParameters: MutableList<KtParameter>,
    ): Boolean {
        val equalsExpressions = this.collectDescendantsOfType<KtBinaryExpression> { it.operationToken == KtTokens.EQEQ }
        if (equalsExpressions.size != klassParameters.size) {
            return false
        }
        val map = mutableMapOf<KtParameter, Boolean>()
        klassParameters.forEach {
            map[it] = false
        }

        equalsExpressions.forEach {
            val left = it.left
            val right = it.right
            checkExpression(left, right, klassParameters, valueParameters, map)
            checkExpression(right, left, klassParameters, valueParameters, map)
        }
        return !map.values.contains(false)
    }

    private fun checkExpression(
        first: KtExpression?,
        second: KtExpression?,
        klassParameters: List<KtParameter>,
        valueParameters: MutableList<KtParameter>,
        map: MutableMap<KtParameter, Boolean>,
    ) {
        val parameter = findParameter(first, klassParameters) ?: return
        when {
            checkDotExpression(second, parameter, valueParameters) -> {
                map[parameter] = true
            }
        }
    }

    private fun findParameter(
        expression: KtExpression?,
        klassParameters: List<KtParameter>,
    ): KtParameter? {
        if (expression is KtNameReferenceExpression) {
            return klassParameters.find { klassParameter -> klassParameter.name == expression.getReferencedName() }
        }
        return null
    }

    private fun checkDotExpression(
        expression: KtExpression?,
        parameter: KtParameter,
        valueParameters: MutableList<KtParameter>,
    ): Boolean {
        if (expression is KtDotQualifiedExpression) {
            return ((expression.receiverExpression as KtNameReferenceExpression).getReferencedName() == valueParameters[0].name
                && (expression.selectorExpression as KtNameReferenceExpression).getReferencedName() == parameter.name)
        }
        return false
    }

}
