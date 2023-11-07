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
import org.sonar.api.Plugin
import org.sonar.api.SonarEdition
import org.sonar.api.SonarQubeSide
import org.sonar.api.SonarRuntime
import org.sonar.api.internal.PluginContextImpl
import org.sonar.api.internal.SonarRuntimeImpl
import org.sonar.api.utils.Version
import kotlin.time.ExperimentalTime
import org.sonar.api.config.Configuration
import org.sonarsource.kotlin.gradle.GRADLE_PROJECT_ROOT_PROPERTY
import java.util.Optional

@ExperimentalTime
internal class KotlinPluginTest {
    @Test
    fun testSonarQube() {
        testSonarQube(18)
    }

    @Test
    fun testSonarLint() {
        testSonarLint(5)
    }

    private fun testSonarQube(expectedExtensionsCount: Int, overrideProperties: Map<String, String> = emptyMap()) {
        val runtime = SonarRuntimeImpl.forSonarQube(Version.create(7, 9), SonarQubeSide.SCANNER, SonarEdition.COMMUNITY)
        test(runtime, expectedExtensionsCount, overrideProperties)
    }

    private fun testSonarLint(expectedExtensionsCount: Int, overrideProperties: Map<String, String> = emptyMap()) {
        val runtime = SonarRuntimeImpl.forSonarLint(Version.create(3, 9))
        test(runtime, expectedExtensionsCount, overrideProperties)
    }

    private fun test(runtime: SonarRuntime, expectedExtensionsCount: Int, overrideProperties: Map<String, String>) {
        val kotlinPlugin = KotlinPlugin()
        val context = getPatchedContext(
            PluginContextImpl.Builder().setSonarRuntime(runtime).build(),
            overrideProperties
        )
        kotlinPlugin.define(context)
        Assertions.assertThat(context.extensions).hasSize(expectedExtensionsCount)
    }

    private fun getPatchedContext(context: Plugin.Context, overrideProperties: Map<String, String>): Plugin.Context {
        return if (overrideProperties.isEmpty()) {
            context
        } else {
            val bootConfiguration = OverrideConfiguration(
                context.bootConfiguration,
                overrideProperties
            )
            PluginContextImpl.Builder().setSonarRuntime(context.runtime).setBootConfiguration(bootConfiguration).build()
        }
    }

}

private class OverrideConfiguration(
    private val superConfiguration: Configuration,
    private val overrides: Map<String, String>
): Configuration {

    override fun get(key: String): Optional<String> {
        val overrideValue = overrides[key]
        return if (overrideValue != null) {
            Optional.of(overrideValue)
        } else {
            superConfiguration.get(key)
        }
    }

    override fun hasKey(key: String): Boolean {
        return overrides.keys.contains(key) || superConfiguration.hasKey(key)
    }

    override fun getStringArray(key: String): Array<String> {
        // TODO
        return superConfiguration.getStringArray(key)
    }
}
