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

import org.sonar.api.SonarRuntime
import org.sonar.api.server.rule.RulesDefinition
import com.sonarsource.plugins.kotlin.api.KotlinPluginExtensionsProvider
import org.sonarsource.kotlin.api.common.KOTLIN_LANGUAGE_KEY
import org.sonarsource.kotlin.api.common.KOTLIN_REPOSITORY_KEY
import org.sonarsource.kotlin.gradle.KOTLIN_GRADLE_CHECKS

class KotlinRulesDefinition(
    private val runtime: SonarRuntime,
    private val extensionsProviders: Array<KotlinPluginExtensionsProvider>,
) : RulesDefinition {

    companion object {
        private const val RESOURCE_FOLDER = "org/sonar/l10n/kotlin/rules/kotlin"
    }

    override fun define(context: RulesDefinition.Context) {
        val checks: MutableList<Class<*>> = mutableListOf()
        checks.addAll(KOTLIN_CHECKS)
        checks.addAll(KOTLIN_GRADLE_CHECKS)

        val extensions = KotlinPluginExtensions(extensionsProviders)

        val rulesByRepositoryKey: MutableMap<String, List<Class<*>>> = mutableMapOf()
        rulesByRepositoryKey.putAll(extensions.rulesByRepositoryKey())
        rulesByRepositoryKey[KOTLIN_REPOSITORY_KEY] = checks

        val ruleMetadataLoaderClass = extensions.ruleMetadataLoaderClass()
        val ruleMetadataLoader = ruleMetadataLoaderClass
            .getConstructor(String::class.java, String::class.java, SonarRuntime::class.java)
            .newInstance(RESOURCE_FOLDER, KotlinProfileDefinition.PATH_TO_JSON, runtime)
        val registerRules = { repositoryKey: String, repositoryName: String ->
            val repository = context.createRepository(repositoryKey, KOTLIN_LANGUAGE_KEY)
                .setName(repositoryName)
            ruleMetadataLoaderClass
                .getMethod("addRulesByAnnotatedClass", RulesDefinition.NewRepository::class.java, List::class.java)
                .invoke(ruleMetadataLoader, repository, rulesByRepositoryKey[repositoryKey])
            repository.done()
        }
        registerRules(KOTLIN_REPOSITORY_KEY, KotlinPlugin.REPOSITORY_NAME)
        extensions.repositories().forEach(registerRules)
    }
}
