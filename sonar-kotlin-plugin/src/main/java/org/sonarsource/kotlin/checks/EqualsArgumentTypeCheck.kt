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

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.lexer.KtTokens.THIS_KEYWORD
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBinaryExpressionWithTypeRHS
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtIsExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.ANY_TYPE
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.api.EQUALS_METHOD_NAME
import org.sonarsource.kotlin.api.FunMatcher
import org.sonarsource.kotlin.api.isAbstract
import org.sonarsource.kotlin.plugin.KotlinFileContext

private val EQUALS_MATCHER = FunMatcher {
    name = EQUALS_METHOD_NAME
    withArguments(ANY_TYPE)
}

@Rule(key = "S2097")
class EqualsArgumentTypeCheck : AbstractCheck() {

    override fun visitNamedFunction(function: KtNamedFunction, ctx: KotlinFileContext) {
        if (!EQUALS_MATCHER.matches(function, ctx.bindingContext)) return
        if (function.isAbstract()) return

        val klass = function.containingClass() ?: return
        val parameter = function.valueParameters.first()
        val klassNames = klass.collectDescendantsOfType<KtClass>().mapNotNull { it.name }

        val hasIsExpression =
            function.collectDescendantsOfType<KtIsExpression> { parameter.name == (it.leftHandSide as? KtNameReferenceExpression)?.getReferencedName() }
                .any { klassNames.contains(it.typeReference!!.nameForReceiverLabel()) }
        if (hasIsExpression) return

        val binaryExpressions = function.collectDescendantsOfType<KtBinaryExpression> {
            it.operationToken == KtTokens.EQEQ || it.operationToken == KtTokens.EXCLEQ
        }
        val isContained = binaryExpressions.any { binaryExpression ->
            val left = binaryExpression.left!!.collectDescendantsOfType<KtNameReferenceExpression>().map { it.getReferencedName() }
            val right = binaryExpression.right!!.collectDescendantsOfType<KtNameReferenceExpression>().map { it.getReferencedName() }
            (left.contains(parameter.name) && isContainedInList(right, klass)) || (isContainedInList(left, klass) && right.contains(
                parameter.name
            ))
        }
        if (isContained) return

        val binaryExpressionsWithType = function.collectDescendantsOfType<KtBinaryExpressionWithTypeRHS> {
            it.operationReference.getReferencedName() == "as?"
        }
        val isContainedInExpressionWithType = binaryExpressionsWithType.any { binaryExpression ->
            val left = binaryExpression.left.collectDescendantsOfType<KtNameReferenceExpression>().map { it.getReferencedName() }
            val right = binaryExpression.right!!.collectDescendantsOfType<KtNameReferenceExpression>().map { it.getReferencedName() }
            (left.contains(parameter.name) && isContainedInList(right, klass)) || (isContainedInList(left, klass) && right.contains(
                parameter.name
            ))
        }
        if (isContainedInExpressionWithType) return

        ctx.reportIssue(function, "Add a type test to this method.")
    }

    private fun isContainedInList(list: List<String>, klass: KtClass) =
        list.contains(klass.name) || list.contains(THIS_KEYWORD.value) || list.contains("javaClass")

}
