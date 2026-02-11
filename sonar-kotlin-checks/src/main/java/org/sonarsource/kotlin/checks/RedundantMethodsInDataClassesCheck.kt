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

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtIsExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.ANY_TYPE
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.checks.BOOLEAN_TYPE
import org.sonarsource.kotlin.api.checks.EQUALS_METHOD_NAME
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.checks.HASHCODE_METHOD_NAME
import org.sonarsource.kotlin.api.checks.INT_TYPE
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

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
                EQUALS_MATCHER.matches(it) -> equalsMethod = it
                HASHCODE_MATCHER.matches(it) -> hashCodeMethod = it
            }
        }

        equalsMethod?.let {
            if (it.equalsHasDefaultImpl(klassParameters, it.valueParameters, klass.name)) {
                context.reportIssue(it.nameIdentifier!!, issueMessage)
            }
        }

        hashCodeMethod?.let {
            if (it.hashCodeHasDefaultImpl(klassParameters)) {
                context.reportIssue(it.nameIdentifier!!, issueMessage)
            }
        }
    }
}

private fun KtNamedFunction.hashCodeHasDefaultImpl(
    klassParameters: List<KtParameter>,
): Boolean {
    return if (hasBlockBody()) {
        val returnExpressions = collectDescendantsOfType<KtReturnExpression>()
        if (returnExpressions.size > 1) return false
        checkHashExpression(returnExpressions[0].returnedExpression, klassParameters)
    } else {
        checkHashExpression(this.bodyExpression, klassParameters)
    }
}

private fun checkHashExpression(
    expression: KtExpression?,
    klassParameters: List<KtParameter>
): Boolean {
    if (expression !is KtDotQualifiedExpression) return false
    if (expression.selectorExpression !is KtCallExpression) return false

    val callExpression = expression.selectorExpression as KtCallExpression
    if (OBJECTS_HASH_MATCHER.matches(callExpression)) {
        if (callExpression.valueArguments.size != klassParameters.size) return false
        return callExpression.valueArguments.all {
            findParameter(it.getArgumentExpression(), klassParameters) != null
        }
    }
    if (ARRAYS_HASHCODE_MATCHER.matches(callExpression)) {
        val argumentExpression = callExpression.valueArguments[0].getArgumentExpression()
        if (argumentExpression !is KtCallExpression) return false
        val arguments = argumentExpression.valueArguments
        if (arguments.size != klassParameters.size) return false
        return arguments.all {
            findParameter(it.getArgumentExpression(), klassParameters) != null
        }
    }
    return false
}

private fun KtNamedFunction.equalsHasDefaultImpl(
    klassParameters: List<KtParameter>,
    methodParameters: List<KtParameter>,
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

private fun checkEqualsExpression(
    expression: KtExpression?,
    klassParameters: List<KtParameter>,
    methodParameters: List<KtParameter>,
    className: String?
): Boolean {
    val map = mutableMapOf<KtParameter, Boolean>()
    klassParameters.forEach {
        map[it] = false
    }
    return visitExpression(expression, klassParameters, methodParameters, map, className) && !map.values.contains(false)
}

private fun visitExpression(
    expression: KtExpression?,
    klassParameters: List<KtParameter>,
    methodParameters: List<KtParameter>,
    map: MutableMap<KtParameter, Boolean>,
    className: String?
): Boolean {
    if (expression is KtBinaryExpression) {
        if (expression.operationToken == KtTokens.ANDAND) {
            return visitExpression(
                expression.right,
                klassParameters,
                methodParameters,
                map,
                className
            ) && visitExpression(expression.left, klassParameters, methodParameters, map, className)
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
    methodParameters: List<KtParameter>,
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
    methodParameters: List<KtParameter>,
): Boolean =
    if (expression is KtDotQualifiedExpression) {
        ((expression.receiverExpression as KtNameReferenceExpression).getReferencedName() == methodParameters[0].name
                && (expression.selectorExpression as KtNameReferenceExpression).getReferencedName() == klassParameter.name)
    } else false


