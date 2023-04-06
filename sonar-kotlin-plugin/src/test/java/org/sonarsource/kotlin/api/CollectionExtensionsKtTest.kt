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
package org.sonarsource.kotlin.api

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