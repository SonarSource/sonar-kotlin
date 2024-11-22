/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2024 SonarSource SA
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

import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.js.descriptorUtils.getKotlinTypeFqName
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.*
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

private val nonMutatingFunctions = FunMatcher {
    withDefiningSupertypes(
        "kotlin.collections.List",
        "kotlin.collections.Set",
        "kotlin.collections.Map",
        "kotlin.collections.Collection",
    )
    withNames(
        "size",
        "isEmpty",
        "contains",
        "containsAll",
        "get",
        "indexOf",
        "lastIndexOf",
        "containsKey",
        "containsValue",
    )
}

private val imMutableCollections =
    setOf(
        "kotlin.collections.Iterable",
        "kotlin.collections.List",
        "kotlin.collections.Set",
        "kotlin.collections.Map",
        "kotlin.collections.Collection"
    )

private val mutatingTokes = setOf(KtTokens.PLUSEQ, KtTokens.MINUSEQ)

private val mutableCollections =
    setOf(
        "kotlin.collections.MutableList",
        "kotlin.collections.MutableSet",
        "kotlin.collections.MutableMap",
        "kotlin.collections.MutableCollection"
    )

@org.sonarsource.kotlin.api.frontend.K1only("easy?")
@Rule(key = "S6524")
class CollectionShouldBeImmutableCheck : AbstractCheck() {


    override fun visitNamedFunction(function: KtNamedFunction, kotlinFileContext: KotlinFileContext) {
        if (
            function.isAbstract() ||
            function.overrides() ||
            function.isOpen() ||
            function.isExpect() ||
            function.isActual() ||
            !function.hasBody()
        ) return

        val bindingContext = kotlinFileContext.bindingContext
        val mutableCollectionsParameters =
            function.valueParameters.filter {
                it.determineType(bindingContext)?.getKotlinTypeFqName(false) in mutableCollections
            }
        val mutableCollectionsVariables =
            function.collectDescendantsOfType<KtVariableDeclaration> {
                it.determineTypeAsString(bindingContext) in mutableCollections
            }

        mutableCollectionsParameters.filter { !it.isMutated(bindingContext, function) }.forEach { parameter ->
            kotlinFileContext.reportIssue(parameter, "Make this collection immutable.")
        }

        mutableCollectionsVariables.filter { !it.isMutated(bindingContext, function) }.forEach { parameter ->
            kotlinFileContext.reportIssue(parameter, "Make this collection immutable.")
        }

    }

    private fun KtCallableDeclaration.isMutated(bindingContext: BindingContext, function: KtNamedFunction): Boolean {
        val usages =
            function.collectDescendantsOfType<KtNameReferenceExpression> { ref -> ref.getReferencedName() == name }
        return usages.any {
            it.parent.skipParentParentheses().isMutatingUsage(bindingContext)
        }
    }

        private fun PsiElement?.isMutatingUsage(bindingContext: BindingContext): Boolean {
            return when(this) {

                is KtDotQualifiedExpression -> {
                    val resolvedCall = getResolvedCall(bindingContext)
                    !(resolvedCall matches nonMutatingFunctions) &&
                            resolvedCall?.resultingDescriptor?.extensionReceiverParameter?.type
                                ?.getKotlinTypeFqName(false) !in imMutableCollections
                }

                is KtValueArgument -> {
                    val resolvedCall = parent.parentOfType<KtCallExpression>().getResolvedCall(bindingContext)
                    val parameterIndex = (parent as? KtValueArgumentList)?.arguments?.indexOf(this) ?: -1
                    if (parameterIndex < 0) {
                        false
                    } else {
                        resolvedCall?.resultingDescriptor?.valueParameters?.get(parameterIndex)?.type?.let {
                            if (it.constructor.declarationDescriptor != null)
                                it.getKotlinTypeFqName(false)
                            else null
                        } !in imMutableCollections
                    }
                }

                is KtArrayAccessExpression -> {
                    val preparent = parent.skipParentParentheses()
                    preparent is KtBinaryExpression && preparent.operationToken == KtTokens.EQ
                }

                is KtBinaryExpression -> {
                    operationToken in mutatingTokes
                }

                is KtUnaryExpression ->
                    baseExpression?.isMutatingUsage(bindingContext) ?: true

                is KtContainerNode ->
                    false

                else -> true
        }
    }
}
