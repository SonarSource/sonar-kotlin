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
package org.sonarsource.kotlin.plugin

import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition
import com.sonarsource.plugins.kotlin.api.KotlinPluginExtensionsProvider
import org.sonarsource.analyzer.commons.BuiltInQualityProfileJsonLoader
import org.sonarsource.kotlin.api.common.KOTLIN_LANGUAGE_KEY
import org.sonarsource.kotlin.api.common.KOTLIN_REPOSITORY_KEY

class KotlinProfileDefinition(
    private val extensionsProviders: Array<KotlinPluginExtensionsProvider>,
) : BuiltInQualityProfilesDefinition {
    companion object {
        const val PATH_TO_JSON = "org/sonar/l10n/kotlin/rules/kotlin/Sonar_way_profile.json"
    }

    override fun define(context: BuiltInQualityProfilesDefinition.Context) {
        val profile = context.createBuiltInQualityProfile(KotlinPlugin.PROFILE_NAME, KOTLIN_LANGUAGE_KEY)
        KotlinPluginExtensions(extensionsProviders).rulesEnabledInSonarWayProfile().forEach { rule ->
            profile.activateRule(rule.repository(), rule.rule())
        }
        BuiltInQualityProfileJsonLoader.load(profile, KOTLIN_REPOSITORY_KEY, PATH_TO_JSON)
        profile.done()
    }
}
