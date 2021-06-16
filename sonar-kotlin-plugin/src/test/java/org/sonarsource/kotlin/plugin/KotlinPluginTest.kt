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
package org.sonarsource.kotlin.plugin

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.sonar.api.SonarEdition
import org.sonar.api.SonarQubeSide
import org.sonar.api.internal.PluginContextImpl
import org.sonar.api.internal.SonarRuntimeImpl
import org.sonar.api.utils.Version

internal class KotlinPluginTest {
    @Test
    fun test() {
        val runtime = SonarRuntimeImpl.forSonarQube(Version.create(7, 9), SonarQubeSide.SCANNER, SonarEdition.COMMUNITY)
        val context = PluginContextImpl.Builder().setSonarRuntime(runtime).build()
        val kotlinPlugin = KotlinPlugin()
        kotlinPlugin.define(context)
        Assertions.assertThat(context.extensions).hasSize(17)
    }

    @Test
    fun test_sonarlint() {
        val runtime = SonarRuntimeImpl.forSonarLint(Version.create(3, 9))
        val context = PluginContextImpl.Builder().setSonarRuntime(runtime).build()
        val kotlinPlugin = KotlinPlugin()
        kotlinPlugin.define(context)
        Assertions.assertThat(context.extensions).hasSize(4)
    }
}
