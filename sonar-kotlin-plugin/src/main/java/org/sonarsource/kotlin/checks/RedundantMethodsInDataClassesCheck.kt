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
private val OBJECTS_HASH_MATCHER = FunMatcher(qualifier = "java.util.Objects", name = "hash")
private val ARRAYS_HASHCODE_MATCHER = FunMatcher(qualifier = "java.util.Arrays", name = "hashCode")

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
            checkHashExpression(returnExpressions[0].returnedExpression, bindingContext, klassParameters)
        } else {
            checkHashExpression(this.bodyExpression, bindingContext, klassParameters)
        }
    }

    private fun checkHashExpression(expression: KtExpression?, bindingContext: BindingContext, klassParameters: List<KtParameter>): Boolean {
        if (expression !is KtDotQualifiedExpression) return false
        if (expression.selectorExpression !is KtCallExpression) return false

        val callExpression = expression.selectorExpression as KtCallExpression
        if (OBJECTS_HASH_MATCHER.matches(callExpression, bindingContext)) {
            if (callExpression.valueArguments.size != klassParameters.size) return false
            return callExpression.valueArguments.all {
                findParameter(it.getArgumentExpression(), klassParameters) != null
            }
        }
        if (ARRAYS_HASHCODE_MATCHER.matches(callExpression, bindingContext)) {
            val arguments = (callExpression.valueArguments[0].getArgumentExpression() as KtCallExpression).valueArguments
            if (arguments.size != klassParameters.size) return false
            return arguments.all {
                findParameter(it.getArgumentExpression(), klassParameters) != null
            }
        }
        return false
    }

    private fun KtNamedFunction.equalsHasDefaultImpl(
            klassParameters: List<KtParameter>,
            methodParameters: MutableList<KtParameter>,
            className: String?
    ): Boolean {
        return if (hasBlockBody()) {
            val returnExpressions = collectDescendantsOfType<KtReturnExpression>()
            if (returnExpressions.size > 1) return false
            checkEqualsExpression(returnExpressions[0].returnedExpression, klassParameters, methodParameters, className)
        } else {
            checkEqualsExpression(this.bodyExpression, klassParameters, methodParameters, className)
        }
    }

    private fun checkEqualsExpression(expression: KtExpression?, klassParameters: List<KtParameter>, methodParameters: MutableList<KtParameter>, className: String?): Boolean {
        val map = mutableMapOf<KtParameter, Boolean>()
        klassParameters.forEach {
            map[it] = false
        }
        return visitExpression(expression, klassParameters, methodParameters, map, className) && !map.values.contains(false)
    }

    private fun visitExpression(expression: KtExpression?, klassParameters: List<KtParameter>, methodParameters: MutableList<KtParameter>, map: MutableMap<KtParameter, Boolean>, className: String?): Boolean {
        if (expression is KtBinaryExpression) {
            if (expression.operationToken == KtTokens.ANDAND) {
                return visitExpression(expression.right, klassParameters, methodParameters, map, className) && visitExpression(expression.left, klassParameters, methodParameters, map, className)
            }
            if (expression.operationToken == KtTokens.EQEQ) {
                checkIfExpressionHasParameter(expression.left, expression.right, klassParameters, methodParameters, map)
                checkIfExpressionHasParameter(expression.right, expression.left, klassParameters, methodParameters, map)
                return true
            }
        }
        if (expression is KtIsExpression) {
            return !(expression.isNegated || expression.typeReference?.nameForReceiverLabel() != className)
        }
        return false
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
