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
package org.sonarsource.kotlin.api.checks

import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.types.typeUtil.TypeNullability

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

    fun matches(valueParameter: KaValueParameterSymbol): Boolean {
        if (!qualified) TODO()
        if (nullability != null) TODO()
        if (isVararg) TODO()
        if (typeName == null) return true
        return typeName == valueParameter.returnType.asFqNameString()
    }
}
