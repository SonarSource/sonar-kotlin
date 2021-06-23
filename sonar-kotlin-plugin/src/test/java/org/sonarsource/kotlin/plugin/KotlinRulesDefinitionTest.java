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
package org.sonarsource.kotlin.plugin;

import org.junit.jupiter.api.Test;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.Version;

import static org.assertj.core.api.Assertions.assertThat;

class KotlinRulesDefinitionTest {

  @Test
  void rules() {
    RulesDefinition rulesDefinition = new KotlinRulesDefinition();
    RulesDefinition.Context context = new RulesDefinition.Context();
    rulesDefinition.define(context);

    RulesDefinition.Repository repository = context.repository("kotlin");
    assertThat(repository.name()).isEqualTo("SonarAnalyzer");
    assertThat(repository.language()).isEqualTo("kotlin");

    RulesDefinition.Rule rule = repository.rule("S1764");
    assertThat(rule).isNotNull();
    assertThat(rule.name()).isEqualTo("Identical expressions should not be used on both sides of a binary operator");
    assertThat(rule.type()).isEqualTo(RuleType.BUG);
    assertThat(rule.htmlDescription()).startsWith("<p>Using the same value on either side");

    RulesDefinition.Rule ruleWithConfig = repository.rule("S100");
    RulesDefinition.Param param = ruleWithConfig.param("format");
    assertThat(param.defaultValue()).isEqualTo("^[a-zA-Z][a-zA-Z0-9]*$");
  }

}
