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
package org.sonarsource.kotlin.plugin

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition

internal class KotlinProfileDefinitionTest {

    @Test
    fun profile() {
        val context = BuiltInQualityProfilesDefinition.Context()
        KotlinProfileDefinition().define(context)
        val profile = context.profile("kotlin", "Sonar way")
        Assertions.assertThat(profile.rules().size).isGreaterThan(2)
        Assertions.assertThat(profile.rules())
            .extracting<String> { obj: BuiltInQualityProfilesDefinition.BuiltInActiveRule -> obj.ruleKey() }
            .contains("S1764")
    }
}
