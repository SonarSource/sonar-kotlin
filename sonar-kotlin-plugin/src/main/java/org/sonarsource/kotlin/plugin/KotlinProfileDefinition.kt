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

import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition
import org.sonarsource.analyzer.commons.BuiltInQualityProfileJsonLoader

class KotlinProfileDefinition : BuiltInQualityProfilesDefinition {
    companion object {
        const val PATH_TO_JSON = "org/sonar/l10n/kotlin/rules/kotlin/Sonar_way_profile.json"
    }

    override fun define(context: BuiltInQualityProfilesDefinition.Context) {
        context.createBuiltInQualityProfile(KotlinPlugin.PROFILE_NAME, KotlinPlugin.KOTLIN_LANGUAGE_KEY).let { profile ->
            BuiltInQualityProfileJsonLoader.load(profile, KotlinPlugin.KOTLIN_REPOSITORY_KEY, PATH_TO_JSON)
            profile.done()
        }
    }
}
