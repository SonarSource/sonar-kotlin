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

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.sonar.api.SonarEdition
import org.sonar.api.SonarQubeSide
import org.sonar.api.internal.SonarRuntimeImpl
import org.sonar.api.rule.RuleScope
import org.sonar.api.rules.RuleType
import org.sonar.api.server.rule.RulesDefinition
import org.sonar.api.utils.Version

internal class KotlinRulesDefinitionTest {

    @Test
    fun rules() {
        val repository = repositoryForVersion(Version.create(8, 9))
        Assertions.assertThat(repository!!.name()).isEqualTo("Sonar")
        Assertions.assertThat(repository.language()).isEqualTo("kotlin")
        val rule = repository.rule("S1764")!!
        Assertions.assertThat(rule.name())
            .isEqualTo("Identical expressions should not be used on both sides of a binary operator")
        Assertions.assertThat(rule.type()).isEqualTo(RuleType.BUG)
        Assertions.assertThat(rule.scope()).isEqualTo(RuleScope.ALL)
        val htmlDescription = rule.htmlDescription()!!.lines()
        Assertions.assertThat(htmlDescription.getOrNull(0))
            .startsWith("<h2>Why is this an issue?</h2>")
        Assertions.assertThat(htmlDescription.getOrNull(1))
            .startsWith("<p>Using the same value on both sides")
        val ruleWithConfig = repository.rule("S100")
        val param = ruleWithConfig!!.param("format")
        Assertions.assertThat(param!!.defaultValue()).isEqualTo("^[a-zA-Z][a-zA-Z0-9]*$")
    }

    @Test
    fun owaspSecurityStandard2021() {
        val repository: RulesDefinition.Repository? = repositoryForVersion(Version.create(9, 3))
        val rule = repository?.rule("S1313")!!
        Assertions.assertThat(rule.securityStandards()).containsExactlyInAnyOrder("owaspTop10:a3", "owaspTop10-2021:a1")
    }

    @Test
    fun owaspSecurityStandard() {
        val repository: RulesDefinition.Repository? = repositoryForVersion(Version.create(8, 9))
        val rule = repository?.rule("S1313")!!
        Assertions.assertThat(rule.securityStandards()).containsExactly("owaspTop10:a3")
    }

    @Test
    fun pciDssSecurityStandard() {
        val repository: RulesDefinition.Repository? = repositoryForVersion(Version.create(9, 9))
        val rule = repository?.rule("S6288")!!
        Assertions.assertThat(rule.securityStandards())
            .containsExactly("cwe:522", "owaspAsvs-4.0:2.10.3", "owaspTop10-2021:a4", "pciDss-3.2:6.5.8", "pciDss-4.0:6.2.4")
    }

    private fun repositoryForVersion(version: Version): RulesDefinition.Repository? {
        val rulesDefinition: RulesDefinition = KotlinRulesDefinition(
            SonarRuntimeImpl.forSonarQube(version, SonarQubeSide.SCANNER, SonarEdition.COMMUNITY),
            emptyArray(),
        )
        val context = RulesDefinition.Context()
        rulesDefinition.define(context)
        return context.repository("kotlin")
    }

    @Test
    fun extensions() {
        val rulesDefinition = KotlinRulesDefinition(
            SonarRuntimeImpl.forSonarQube(Version.create(9, 9), SonarQubeSide.SCANNER, SonarEdition.COMMUNITY),
            arrayOf(DummyKotlinPluginExtensionsProvider()),
        )
        val context = RulesDefinition.Context()
        rulesDefinition.define(context)

        val repository = context.repository(DummyKotlinPluginExtensionsProvider.DUMMY_REPOSITORY_KEY)!!
        Assertions.assertThat(repository.language()).isEqualTo("kotlin")
        Assertions.assertThat(repository.name()).isEqualTo("Dummy Repository")
        val rule = repository.rule("DummyRule")!!
        Assertions.assertThat(rule.name()).isEqualTo("Dummy Rule")
        Assertions.assertThat(rule.htmlDescription()).isEqualTo("Dummy Description")

        Assertions.assertThat(
            context.repository(DummyKotlinPluginExtensionsProvider.DUMMY_NON_SONAR_WAY_REPOSITORY_KEY)!!
                .rule("DummyRule")
        ).isNotNull
    }

}
