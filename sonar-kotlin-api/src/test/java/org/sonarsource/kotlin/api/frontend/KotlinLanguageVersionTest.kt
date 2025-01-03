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
package org.sonarsource.kotlin.api.frontend

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.config.LanguageVersion
import org.junit.jupiter.api.Test

/**
 * Failure of this test indicates a need to update documentation regarding supported Kotlin versions.
 */
class KotlinLanguageVersionTest {

    @Test
    fun first_supported() {
        assertThat(LanguageVersion.FIRST_SUPPORTED).isEqualTo(LanguageVersion.KOTLIN_1_4)
    }

    @Test
    fun first_non_deprecated() {
        assertThat(LanguageVersion.FIRST_NON_DEPRECATED).isEqualTo(LanguageVersion.KOTLIN_1_7)
    }

    @Test
    fun latest_stable() {
        assertThat(LanguageVersion.LATEST_STABLE).isEqualTo(LanguageVersion.KOTLIN_2_0)
    }

}
