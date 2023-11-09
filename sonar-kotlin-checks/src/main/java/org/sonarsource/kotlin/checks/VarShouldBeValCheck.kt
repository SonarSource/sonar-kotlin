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

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtOperationExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtUnaryExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContext.REFERENCE_TARGET
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

@Rule(key = "S3353")
class VarShouldBeValCheck : AbstractCheck() {
    override fun visitProperty(property: KtProperty, context: KotlinFileContext) {
        if (property.isLocal && property.isVar) {
            val bindingContext = context.bindingContext
            val block = property.getParentOfType<KtBlockExpression>(false)!!
            val assignments = collectAssignmentTo(block, property, bindingContext)

            if (assignments.isEmpty()) {
                context.reportIssue(property, """Replace the keyword `var` with `val`.""")
            }
        }
    }

    private fun collectAssignmentTo(block: PsiElement, variable: KtProperty, bindingContext: BindingContext): List<KtOperationExpression> {
        KtDotQualifiedExpression
        return block
            .collectDescendantsOfType<KtOperationExpression> { expr -> isAssignment(expr) }
            .filter { assignment ->
                when (assignment) {
                    is KtBinaryExpression -> isReferencingVar(variable, getReference(assignment), bindingContext) ?: false
                    is KtUnaryExpression -> isReferencingVar(variable, getReference(assignment), bindingContext)
                    // I don't know how to cover it
                    else -> false
                }
            }
    }

    private fun getReference(operator: KtUnaryExpression): KtNameReferenceExpression {
        val expr = operator.baseExpression!!
        return if(expr is KtNameReferenceExpression){
            expr
        }else{
            expr.findDescendantOfType<KtNameReferenceExpression>()!!
        }
    }


    private fun isAssignment(expr: KtOperationExpression): Boolean {
        return when (expr.operationReference.getReferencedNameElementType()) {
            KtTokens.EQ -> true
            KtTokens.PLUSEQ -> true
            KtTokens.MINUSEQ -> true
            KtTokens.MULTEQ -> true
            KtTokens.DIVEQ -> true
            KtTokens.PERCEQ -> true
            KtTokens.PLUSPLUS -> true
            KtTokens.MINUSMINUS -> true
            else -> false
        }
    }

    private fun getReference(expr: KtBinaryExpression): KtNameReferenceExpression {
        return expr.left!!.findDescendantOfType<KtNameReferenceExpression>()!!
    }

    private fun isReferencingVar(variable: KtProperty, reference: KtNameReferenceExpression, bindingContext: BindingContext): Boolean {
        val definition =
            bindingContext[REFERENCE_TARGET, reference]?.let { DescriptorToSourceUtils.descriptorToDeclaration(it) as? KtProperty }
        return if(definition == null){
            //no semantics true if same identifier
            variable.name  == reference.getReferencedName()
        }else{
            variable === definition
        }
    }
}
