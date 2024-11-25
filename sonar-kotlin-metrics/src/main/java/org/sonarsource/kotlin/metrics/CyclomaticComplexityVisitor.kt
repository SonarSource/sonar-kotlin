/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2024 SonarSource SA
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
package org.sonarsource.kotlin.metrics

import com.intellij.psi.PsiElement
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
