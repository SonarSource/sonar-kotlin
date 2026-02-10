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
package org.sonarsource.kotlin.plugin

import com.sonarsource.plugins.kotlin.api.KotlinPluginExtensionsProvider
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
import java.util.Optional

@ExperimentalTime
internal class KotlinPluginTest {

    @Test
    fun `should implement KotlinPluginExtensionsProvider`() {
        Assertions.assertThat(KotlinPlugin::class.java.interfaces)
            .describedAs("To please dependency injection framework of SonarQube Cloud")
            .contains(KotlinPluginExtensionsProvider::class.java)
    }

    @Test
    fun testSonarQube() {
        testSonarQube(20)
    }

    @Test
    fun testSonarLint() {
        testSonarLint(7)
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
