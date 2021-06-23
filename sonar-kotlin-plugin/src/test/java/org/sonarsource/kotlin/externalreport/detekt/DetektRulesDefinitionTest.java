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
package org.sonarsource.kotlin.externalreport.detekt;

import org.junit.jupiter.api.Test;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;

import static org.assertj.core.api.Assertions.assertThat;

class DetektRulesDefinitionTest {

  @Test
  void detekt_external_repository() {
    RulesDefinition.Context context = new RulesDefinition.Context();
    DetektRulesDefinition rulesDefinition = new DetektRulesDefinition();
    rulesDefinition.define(context);

    assertThat(context.repositories()).hasSize(1);
    RulesDefinition.Repository repository = context.repository("external_detekt");
    assertThat(repository.name()).isEqualTo("detekt");
    assertThat(repository.language()).isEqualTo("kotlin");
    assertThat(repository.isExternal()).isTrue();

    assertThat(repository.rules().size()).isEqualTo(111);

    RulesDefinition.Rule classNaming = repository.rule("ClassNaming");
    assertThat(classNaming).isNotNull();
    assertThat(classNaming.name()).isEqualTo("Class Naming");
    assertThat(classNaming.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(classNaming.severity()).isEqualTo("INFO");
    assertThat(classNaming.htmlDescription()).isEqualTo("<p>A classes name should fit the naming pattern defined in the projects configuration.</p> " +
      "<p>See more at the <a href=\"https://arturbosch.github.io/detekt/naming.html#classnaming\">detekt website</a>.</p>");
    assertThat(classNaming.tags()).containsExactlyInAnyOrder("style");
    assertThat(classNaming.debtRemediationFunction().baseEffort()).isEqualTo("5min");
  }

}
