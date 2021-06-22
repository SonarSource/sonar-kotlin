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
package org.sonarsource.kotlin.plugin

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtLoopExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.KtWhenEntry

class CyclomaticComplexityVisitor : KtTreeVisitorVoid() {
    private val complexityTrees = mutableListOf<PsiElement>()

    fun complexityTrees() = complexityTrees.toList()

    override fun visitNamedFunction(function: KtNamedFunction) {
        if(function.hasBody() && function.name != null) {
            complexityTrees.add(function)
        }
        super.visitNamedFunction(function)
    }

    override fun visitIfExpression(expression: KtIfExpression) {
        complexityTrees.add(expression.ifKeyword)
        super.visitIfExpression(expression)
    }

    override fun visitLoopExpression(loopExpression: KtLoopExpression) {
        complexityTrees.add(loopExpression)
        super.visitLoopExpression(loopExpression)
    }

    override fun visitWhenEntry(whenEntry: KtWhenEntry) {
        complexityTrees.add(whenEntry)
        super.visitWhenEntry(whenEntry)
    }

    override fun visitBinaryExpression(expression: KtBinaryExpression) {
        if (expression.operationToken == KtTokens.ANDAND || expression.operationToken == KtTokens.OROR) {
            complexityTrees.add(expression)
        }
        super.visitBinaryExpression(expression)
    }
}
