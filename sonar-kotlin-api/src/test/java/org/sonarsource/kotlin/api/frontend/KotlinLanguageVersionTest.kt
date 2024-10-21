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
package org.sonarsource.kotlin.api.frontend

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.config.LanguageVersion
import org.junit.jupiter.api.Test

// TODO add comment
class KotlinLanguageVersionTest {

    @Test
    fun first_supported() {
        assertThat(LanguageVersion.FIRST_SUPPORTED).isEqualTo(LanguageVersion.KOTLIN_1_4)
    }

    @Test
    fun first_non_deprecated() {
        assertThat(LanguageVersion.FIRST_NON_DEPRECATED).isEqualTo(LanguageVersion.KOTLIN_1_7)
    }

}
