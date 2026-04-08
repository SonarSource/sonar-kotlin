/*
 * SonarSource Kotlin
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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
