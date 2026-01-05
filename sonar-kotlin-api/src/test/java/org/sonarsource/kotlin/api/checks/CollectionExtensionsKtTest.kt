/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2026 SonarSource SA
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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class CollectionExtensionsKtTest {

    @Test
    fun test() {
        val list = listOf(8, 2, 5, 4, 9, 3, 3, 4)

        val listA = emptyList<Int>()
        val listB = listOf(8, 2, 5, 4, 9)
        val listC = listOf(8, 2, 5, 4, 9, 3, 3, 5)
        val listD = listOf(8, 2, 5, 2, 9, 3, 3, 4)
        val listE = listOf(9, 2, 5, 4, 9, 3, 3, 4)
        val listF = listOf(8, 2, 5, 4, 9, 3, 3, 4)

        assertThat(list.allPaired(list, ::isEqual)).isTrue()
        assertThat(list.allPaired(listA, ::isEqual)).isFalse()
        assertThat(list.allPaired(listB, ::isEqual)).isFalse()
        assertThat(list.allPaired(listC, ::isEqual)).isFalse()
        assertThat(list.allPaired(listD, ::isEqual)).isFalse()
        assertThat(list.allPaired(listE, ::isEqual)).isFalse()
        assertThat(list.allPaired(listF, ::isEqual)).isTrue()

        assertThat(listA.allPaired(list, ::isEqual)).isFalse()
        assertThat(listA.allPaired(listA, ::isEqual)).isTrue()
    }
}

private fun isEqual(a: Int, b: Int): Boolean = a == b