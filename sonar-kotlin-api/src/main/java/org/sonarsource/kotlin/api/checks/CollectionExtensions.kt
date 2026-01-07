/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2026 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1.0.1, as published by SonarSource Sàrl.
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

/**
 * Returns `true` if the predicate does match for all element pairs (`this[i]`,`other[i]`) of both lists.
 * This implies that both lists must have the same size.
 */
@Generated
inline fun <T, U> List<T>.allPaired(other: List<U>, predicate: (T, U) -> Boolean) =
    size == other.size && run {
        forEachIndexed { index, value ->
            if (!predicate(value, other[index])) {
                return false
            }
        }
        true
    }

private annotation class Generated
