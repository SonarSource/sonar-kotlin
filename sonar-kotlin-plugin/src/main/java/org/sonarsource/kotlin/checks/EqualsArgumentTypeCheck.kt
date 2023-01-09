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
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.ANY_TYPE
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.api.EQUALS_METHOD_NAME
import org.sonarsource.kotlin.api.FunMatcher
import org.sonarsource.kotlin.api.determineType
import org.sonarsource.kotlin.api.isSupertypeOf
import org.sonarsource.kotlin.plugin.KotlinFileContext

private val EQUALS_MATCHER = FunMatcher {
    name = EQUALS_METHOD_NAME
    withArguments(ANY_TYPE)
}

@Rule(key = "S2097")
class EqualsArgumentTypeCheck : AbstractCheck() {

    override fun visitNamedFunction(function: KtNamedFunction, ctx: KotlinFileContext) {
        val bindingContext = ctx.bindingContext
        if (!EQUALS_MATCHER.matches(function, bindingContext)) return
        if (!function.hasBody()) return

        val klass = function.containingClass() ?: return
        val parameter = function.valueParameters.first()
        val parentNames = klass.superTypeListEntries.mapNotNull { it.typeReference!!.nameForReceiverLabel() }

        if (function.collectDescendantsOfType<KtIsExpression> { parameter.name == (it.leftHandSide as? KtNameReferenceExpression)?.getReferencedName() }
                .none {
                    // typeReference is always present
                    val name = it.typeReference!!.nameForReceiverLabel()
                    klass.name == name || parentNames.contains(name) ||
                        it.typeReference!!.determineType(bindingContext)
                            ?.let { type -> klass.determineType(bindingContext)?.isSupertypeOf(type) } == true
                } &&

            function.collectDescendantsOfType<KtBinaryExpression> { it.operationToken == KtTokens.EQEQ || it.operationToken == KtTokens.EXCLEQ }
                .none { binaryExpression -> isBinaryExpressionCorrect(binaryExpression, parameter, klass) } &&

            function.collectDescendantsOfType<KtBinaryExpressionWithTypeRHS> { it.operationReference.getReferencedName() == "as?" }
                .none { binaryExpression -> isBinaryExpressionWithTypeCorrect(binaryExpression, parameter, klass) }
        ) {
            ctx.reportIssue(function.nameIdentifier!!, "Add a type test to this method.")
        }
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
