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

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.sonar.api.rules.RuleType
import org.sonar.api.server.rule.RulesDefinition

internal class KotlinRulesDefinitionTest {

    @Test
    fun rules() {
        val rulesDefinition: RulesDefinition = KotlinRulesDefinition()
        val context = RulesDefinition.Context()
        rulesDefinition.define(context)
        val repository = context.repository("kotlin")
        Assertions.assertThat(repository!!.name()).isEqualTo("SonarAnalyzer")
        Assertions.assertThat(repository.language()).isEqualTo("kotlin")
        val rule = repository.rule("S1764")
        Assertions.assertThat(rule).isNotNull
        Assertions.assertThat(rule!!.name())
            .isEqualTo("Identical expressions should not be used on both sides of a binary operator")
        Assertions.assertThat(rule.type()).isEqualTo(RuleType.BUG)
        Assertions.assertThat(rule.htmlDescription()).startsWith("<p>Using the same value on either side")
        val ruleWithConfig = repository.rule("S100")
        val param = ruleWithConfig!!.param("format")
        Assertions.assertThat(param!!.defaultValue()).isEqualTo("^[a-zA-Z][a-zA-Z0-9]*$")
    }
}
