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
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.*
import org.sonarsource.kotlin.plugin.KotlinFileContext

private val EQUALS_MATCHER = FunMatcher {
    name = EQUALS_METHOD_NAME
    withArguments(ANY_TYPE)
    returnType = BOOLEAN_TYPE
}
private val HASHCODE_MATCHER = FunMatcher {
    name = HASHCODE_METHOD_NAME
    withNoArguments()
    returnType = INT_TYPE
}

private val OBJECT_ARRAY_HASH_MATCHER = listOf(
        FunMatcher(qualifier = "java.util.Objects", name = "hash"),
        FunMatcher(qualifier = "java.util.Arrays", name = "hashCode")
)

@Rule(key = "S6207")
class RedundantMethodsInDataClassesCheck : AbstractCheck() {

    private val issueMessage = "Remove this redundant method which is the same as a default one."

    override fun visitClass(klass: KtClass, context: KotlinFileContext) {
        if (!klass.isData()) return

        // if class is data class it always contains constructor
        val klassParameters = klass.primaryConstructor!!.valueParameters
        var equalsMethod: KtNamedFunction? = null
        var hashCodeMethod: KtNamedFunction? = null

        klass.body?.functions?.forEach {
            when {
                EQUALS_MATCHER.matches(it, context.bindingContext) -> equalsMethod = it
                HASHCODE_MATCHER.matches(it, context.bindingContext) -> hashCodeMethod = it
            }
        }

        equalsMethod?.let {
            if (it.equalsHasDefaultImpl(klassParameters, it.valueParameters, klass.name)) {
                context.reportIssue(it.nameIdentifier!!, issueMessage)
            }
        }

        hashCodeMethod?.let {
            if (it.hashCodeHasDefaultImpl(klassParameters, context.bindingContext)) {
                context.reportIssue(it.nameIdentifier!!, issueMessage)
            }
        }
    }

    private fun KtNamedFunction.hashCodeHasDefaultImpl(klassParameters: List<KtParameter>, bindingContext: BindingContext): Boolean {
        return if (hasBlockBody()) {
            val returnExpressions = collectDescendantsOfType<KtReturnExpression>()
            if (returnExpressions.size > 1) return false
            checkHashExpression(returnExpressions[0], bindingContext, klassParameters)
        } else {
            checkHashExpression(this, bindingContext, klassParameters)
        }
    }

    private fun checkHashExpression(expression: KtExpression, bindingContext: BindingContext, klassParameters: List<KtParameter>): Boolean {
        val callExpressions = expression.collectDescendantsOfType<KtCallExpression> {
            OBJECT_ARRAY_HASH_MATCHER.any { e ->
                e.matches(it, bindingContext)
            }
        }
        if (callExpressions.size > 1 || callExpressions.isEmpty()) return false

        if ((callExpressions[0].calleeExpression as KtNameReferenceExpression).getReferencedName() == "hash") {
            if (callExpressions[0].valueArguments.size != klassParameters.size) return false
            callExpressions[0].valueArguments.forEach {
                findParameter(it.getArgumentExpression(), klassParameters) ?: return false
            }
        } else {
            val arguments = callExpressions[0].valueArguments[0].collectDescendantsOfType<KtValueArgumentList>()
            if (arguments.isEmpty() || arguments.size > 1) return false

            if (arguments[0].arguments.size != klassParameters.size) return false
            arguments[0].arguments.forEach {
                findParameter(it.getArgumentExpression(), klassParameters) ?: return false
            }
        }
        return true
    }

    private fun KtNamedFunction.equalsHasDefaultImpl(
            klassParameters: List<KtParameter>,
            methodParameters: MutableList<KtParameter>,
            className: String?
    ): Boolean {
        return if (hasBlockBody()) {
            val returnExpressions = collectDescendantsOfType<KtReturnExpression>()
            if (returnExpressions.size > 1) return false
            checkEqualsExpression(returnExpressions[0], klassParameters, methodParameters, className)
        } else {
            val expression = findDescendantOfType<KtBinaryExpression>() ?: return false
            checkEqualsExpression(expression, klassParameters, methodParameters, className)
        }
    }

    private fun checkEqualsExpression(expression: KtExpression, klassParameters: List<KtParameter>, methodParameters: MutableList<KtParameter>, className: String?): Boolean {
        val notEqualsExpressions = expression.collectDescendantsOfType<KtBinaryExpression> { it.operationToken == KtTokens.EXCLEQ }
        if (notEqualsExpressions.isNotEmpty()) return false

        val isExpressions = expression.collectDescendantsOfType<KtIsExpression>()
        if (isExpressions.size > 1 || isExpressions.isEmpty()) return false
        if (isExpressions[0].isNegated || isExpressions[0].typeReference?.nameForReceiverLabel() != className) return false

        val equalsExpressions = expression.collectDescendantsOfType<KtBinaryExpression> { it.operationToken == KtTokens.EQEQ }
        if (equalsExpressions.size != klassParameters.size) return false

        val map = mutableMapOf<KtParameter, Boolean>()
        klassParameters.forEach {
            map[it] = false
        }

        equalsExpressions.forEach {
            val left = it.left
            val right = it.right
            checkIfExpressionHasParameter(left, right, klassParameters, methodParameters, map)
            checkIfExpressionHasParameter(right, left, klassParameters, methodParameters, map)
        }

        return !map.values.contains(false)
    }

    private fun checkIfExpressionHasParameter(
            first: KtExpression?,
            second: KtExpression?,
            klassParameters: List<KtParameter>,
            methodParameters: MutableList<KtParameter>,
            map: MutableMap<KtParameter, Boolean>,
    ) {
        val parameter = findParameter(first, klassParameters) ?: return
        if (checkDotExpression(second, parameter, methodParameters))
            map[parameter] = true
    }

    private fun findParameter(
            expression: KtExpression?,
            klassParameters: List<KtParameter>,
    ): KtParameter? =
            if (expression is KtNameReferenceExpression)
                klassParameters.find { klassParameter -> klassParameter.name == expression.getReferencedName() }
            else null

    private fun checkDotExpression(
            expression: KtExpression?,
            klassParameter: KtParameter,
            methodParameters: MutableList<KtParameter>,
    ): Boolean =
            if (expression is KtDotQualifiedExpression) {
                ((expression.receiverExpression as KtNameReferenceExpression).getReferencedName() == methodParameters[0].name
                        && (expression.selectorExpression as KtNameReferenceExpression).getReferencedName() == klassParameter.name)
            } else false

}
