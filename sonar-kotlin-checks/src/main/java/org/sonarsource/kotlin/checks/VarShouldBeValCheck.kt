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
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtUnaryExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.isPrivate
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContext.DECLARATION_TO_DESCRIPTOR
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

@Rule(key = "S3353")
class VarShouldBeValCheck : AbstractCheck() {

    override fun visitKtFile(file: KtFile, data: KotlinFileContext?) {
        super.visitKtFile(file, data)
        val context = data ?: return
        val bindingContext: BindingContext = context.bindingContext

        val varProperties = file.collectDescendantsOfType<KtProperty> { it.localVar() || it.isPrivate() }
        val destructedVar = file.collectDescendantsOfType<KtDestructuringDeclarationEntry> { it.localVar() }
        val allVars: List<KtNamedDeclaration> = varProperties + destructedVar

        val binaryAssignments = file.collectDescendantsOfType<KtBinaryExpression> { it.isAssignment() }
        val unaryAssignments = file.collectDescendantsOfType<KtUnaryExpression> { it.isAssignment() }
        val assignedExpressions = binaryAssignments.mapNotNull { it.reference() } + unaryAssignments.mapNotNull { it.reference() }

        val declarationToAssignment = assignedExpressions.groupBy { it.getResolvedCall(bindingContext)?.resultingDescriptor?.original }
        val nameToAssignment = assignedExpressions.groupBy { it.getReferencedName() }

        allVars.forEach { variable ->
            val descriptor = bindingContext[DECLARATION_TO_DESCRIPTOR, variable]
            val name = variable.name!!

            val nameNotReferenced = nameToAssignment[name] == null
            val isNotReferenced = declarationToAssignment[descriptor] == null

            //first check in case we don't have semantics, second check use semantics
            if (nameNotReferenced || (descriptor != null && isNotReferenced)) {
                context.reportIssue(variable, """Replace the keyword `var` with `val`.""")
            }
        }
    }

    private fun KtProperty.localVar(): Boolean {
        return this.isLocal && this.isVar
    }

    private fun KtDestructuringDeclarationEntry.localVar(): Boolean {
        return this.isVar
    }

    private fun KtBinaryExpression.isAssignment(): Boolean {
        return this.operationReference.getReferencedNameElementType() in setOf(
            KtTokens.EQ,
            KtTokens.PLUSEQ,
            KtTokens.MINUSEQ,
            KtTokens.MULTEQ,
            KtTokens.DIVEQ,
            KtTokens.PERCEQ
        )
    }

    private fun KtUnaryExpression.isAssignment(): Boolean {
        return this.operationReference.getReferencedNameElementType() in setOf(
            KtTokens.PLUSPLUS,
            KtTokens.MINUSMINUS
        )
    }

    private fun KtBinaryExpression.reference(): KtNameReferenceExpression? {
        return this.left!!.findDescendantOfType<KtNameReferenceExpression>()
    }

    private fun KtUnaryExpression.reference(): KtNameReferenceExpression? {
        return this.findDescendantOfType<KtNameReferenceExpression>()
    }

}
