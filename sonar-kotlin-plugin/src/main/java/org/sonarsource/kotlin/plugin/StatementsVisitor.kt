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

import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtLoopExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.KtWhenExpression

class StatementsVisitor : KtTreeVisitorVoid() {
    var statements = 0

    override fun visitBlockExpression(expression: KtBlockExpression) {
        expression.statements
            .filter { !it.isDeclaration() }
            .forEach { it.checkStatements() }
        super.visitBlockExpression(expression)
    }

    override fun visitNamedFunction(function: KtNamedFunction) {
        function.bodyExpression?.checkStatements()
        super.visitNamedFunction(function)
    }

    override fun visitProperty(property: KtProperty) {
        if (property.isLocal) return
        property.initializer?.checkStatements()
        super.visitProperty(property)
    }

    private fun KtExpression.checkStatements() = let {
        if (it !is KtBlockExpression) {
            processExpression(it)
            statements++
        }
    }

    private fun processExpression(expression: KtExpression) {
        when (expression) {
            is KtIfExpression -> processIfExpression(expression)
            is KtWhenExpression -> processWhenExpression(expression)
            is KtLoopExpression -> processLoopExpression(expression)
        }
    }

    private fun processIfExpression(expression: KtIfExpression) {
        if (expression.then != null && expression.then !is KtBlockExpression) {
            statements++
        }
        if (expression.`else` != null && expression.`else` !is KtBlockExpression) {
            statements++
        }
    }

    private fun processWhenExpression(expression: KtWhenExpression) {
        expression.entries
            .filterNot { it.expression is KtBlockExpression }
            .forEach { statements++ }
    }

    private fun processLoopExpression(loopExpression: KtLoopExpression) {
        if (loopExpression.body !is KtBlockExpression) statements++
    }
}

private fun KtElement.isDeclaration() =
    this is KtClassOrObject
        || this is KtNamedFunction
        || this is KtPackageDirective
        || this is KtImportDirective
