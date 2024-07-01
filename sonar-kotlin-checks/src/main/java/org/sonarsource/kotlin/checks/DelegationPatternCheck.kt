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
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptorWithResolutionScopes
import org.jetbrains.kotlin.js.descriptorUtils.getKotlinTypeFqName
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression
import org.jetbrains.kotlin.psi.psiUtil.isPublic
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.isInterface
import org.jetbrains.kotlin.types.typeUtil.supertypes
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.checks.allPaired
import org.sonarsource.kotlin.api.checks.determineType
import org.sonarsource.kotlin.api.checks.determineTypeAsString
import org.sonarsource.kotlin.api.checks.overrides
import org.sonarsource.kotlin.api.checks.returnType
import org.sonarsource.kotlin.api.checks.returnTypeAsString
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

@Rule(key = "S6514")
class DelegationPatternCheck : AbstractCheck() {

    override fun visitClassOrObject(classOrObject: KtClassOrObject, context: KotlinFileContext) {
        val classType = classOrObject.determineType(context.bindingContext) ?: return
        if (classType.isInterface()) return
        val superInterfaces = getSuperInterfaces(classType).toSet()
        if (superInterfaces.isEmpty()) return

        classOrObject.declarations.forEach {
            if (it is KtNamedFunction) {
                checkNamedFunction(it, superInterfaces, context)
            }
        }
    }

    private fun checkNamedFunction(function: KtNamedFunction, superInterfaces: Set<KotlinType>, context: KotlinFileContext) {
        if (!(function.isPublic && function.overrides())) return
        val bindingContext = context.bindingContext
        val delegeeType = getDelegeeOrNull(function, bindingContext)?.determineType(bindingContext) ?: return

        if (getCommonSuperInterfaces(superInterfaces, delegeeType).any {
            isFunctionInInterface(function, it, bindingContext)
        }) {
            context.reportIssue(function.nameIdentifier!!, """Replace with interface delegation using "by" in the class header.""")
        }
    }
}

private fun isFunctionInInterface(function: KtNamedFunction, superInterface: KotlinType, bindingContext: BindingContext): Boolean {
    val classDescriptor = TypeUtils.getClassDescriptor(superInterface) as? ClassDescriptorWithResolutionScopes ?: return false
    return classDescriptor.declaredCallableMembers.any {
        isEqualFunctionSignature(function, it, bindingContext)
    }
}

private fun isEqualFunctionSignature(
    function: KtNamedFunction,
    functionDescriptor: CallableMemberDescriptor,
    bindingContext: BindingContext,
) = function.name == functionDescriptor.name.identifier &&
    function.returnTypeAsString(bindingContext) == functionDescriptor.returnType?.getKotlinTypeFqName(false) &&
    function.valueParameters.allPaired(functionDescriptor.valueParameters) { parameter, paramerterDescriptor ->
        parameter.typeReference?.determineTypeAsString(bindingContext) == paramerterDescriptor.type.getKotlinTypeFqName(false)
    }

fun getCommonSuperInterfaces(superInterfaces: Set<KotlinType>, otherType: KotlinType) =
    getSuperInterfaces(otherType).intersect(superInterfaces)

private fun getSuperInterfaces(kotlinType: KotlinType): List<KotlinType> =
    kotlinType.supertypes().filter(KotlinType::isInterface).let {
        if (kotlinType.isInterface()) it + kotlinType else it
    }

private fun getDelegeeOrNull(function: KtNamedFunction, bindingContext: BindingContext): KtNameReferenceExpression? {
    val qualifiedCallExpression = getFunctionSingleBodyElementOrNull(function) as? KtDotQualifiedExpression ?: return null
    val receiverExpression = qualifiedCallExpression.receiverExpression as? KtNameReferenceExpression ?: return null
    val callExpression = qualifiedCallExpression.selectorExpression as? KtCallExpression ?: return null

    return if (function.name!! == callExpression.getCallNameExpression()?.getReferencedName() &&
        function.returnType(bindingContext) == callExpression.determineType(bindingContext) &&
        function.valueParameters.allPaired(callExpression.valueArguments) { parameter, argument ->
            isDelegatedParameter(parameter, argument, bindingContext)
        }) receiverExpression else null
}

private fun isDelegatedParameter(parameter: KtParameter, arguments: KtValueArgument, bindingContext: BindingContext): Boolean {
    val argumentExpression = arguments.getArgumentExpression() as? KtNameReferenceExpression ?: return false
    return parameter.name == argumentExpression.getReferencedName() &&
        parameter.determineType(bindingContext) == argumentExpression.determineType(bindingContext)
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
