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

import org.jetbrains.kotlin.js.descriptorUtils.getKotlinTypeFqName
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.isPrivate
import org.jetbrains.kotlin.psi.psiUtil.isProtected
import org.jetbrains.kotlin.psi.psiUtil.isPublic
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isNullable
import org.jetbrains.kotlin.util.isAnnotated
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.checks.isAbstract
import org.sonarsource.kotlin.api.checks.overrides
import org.sonarsource.kotlin.api.checks.returnType
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.frontend.secondaryOf

private val GETTER_PREFIX = Regex("""^(get|is)\p{javaUpperCase}""")
private val SETTER_PREFIX = Regex("""^set\p{javaUpperCase}""")

@Rule(key = "S6512")
class PropertyGetterAndSetterUsageCheck : AbstractCheck() {

    override fun visitClass(klass: KtClass, ctx: KotlinFileContext) {
        val classBody = klass.body ?: return
        val javaAccessors = classBody.functions
            .filter { isGetterOrSetter(it.valueParameters.size, it.name ?: "") }
            .groupBy { it.name ?: "" }

        if (javaAccessors.isEmpty()) return
        val bindingCtx = ctx.bindingContext
        classBody.properties
            .filter { it.isReadyForAccessorsConversion() }
            .forEach { checkProperty(it, javaAccessors, ctx, bindingCtx) }
    }

    private fun checkProperty(
        prop: KtProperty, javaAccessors: Map<String, List<KtNamedFunction>>, ctx: KotlinFileContext, bindingCtx: BindingContext
    ) {
        prop.nameIdentifier?.let { propIdentifier ->
            prop.typeReference?.let { bindingCtx[BindingContext.TYPE, it] }?.let { propType ->
                val propName = prop.name!!
                val getterFunc = findJavaStyleGetterFunc(propName, propType, javaAccessors, bindingCtx)
                val getterName = getterFunc?.nameIdentifier
                if (getterName == null || getterFunc.isIncompatiblePropertyAccessor()) return
                val secondaries = mutableListOf(ctx.secondaryOf(propIdentifier, """Property "$propName""""))
                val setterFunc = findJavaStyleSetterFunc(propName, propType, javaAccessors, bindingCtx)
                if (setterFunc != null) {
                    if (setterFunc.isIncompatiblePropertyAccessor() || getterFunc.isLessVisibleThan(setterFunc)) return
                    setterFunc.nameIdentifier?.let { secondaries += ctx.secondaryOf(it, """Setter to convert to a "set(value)"""") }
                }
                ctx.reportIssue(getterName, """Convert this getter to a "get()" on the property "$propName".""", secondaries)
            }
        }
    }
}

private fun KtProperty.isReadyForAccessorsConversion(): Boolean = isPrivate() && accessors.isEmpty() && !isAnnotated

private fun isGetterOrSetter(parameterCount: Int, functionName: String) =
    (parameterCount == 0 && GETTER_PREFIX.find(functionName) != null) ||
        (parameterCount == 1 && SETTER_PREFIX.find(functionName) != null)

private fun findJavaStyleGetterFunc(
    propName: String, propType: KotlinType, javaAccessors: Map<String, List<KtNamedFunction>>, bindingCtx: BindingContext
): KtNamedFunction? {
    val capitalizedName = capitalize(propName)
    val functionsPrefixedByIs = if (propType.matches("kotlin.Boolean")) {
        javaAccessors.getOrElse("is${capitalizedName}") { emptyList() }
    } else {
        emptyList()
    }
    return (javaAccessors.getOrElse("get${capitalizedName}") { emptyList() } + functionsPrefixedByIs)
        .filter { it.returnType(bindingCtx) == propType }
        .unambiguousFunction()
}

private fun parameterMatchesType(parameter: KtParameter, type: KotlinType, bindingCtx: BindingContext): Boolean {
    return !parameter.isVarArg && type == (parameter.typeReference?.let { bindingCtx[BindingContext.TYPE, it] })
}

private fun findJavaStyleSetterFunc(
    propName: String, propType: KotlinType, javaAccessors: Map<String, List<KtNamedFunction>>, bindingCtx: BindingContext
): KtNamedFunction? =
    javaAccessors.getOrElse("set${capitalize(propName)}") { emptyList() }
        .filter { it.returnType(bindingCtx)?.matches("kotlin.Unit") ?: false }
        // isGetterOrSetter ensures setters have: valueParameters.size == 1
        .filter { parameterMatchesType(it.valueParameters[0], propType, bindingCtx) }
        .unambiguousFunction()

private fun List<KtNamedFunction>.unambiguousFunction() = if (this.size == 1) this[0] else null /* empty or ambiguous */

private fun capitalize(name: String): String = name.replaceFirstChar { it.uppercase() }

private fun KotlinType.matches(qualifiedTypeName: String) = !isNullable() && getKotlinTypeFqName(true) == qualifiedTypeName

private fun KtNamedFunction.isIncompatiblePropertyAccessor(): Boolean = isAbstract() || overrides() || isAnnotated

private fun KtNamedFunction.isLessVisibleThan(other: KtNamedFunction): Boolean = when {
    isInternal() -> other.isPublic || other.isProtected()
    isProtected() -> other.isPublic || other.isInternal()
    isPrivate() -> !other.isPrivate()
    // isPublic
    else -> false
}

private fun KtModifierListOwner.isInternal(): Boolean = hasModifier(KtTokens.INTERNAL_KEYWORD)
