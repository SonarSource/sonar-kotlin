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
package org.sonarsource.kotlin.externalreport.ktlint

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.sonar.api.rules.RuleType
import org.sonar.api.server.rule.RulesDefinition

class KtlintRulesDefinitionTest {

    @Test
    fun detekt_external_repository() {
        val context = RulesDefinition.Context()
        val rulesDefinition = KtlintRulesDefinition()
        rulesDefinition.define(context)
        assertThat(context.repositories()).hasSize(1)
        val repository = context.repository("external_ktlint")!!
        assertThat(repository.name()).isEqualTo("ktlint")
        assertThat(repository.language()).isEqualTo("kotlin")
        assertThat(repository.isExternal).isTrue
        assertThat(repository.rules().size).isEqualTo(41)
        val modifierOrder = repository.rule("modifier-order")!!
        assertThat(modifierOrder).isNotNull
        assertThat(modifierOrder.name()).isEqualTo("Modifier Order")
        assertThat(modifierOrder.type()).isEqualTo(RuleType.CODE_SMELL)
        assertThat(modifierOrder.severity()).isEqualTo("MAJOR")
        assertThat(modifierOrder.htmlDescription())
            .isEqualTo("""See description of ktlint rule <code>modifier-order</code> at the <a href="https://ktlint.github.io/#rules">ktlint website</a>.""")
        assertThat(modifierOrder.tags()).containsExactlyInAnyOrder("ktlint", "style")
        assertThat(modifierOrder.debtRemediationFunction()?.baseEffort()).isEqualTo("0min")
    }
}
