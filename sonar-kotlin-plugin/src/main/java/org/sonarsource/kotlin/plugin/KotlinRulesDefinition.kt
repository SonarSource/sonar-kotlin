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

import org.sonar.api.SonarRuntime
import org.sonar.api.server.rule.RulesDefinition
import org.sonarsource.analyzer.commons.RuleMetadataLoader
import org.sonarsource.kotlin.api.common.KOTLIN_LANGUAGE_KEY
import org.sonarsource.kotlin.api.common.KOTLIN_REPOSITORY_KEY
import org.sonarsource.kotlin.gradle.KOTLIN_GRADLE_CHECKS

class KotlinRulesDefinition(private val runtime: SonarRuntime) : RulesDefinition {

    companion object {
        private const val RESOURCE_FOLDER = "org/sonar/l10n/kotlin/rules/kotlin"
    }

    override fun define(context: RulesDefinition.Context) {
        context
            .createRepository(KOTLIN_REPOSITORY_KEY, KOTLIN_LANGUAGE_KEY)
            .setName(KotlinPlugin.REPOSITORY_NAME).let { repository ->
                val checks = KOTLIN_CHECKS +  KOTLIN_GRADLE_CHECKS
                RuleMetadataLoader(RESOURCE_FOLDER, KotlinProfileDefinition.PATH_TO_JSON, runtime).addRulesByAnnotatedClass(repository, checks)
                repository.done()
            }
    }
}
