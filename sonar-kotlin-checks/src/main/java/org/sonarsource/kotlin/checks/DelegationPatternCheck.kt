/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2025 SonarSource SA
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

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.name
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression
import org.jetbrains.kotlin.psi.psiUtil.isPublic
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.checks.allPaired
import org.sonarsource.kotlin.api.checks.determineType
import org.sonarsource.kotlin.api.checks.overrides
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.withKaSession

@Rule(key = "S6514")
class DelegationPatternCheck : AbstractCheck() {

    override fun visitClassOrObject(classOrObject: KtClassOrObject, context: KotlinFileContext) = withKaSession {
        val classSymbol = classOrObject.classSymbol ?: return
        if (classSymbol.classKind == KaClassKind.INTERFACE) return
        val superInterfaces: Set<KaClassSymbol> = classSymbol.getSuperInterfaces()
        if (superInterfaces.isEmpty()) return

        classOrObject.declarations.forEach {
            if (it is KtNamedFunction) {
                checkNamedFunction(it, superInterfaces, context)
            }
        }
    }

    private fun checkNamedFunction(function: KtNamedFunction, superInterfaces: Set<KaClassSymbol>, context: KotlinFileContext) {
        if (!(function.isPublic && function.overrides())) return
        val delegeeType = getDelegeeOrNull(function)?.determineType() ?: return

        if (getCommonSuperInterfaces(superInterfaces, delegeeType).any {
            isFunctionInInterface(function, it)
        }) {
            context.reportIssue(function.nameIdentifier!!, """Replace with interface delegation using "by" in the class header.""")
        }
    }
}

private fun isFunctionInInterface(
    function: KtNamedFunction,
    superInterface1: KaClassSymbol
): Boolean = withKaSession {
    val classDeclaration = superInterface1.psi as? KtClass ?: return false
    return classDeclaration.declarations.any {
        it is KtNamedFunction && haveCompatibleFunctionSignature(it.symbol, function.symbol)
    }
}

private fun haveCompatibleFunctionSignature(
    function1: KaFunctionSymbol,
    function2: KaFunctionSymbol,
) = withKaSession {
    function1.returnType.sameOrTypeParam(function2.returnType) &&
            function1.name == function2.name &&
            function1.valueParameters.allPaired(function2.valueParameters) { p1, p2 ->
                p1.returnType.sameOrTypeParam(p2.returnType)
            }
}

fun KaType.sameOrTypeParam(other: KaType): Boolean = withKaSession {
    if (this@sameOrTypeParam is KaTypeParameterType && other is KaTypeParameterType) return true
    return symbol != null && other.symbol != null && symbol == other.symbol
}

fun getCommonSuperInterfaces(superInterfaces: Set<KaClassSymbol>, otherType: KaType) =
    (otherType.symbol as? KaClassSymbol)?.let {
        (it.getSuperInterfaces() + it).intersect(superInterfaces)
    } ?: emptySet()

fun KaType.getSuperInterfaces(): Set<KaClassSymbol> =
    (symbol as? KaClassSymbol)?.getSuperInterfaces() ?: emptySet()

fun KaClassSymbol.getSuperInterfaces(): Set<KaClassSymbol> = withKaSession {
    val superTypes = this@getSuperInterfaces.superTypes.filter { !it.isAnyType }
    val symbols: Collection<KaClassSymbol> = superTypes.mapNotNull { it.symbol as? KaClassSymbol }
    val hierarchy: List<KaClassSymbol> = superTypes.flatMap { it.getSuperInterfaces() }
    return (symbols + hierarchy)
        .filter { it.classKind == KaClassKind.INTERFACE }
        .toSet()
}

private fun getDelegeeOrNull(function: KtNamedFunction): KtNameReferenceExpression? = withKaSession {
    val qualifiedCallExpression = getFunctionSingleBodyElementOrNull(function) as? KtDotQualifiedExpression ?: return null
    val receiverExpression = qualifiedCallExpression.receiverExpression as? KtNameReferenceExpression ?: return null
    val callExpression = qualifiedCallExpression.selectorExpression as? KtCallExpression ?: return null
    val callExpressionType = callExpression.expressionType ?: return null

    return if (function.name!! == callExpression.getCallNameExpression()?.getReferencedName() &&
        function.returnType.semanticallyEquals(callExpressionType) &&
        function.valueParameters.allPaired(callExpression.valueArguments) { parameter, argument ->
            isDelegatedParameter(parameter, argument)
        }) receiverExpression else null
}

private fun isDelegatedParameter(parameter: KtParameter, arguments: KtValueArgument): Boolean = withKaSession {
    val argumentExpression = arguments.getArgumentExpression() as? KtNameReferenceExpression ?: return false
    if (parameter.name != argumentExpression.getReferencedName()) return false
    val argumentType = argumentExpression.determineType() ?: return false
    return parameter.symbol.returnType.semanticallyEquals(argumentType)
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
