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
package org.sonarsource.kotlin.metrics

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

private fun KtElement.isDeclaration() =
    this is KtClassOrObject
        || this is KtNamedFunction
        || this is KtPackageDirective
        || this is KtImportDirective

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