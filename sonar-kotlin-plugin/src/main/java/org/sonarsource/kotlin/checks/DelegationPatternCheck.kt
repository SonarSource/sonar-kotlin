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
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptorWithResolutionScopes
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectLiteralExpression
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression
import org.jetbrains.kotlin.psi.psiUtil.isPublic
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassDescriptor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext.eraseContainingTypeParameters
import org.jetbrains.kotlin.types.checker.TypeCheckingProcedure
import org.jetbrains.kotlin.types.typeUtil.immediateSupertypes
import org.jetbrains.kotlin.types.typeUtil.isInterface
import org.jetbrains.kotlin.types.typeUtil.supertypes
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.api.determineType
import org.sonarsource.kotlin.api.isSupertypeOf
import org.sonarsource.kotlin.api.overrides
import org.sonarsource.kotlin.api.returnType
import org.sonarsource.kotlin.api.returnTypeAsString
import org.sonarsource.kotlin.plugin.KotlinFileContext

@Rule(key = "S6514")
class DelegationPatternCheck : AbstractCheck() {

    override fun visitClassOrObject(classOrObject: KtClassOrObject, context: KotlinFileContext) {

        val classType = classOrObject.determineType(context.bindingContext) ?: return
        val superInterfaces = getSuperInterfaces(classType).toSet()
        if (superInterfaces.isEmpty()) {
            return
        }

        classOrObject.declarations.forEach {
            if (it is KtNamedFunction) {
                checkNamedFunction(it, superInterfaces, context)
            }
        }
    }

    private fun checkNamedFunction(function: KtNamedFunction, superInterfaces: Set<KotlinType>, context: KotlinFileContext) {
        if (!(function.isPublic && function.overrides())) {
            return
        }
        val bindingContext = context.bindingContext
        val delegeeType = getDelegeeOrNull(function, bindingContext)?.determineType(bindingContext) ?: return

        // Problem: check if the function is an interface function of an interface that the owner of the function implements

        if (getCommonSuperInterfaces(superInterfaces, delegeeType).any {
            isFunctionInInterface(function, it, bindingContext)
        }) {
            context.reportIssue(function, "USE by FOR DELEGATE")
            // println("DELEGATE: "+function.name)
            // TODO: report problem
        }
    }
}

private fun isFunctionInInterface(function: KtNamedFunction, superInterface: KotlinType, bindingContext: BindingContext): Boolean {
    val classDescriptor = TypeUtils.getClassDescriptor(superInterface) as? ClassDescriptorWithResolutionScopes ?: return false
    return classDescriptor.declaredCallableMembers.any {
        isEqualFunctionSignature(function, it, bindingContext)
    }
}

private fun isEqualFunctionSignature2(
    function: KtNamedFunction,
    functionDescriptor: CallableMemberDescriptor,
    bindingContext: BindingContext
) = function.name == functionDescriptor.name.identifier &&
    function.returnType(bindingContext) == functionDescriptor.returnType &&
    allPaired(function.valueParameters, functionDescriptor.valueParameters) { parameter, paramerterDescriptor ->
        bindingContext.get(BindingContext.TYPE, parameter.typeReference) == paramerterDescriptor.type
    }


private fun isEqualFunctionSignature(
    function: KtNamedFunction,
    functionDescriptor: CallableMemberDescriptor,
    bindingContext: BindingContext
): Boolean {
    val nameMatch = function.name == functionDescriptor.name.identifier
    if (!nameMatch) return false

    //functionDescriptor.returnType.eraseContainingTypeParameters()

    val lhs = function.returnType(bindingContext)
    val rhs = functionDescriptor.returnType
    val returnTypeMatch = lhs?.getJetTypeFqName(false) == rhs?.getJetTypeFqName(false)

    val paramMatch = allPaired(function.valueParameters, functionDescriptor.valueParameters) { parameter, paramerterDescriptor ->
        bindingContext.get(BindingContext.TYPE, parameter.typeReference)?.getJetTypeFqName(false) == paramerterDescriptor.type.getJetTypeFqName(false)
    }

    return returnTypeMatch && paramMatch
}


fun getCommonSuperInterfaces(superInterfaces: Set<KotlinType>, otherType: KotlinType) =
    getSuperInterfaces(otherType).intersect(superInterfaces)

private fun getSuperInterfaces(kotlinType: KotlinType): List<KotlinType> =
    kotlinType.supertypes().filter(KotlinType::isInterface)

private fun getDelegeeOrNull(function: KtNamedFunction, bindingContext: BindingContext): KtNameReferenceExpression? {
    val qualifiedCallExpression = getFunctionSingleBodyElementOrNull(function) as? KtDotQualifiedExpression ?: return null
    val receiverExpression = qualifiedCallExpression.receiverExpression as? KtNameReferenceExpression ?: return null
    val callExpression = qualifiedCallExpression.selectorExpression as? KtCallExpression ?: return null

    return if(function.name!! == callExpression.getCallNameExpression()?.getReferencedName() &&
        function.returnType(bindingContext) == callExpression.determineType(bindingContext) &&
        allPaired(function.valueParameters, callExpression.valueArguments) { parameter, argument ->
            isDelegatedParameter(parameter, argument, bindingContext)
        }) {
        receiverExpression
    } else {
        null
    }
}

// TODO: bindingContext.get(BindingContext.TYPE, parameter.typeReference) -> is there a service function already?

private fun isDelegatedParameter(parameter: KtParameter, arguments: KtValueArgument, bindingContext: BindingContext): Boolean {
    val argumentExpression = arguments.getArgumentExpression() as? KtNameReferenceExpression ?: return false
    return parameter.name == argumentExpression.getReferencedName() &&
        bindingContext.get(BindingContext.TYPE, parameter.typeReference) == argumentExpression.determineType(bindingContext)
}

private fun getFunctionSingleBodyElementOrNull(function: KtNamedFunction): PsiElement? {
    val bodyExpression = function.bodyExpression
    if (bodyExpression !is KtBlockExpression) {
        return bodyExpression
    }

    val singleBodyElement = bodyExpression.children.singleOrNull()
    return if (singleBodyElement is KtReturnExpression) {
        singleBodyElement.returnedExpression
    } else {
        singleBodyElement
    }
}

private inline fun <T, U> allPaired(listT: List<T>, listU: List<U>, predicate: (T, U) -> Boolean) =
    listT.size == listU.size && uncheckedAllPaired(listT, listU, predicate)

private inline fun <T, U> uncheckedAllPaired(listT: List<T>, listU: List<U>, predicate: (T, U) -> Boolean): Boolean {
    listT.forEachIndexed { index, t ->
        if (!predicate(t, listU[index])) {
            return false
        }
    }
    return true
}
