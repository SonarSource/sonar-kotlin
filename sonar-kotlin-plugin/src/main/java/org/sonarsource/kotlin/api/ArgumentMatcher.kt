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
package org.sonarsource.kotlin.api

import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.js.descriptorUtils.nameIfStandardType
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
) {
    fun matches(descriptor: ValueParameterDescriptor) =
        matchesName(descriptor) && matchesNullability(descriptor)

    private fun matchesName(descriptor: ValueParameterDescriptor) =
        if (qualified) matchesQualifiedName(descriptor) else matchesUnqualifiedName(descriptor)

    private fun matchesNullability(descriptor: ValueParameterDescriptor) =
        nullability?.let { it == descriptor.type.nullability() } ?: true

    private fun matchesQualifiedName(descriptor: ValueParameterDescriptor) =
        // Note that getJetTypeFqName(...) is from the kotlin.js package. We use it anyway,
        // as it seems to be the best option to get a type's fully qualified name
        typeName?.let { it == descriptor.type.getJetTypeFqName(false) } ?: true

    private fun matchesUnqualifiedName(descriptor: ValueParameterDescriptor) =
        // Note that nameIfStandardType is from the kotlin.js package. We use it anyway,
        // as it seems to be the best option to get a type's simple name
        typeName?.let { it == descriptor.type.nameIfStandardType?.asString() } ?: true
}
