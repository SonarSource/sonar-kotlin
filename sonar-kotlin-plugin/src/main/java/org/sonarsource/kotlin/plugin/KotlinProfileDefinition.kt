/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2024 SonarSource SA
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
import org.sonarsource.analyzer.commons.BuiltInQualityProfileJsonLoader
import org.sonarsource.kotlin.api.common.KOTLIN_LANGUAGE_KEY
import org.sonarsource.kotlin.api.common.KOTLIN_REPOSITORY_KEY

class KotlinProfileDefinition : BuiltInQualityProfilesDefinition {
    companion object {
        const val PATH_TO_JSON = "org/sonar/l10n/kotlin/rules/kotlin/Sonar_way_profile.json"
    }

    override fun define(context: BuiltInQualityProfilesDefinition.Context) {
        context.createBuiltInQualityProfile(KotlinPlugin.PROFILE_NAME, KOTLIN_LANGUAGE_KEY).let { profile ->
            BuiltInQualityProfileJsonLoader.load(profile, KOTLIN_REPOSITORY_KEY, PATH_TO_JSON)
            profile.done()
        }
    }
}
