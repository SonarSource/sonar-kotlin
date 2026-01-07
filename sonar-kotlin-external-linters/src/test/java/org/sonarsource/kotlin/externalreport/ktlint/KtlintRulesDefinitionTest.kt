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
package org.sonarsource.kotlin.externalreport.ktlint

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.sonar.api.rules.RuleType
import org.sonar.api.server.rule.RulesDefinition

class KtlintRulesDefinitionTest {

    @Test
    fun `ktlint external repository`() {
        val context = RulesDefinition.Context()
        val rulesDefinition = KtlintRulesDefinition()
        rulesDefinition.define(context)
        assertThat(context.repositories()).hasSize(1)
        val repository = context.repository("external_ktlint")!!
        assertThat(repository.name()).isEqualTo("ktlint")
        assertThat(repository.language()).isEqualTo("kotlin")
        assertThat(repository.isExternal).isTrue
        assertThat(repository.rules().size).isEqualTo(86)
        val modifierOrder = repository.rule("standard:modifier-order")!!
        assertThat(modifierOrder).isNotNull
        assertThat(modifierOrder.name()).isEqualTo("Modifier Order")
        assertThat(modifierOrder.type()).isEqualTo(RuleType.CODE_SMELL)
        assertThat(modifierOrder.severity()).isEqualTo("MAJOR")
        assertThat(modifierOrder.htmlDescription())
            .isEqualTo("""See description of ktlint rule <code>standard:modifier-order</code> at the <a href="https://ktlint.github.io/#rules">ktlint website</a>.""")
        assertThat(modifierOrder.tags()).containsExactlyInAnyOrder("ktlint", "style")
        assertThat(modifierOrder.debtRemediationFunction()?.baseEffort()).isEqualTo("0min")
    }
}
