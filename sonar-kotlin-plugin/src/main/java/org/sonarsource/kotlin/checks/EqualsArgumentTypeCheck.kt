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
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtIsExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
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

    override fun visitClassBody(klass: KtClassBody, ctx: KotlinFileContext) {
        val equalsMethod = klass.functions.find {
            EQUALS_MATCHER.matches(it, ctx.bindingContext)
        } ?: return

        val parameter = equalsMethod.valueParameters[0]
        val klassNames = (klass.parent as KtClass).collectDescendantsOfType<KtClass>().map {
            it.name
        }
        val klassName = (klass.parent as KtClass).name

        val hasIsExpression =
            equalsMethod.collectDescendantsOfType<KtIsExpression> { parameter.name == (it.leftHandSide as KtNameReferenceExpression).getReferencedName() }
                .flatMap { it.typeReference!!.text.split(".") } // class.childClass case
                .any { it -> klassNames.contains(it) }


        val binaryExpressions =
            equalsMethod.collectDescendantsOfType<KtBinaryExpression> { it.operationToken == KtTokens.EQEQ || it.operationToken == KtTokens.EXCLEQ }

        var isContained = false
        run lit@{
            binaryExpressions.forEach { binaryExpression ->
                val left = binaryExpression.left!!.collectDescendantsOfType<KtNameReferenceExpression>().map { it.getReferencedName() }
                val right = binaryExpression.right!!.collectDescendantsOfType<KtNameReferenceExpression>().map { it.getReferencedName() }

                isContained =
                    (left.contains(parameter.name) && (right.contains(klassName) || right.contains(THIS_KEYWORD.value))|| right.contains("javaClass"))
                        ((left.contains(klassName) || left.contains(THIS_KEYWORD.value) || left.contains("javaClass")) && right.contains(parameter.name))
                if (isContained)
                    return@lit
            }
        }

        equalsMethod.let {
            if (!it.isAbstract() && !hasIsExpression && !isContained) ctx.reportIssue(it, "Add a type test to this method.")
        }
    }

}
