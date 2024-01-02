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
package org.sonarsource.kotlin.plugin

import org.assertj.core.api.AssertionsForClassTypes
import org.junit.jupiter.api.Test
import org.sonar.api.config.internal.MapSettings
import org.sonarsource.kotlin.api.common.KOTLIN_FILE_SUFFIXES_KEY
import org.sonarsource.kotlin.api.common.KotlinLanguage

internal class KotlinLanguageTest {

    @Test
    fun test_suffixes_default() {
        val kotlinLanguage = KotlinLanguage(MapSettings().asConfig())
        AssertionsForClassTypes.assertThat(kotlinLanguage.fileSuffixes).containsExactlyInAnyOrder(".kt", ".kts")
    }

    @Test
    fun test_suffixes_empty() {
        val kotlinLanguage =
            KotlinLanguage(MapSettings().setProperty(KOTLIN_FILE_SUFFIXES_KEY, "").asConfig())
        AssertionsForClassTypes.assertThat(kotlinLanguage.fileSuffixes).containsExactlyInAnyOrder(".kt", ".kts")
    }

    @Test
    fun test_suffixes_custom() {
        val kotlinLanguage =
            KotlinLanguage(MapSettings().setProperty(KOTLIN_FILE_SUFFIXES_KEY, ".foo, .bar").asConfig())
        AssertionsForClassTypes.assertThat(kotlinLanguage.fileSuffixes).containsExactly(".foo", ".bar")
    }

    @Test
    fun test_key_and_name() {
        val kotlinLanguage = KotlinLanguage(MapSettings().asConfig())
        AssertionsForClassTypes.assertThat(kotlinLanguage.key).isEqualTo("kotlin")
        AssertionsForClassTypes.assertThat(kotlinLanguage.name).isEqualTo("Kotlin")
    }
}
