/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2026 SonarSource SA
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
package org.sonarsource.kotlin.externalreport.detekt

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.sonar.api.rules.RuleType
import org.sonar.api.server.rule.RulesDefinition

internal class DetektRulesDefinitionTest {
    @Test
    fun detekt_external_repository() {
        val context = RulesDefinition.Context()
        val rulesDefinition = DetektRulesDefinition()
        rulesDefinition.define(context)
        assertThat(context.repositories()).hasSize(1)

        val repository = context.repository("external_detekt")
        assertThat(repository!!.name()).isEqualTo("detekt")
        assertThat(repository.language()).isEqualTo("kotlin")
        assertThat(repository.isExternal).isTrue
        assertThat(repository.rules().size).isEqualTo(214)

        val classNaming = repository.rule("ClassNaming")
        assertThat(classNaming).isNotNull
        assertThat(classNaming!!.name()).isEqualTo("Class Naming")
        assertThat(classNaming.type()).isEqualTo(RuleType.CODE_SMELL)
        assertThat(classNaming.severity()).isEqualTo("INFO")
        assertThat(classNaming.htmlDescription())
            .isEqualTo("<p>A class or object name should fit the naming pattern defined in the projects configuration.</p> " +
                "<p>See more at the <a href=\"https://detekt.github.io/detekt/naming.html#classnaming\">detekt website</a>.</p>")
        assertThat(classNaming.tags()).containsExactlyInAnyOrder("style")
        assertThat(classNaming.debtRemediationFunction()!!.baseEffort()).isEqualTo("5min")
    }
}
