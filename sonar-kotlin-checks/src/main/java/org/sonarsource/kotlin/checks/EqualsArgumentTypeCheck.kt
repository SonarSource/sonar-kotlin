/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2025 SonarSource SA
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

import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.lexer.KtTokens.THIS_KEYWORD
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBinaryExpressionWithTypeRHS
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtIsExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtSafeQualifiedExpression
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtWhenConditionIsPattern
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.ANY_TYPE
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.checks.EQUALS_METHOD_NAME
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.withKaSession

private val EQUALS_MATCHER = FunMatcher {
    name = EQUALS_METHOD_NAME
    withArguments(ANY_TYPE)
}

@Rule(key = "S2097")
class EqualsArgumentTypeCheck : AbstractCheck() {

    override fun visitNamedFunction(function: KtNamedFunction, ctx: KotlinFileContext) {
        if (!EQUALS_MATCHER.matches(function)) return
        if (!function.hasBody()) return

        val klass = function.containingClass() ?: return
        val parameter = function.valueParameters.first()


        if (checkIsExpression(function, parameter, klass) &&
            checkWhenExpression(function, parameter, klass) &&
            checkBinaryExpression(function, parameter, klass) &&
            checkBinaryExpressionRHS(function, parameter, klass)
        ) {
            ctx.reportIssue(function.nameIdentifier!!, "Add a type test to this method.")
        }
    }

    private fun checkBinaryExpressionRHS(
        function: KtNamedFunction,
        parameter: KtParameter,
        klass: KtClass
    ) = function.collectDescendantsOfType<KtBinaryExpressionWithTypeRHS> { it.operationReference.getReferencedName() == "as?" }
        .none { binaryExpression -> isBinaryExpressionWithTypeCorrect(binaryExpression, parameter, klass) }

    private fun checkBinaryExpression(
        function: KtNamedFunction,
        parameter: KtParameter,
        klass: KtClass
    ) = function.collectDescendantsOfType<KtBinaryExpression> { it.operationToken == KtTokens.EQEQ || it.operationToken == KtTokens.EXCLEQ }
        .none { binaryExpression -> isBinaryExpressionCorrect(binaryExpression, parameter, klass) }

    private fun checkWhenExpression(
        function: KtNamedFunction,
        parameter: KtParameter,
        klass: KtClass
    ) =
        function.collectDescendantsOfType<KtWhenExpression> { parameter.name == (it.subjectExpression as? KtNameReferenceExpression)?.getReferencedName() }
            .none {
                it.collectDescendantsOfType<KtWhenConditionIsPattern>().any { whenConditionIsPattern ->
                    // typeReference is always present
                    isExpressionCorrectType(whenConditionIsPattern.typeReference!!, klass)
                }
            }

    private fun checkIsExpression(
        function: KtNamedFunction,
        parameter: KtParameter,
        klass: KtClass
    ) =
        function.collectDescendantsOfType<KtIsExpression> { parameter.name == (it.leftHandSide as? KtNameReferenceExpression)?.getReferencedName() }
            .none {
                // typeReference is always present
                isExpressionCorrectType(it.typeReference!!, klass)
            }

    private fun isExpressionCorrectType(typeReference: KtTypeReference, klass: KtClass): Boolean = withKaSession {
        val name = typeReference.nameForReceiverLabel()
        val parentNames = klass.superTypeListEntries.mapNotNull { it.typeReference!!.nameForReceiverLabel() }
        val klassSymbol: KaClassSymbol? = klass.classSymbol
        return klass.name == name || parentNames.contains(name) ||
                klassSymbol != null && typeReference.type.isSubtypeOf(klassSymbol)
    }

    private fun isBinaryExpressionWithTypeCorrect(
        binaryExpression: KtBinaryExpressionWithTypeRHS,
        parameter: KtParameter,
        klass: KtClass,
    ): Boolean {
        val left = binaryExpression.left.collectDescendantsOfType<KtNameReferenceExpression>().map { it.getReferencedName() }
        val right = binaryExpression.right!!.collectDescendantsOfType<KtNameReferenceExpression>().map { it.getReferencedName() }
        return left.contains(parameter.name) && isTestedAgainstProperType(right, klass)
    }

    private fun isBinaryExpressionCorrect(binaryExpression: KtBinaryExpression, parameter: KtParameter, klass: KtClass): Boolean {
        val left = binaryExpression.left!!.collectDescendantsOfType<KtNameReferenceExpression>()
            .filterNot { (it.parent is KtDotQualifiedExpression || it.parent is KtSafeQualifiedExpression) && it.getReferencedName() == "javaClass" }
            .map { it.getReferencedName() }
        val right = binaryExpression.right!!.collectDescendantsOfType<KtNameReferenceExpression>()
            .filterNot { (it.parent is KtDotQualifiedExpression || it.parent is KtSafeQualifiedExpression) && it.getReferencedName() == "javaClass" }
            .map { it.getReferencedName() }
        return (left.contains(parameter.name) && isTestedAgainstProperType(right, klass)) || (isTestedAgainstProperType(
            left,
            klass
        ) && right.contains(
            parameter.name
        ))
    }

    private fun isTestedAgainstProperType(list: List<String>, klass: KtClass) =
        listOf(klass.name, THIS_KEYWORD.value, "javaClass").any { list.contains(it) }

}
