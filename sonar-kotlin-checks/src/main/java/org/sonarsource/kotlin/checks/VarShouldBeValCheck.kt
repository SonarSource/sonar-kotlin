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
package org.sonarsource.kotlin.checks

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtParenthesizedExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtUnaryExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.isPrivate
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContext.DECLARATION_TO_DESCRIPTOR
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.reporting.message

@Rule(key = "S3353")
class VarShouldBeValCheck : AbstractCheck() {
    override fun visitKtFile(file: KtFile, data: KotlinFileContext) {
        val bindingContext: BindingContext = data.bindingContext

        val binaryAssignments = file.collectDescendantsOfType<KtBinaryExpression> { it.isAssignment() }
        val unaryAssignments = file.collectDescendantsOfType<KtUnaryExpression> { it.isAssignment() }
        val callableVarReference = file.collectDescendantsOfType<KtCallableReferenceExpression>()

        val assignedExpressions = listOf(binaryAssignments.mapNotNull { it.reference() },
            unaryAssignments.mapNotNull { it.reference() },
            callableVarReference.map { it.callableReference }).flatten()

        val assignedDeclarations = assignedExpressions
            .mapNotNull { it.getResolvedCall(bindingContext)?.resultingDescriptor?.original }
            .toSet()

        val assignedNames = assignedExpressions
            .map { it.getReferencedName() }
            .toSet()

        val varProperties = file.collectDescendantsOfType<KtProperty> {
            (it.localVar() || it.privateClassVar()) && !it.isLateInit()
        }

        val msg = message {
            +"Replace the keyword "
            code("var")
            +" with "
            code("val")
            +". This property is never modified."
        }

        varProperties
            .filter { it.isNotReferenced(assignedDeclarations, assignedNames, bindingContext) }
            .forEach { variable ->
                data.reportIssue(variable.valOrVarKeyword, msg)
            }

        val destructedDeclaration = file.collectDescendantsOfType<KtDestructuringDeclaration> { it.isVar }
        destructedDeclaration
            .filter {
                it.collectDescendantsOfType<KtDestructuringDeclarationEntry>()
                    .all { variable -> variable.isNotReferenced(assignedDeclarations, assignedNames, bindingContext) }
            }
            .forEach { variable ->
                data.reportIssue(variable.valOrVarKeyword!!, msg)
            }
    }

    private fun KtProperty.isLateInit(): Boolean = hasModifier(KtTokens.LATEINIT_KEYWORD)
    private fun KtProperty.localVar(): Boolean = this.isLocal && this.isVar
    private fun KtProperty.privateClassVar(): Boolean = this.isPrivate() && this.isVar
    private fun KtNamedDeclaration.isNotReferenced(
        assignedDeclarations: Set<CallableDescriptor>,
        assignedNames: Set<String>,
        bindingContext: BindingContext,
    ): Boolean {
        val descriptor = bindingContext[DECLARATION_TO_DESCRIPTOR, this]
        val name = this.name!!

        return if (descriptor != null) {
            !assignedDeclarations.contains(descriptor)
        } else {
            !assignedNames.contains(name)
        }
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
        return assignedExpression(this.left!!)
    }

    private fun KtUnaryExpression.reference(): KtNameReferenceExpression? {
        return assignedExpression(this.baseExpression!!)
    }

    private fun assignedExpression(expr: KtExpression): KtNameReferenceExpression? {
        return when (expr) {
            is KtNameReferenceExpression -> expr
            is KtParenthesizedExpression -> assignedExpression(expr.expression!!)
            is KtDotQualifiedExpression -> expr.selectorExpression?.let { assignedExpression(it) }
            else -> null
        }
    }

}
