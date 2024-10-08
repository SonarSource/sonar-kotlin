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
package org.sonarsource.kotlin.api.checks

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.typeParameters
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaFlexibleType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.codegen.optimization.common.analyze
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.js.descriptorUtils.getKotlinTypeFqName
import org.jetbrains.kotlin.js.descriptorUtils.nameIfStandardType
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.TypeNullability
import org.jetbrains.kotlin.types.typeUtil.nullability

/**
 * Determines whether a given descriptor matches certain criteria. If a criteria is `null`, it will always match. In other words, if
 * an [ArgumentMatcher] is created only will `null` for all criteria, it will match all argument types.
 */
class ArgumentMatcher(
    private val typeName: String? = null,
    private val nullability: TypeNullability? = null,
    private val qualified: Boolean = true,
    internal val isVararg: Boolean = false,
) {
    companion object {
        val ANY = ArgumentMatcher()
    }

    fun matches(descriptor: ValueParameterDescriptor) = (isVararg == (descriptor.varargElementType != null)) &&
        matchesNullability(descriptor) && matchesName(if (isVararg) descriptor.varargElementType else descriptor.type)

    fun matches(descriptor: KaValueParameterSymbol): Boolean {
        if (!qualified) TODO()
        if (nullability != null) TODO()
        if (isVararg) TODO()
        if (typeName == null) return true
        return typeName == fqn(descriptor.returnType)
    }

    private fun fqn(type: KaType): String? {
        return when (type) {
            is KaClassType -> type.classId.asFqNameString().replace('/', '.')
            is KaFlexibleType -> fqn(type.lowerBound)
            else -> null
        }
    }

    private fun matchesName(kotlinType: KotlinType?) =
        if (qualified) matchesQualifiedName(kotlinType) else matchesUnqualifiedName(kotlinType)

    private fun matchesNullability(descriptor: ValueParameterDescriptor) =
        nullability?.let { it == descriptor.type.nullability() } ?: true

    @Deprecated("")
    private fun matchesQualifiedName(kotlinType: KotlinType?) =
        // Note that getKotlinTypeFqName(...) is from the kotlin.js package. We use it anyway,
        // as it seems to be the best option to get a type's fully qualified name
        typeName?.let { it == kotlinType?.getKotlinTypeFqName(false) } ?: true

    private fun matchesUnqualifiedName(kotlinType: KotlinType?) =
        // Note that nameIfStandardType is from the kotlin.js package. We use it anyway,
        // as it seems to be the best option to get a type's simple name
        typeName?.let { it == kotlinType?.nameIfStandardType?.asString() } ?: true
}

val ANY = ArgumentMatcher()
