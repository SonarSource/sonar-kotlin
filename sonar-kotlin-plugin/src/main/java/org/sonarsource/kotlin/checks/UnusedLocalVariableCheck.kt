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
package org.sonarsource.kotlin.checks

import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.util.containingNonLocalDeclaration
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.plugin.KotlinFileContext

/**
 * Replacement for [org.sonarsource.slang.checks.UnusedLocalVariableCheck]
 */
@Rule(key = "S1481")
class UnusedLocalVariableCheck : AbstractCheck() {

    override fun visitNamedFunction(function: KtNamedFunction, context: KotlinFileContext) {
        if (function.isLocal) return
        function.checkUnusedVariables(context)
    }

    override fun visitSecondaryConstructor(constructor: KtSecondaryConstructor, context: KotlinFileContext) {
        constructor.checkUnusedVariables(context)
    }

    override fun visitLambdaExpression(expression: KtLambdaExpression, context: KotlinFileContext) {
        if (expression.containingNonLocalDeclaration() !is KtFunction) {
            expression.checkUnusedVariables(context)
        }
    }

    private fun KtElement.checkUnusedVariables(context: KotlinFileContext) {
        forEachDescendantOfType<KtProperty> { property ->
            val nameIdentifier = property.nameIdentifier!!
            if (property.isLocal && !anyDescendantOfType<KtNameReferenceExpression> { 
                        reference -> reference.text == nameIdentifier.text 
            }) {
                context.reportIssue(nameIdentifier,
                    "Remove this unused \"${nameIdentifier.text}\" local variable.")
            }
        }
    }
}
