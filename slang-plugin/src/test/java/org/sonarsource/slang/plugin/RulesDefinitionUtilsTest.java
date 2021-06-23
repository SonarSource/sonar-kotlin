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
package org.sonarsource.slang.plugin;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.server.rule.RulesDefinitionAnnotationLoader;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonarsource.slang.checks.utils.Language;
import org.sonarsource.slang.checks.utils.PropertyDefaultValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RulesDefinitionUtilsTest {

  private static final String REPOSITORY = "test";
  private RulesDefinition.NewRepository repository;
  private RulesDefinition.Context context;

  @Test
  void test_setDefaultValuesForParameters_kotlin() {
    initRepository();

    RulesDefinitionUtils.setDefaultValuesForParameters(repository, Collections.singletonList(Check.class), Language.KOTLIN);
    repository.done();

    RulesDefinition.Repository repository = context.repository(REPOSITORY);
    RulesDefinition.Rule check = repository.rule("check");
    RulesDefinition.Param param = check.param("param");
    assertThat(param.defaultValue()).isEqualTo("kotlin");
  }

  @Test
  void test_setDefaultValuesForParameters_ruby() {
    initRepository();

    RulesDefinitionUtils.setDefaultValuesForParameters(repository, Collections.singletonList(Check.class), Language.RUBY);
    repository.done();

    RulesDefinition.Repository repository = context.repository(REPOSITORY);
    RulesDefinition.Rule check = repository.rule("check");
    RulesDefinition.Param param = check.param("param");
    assertThat(param.defaultValue()).isEqualTo("ruby");
  }

  @Test
  void test_setDefaultValuesForParameters_scala() {
    initRepository();

    RulesDefinitionUtils.setDefaultValuesForParameters(repository, Collections.singletonList(Check.class), Language.SCALA);
    repository.done();

    RulesDefinition.Repository repository = context.repository(REPOSITORY);
    RulesDefinition.Rule check = repository.rule("check");
    RulesDefinition.Param param = check.param("param");
    assertThat(param.defaultValue()).isEqualTo("scala");
  }

  @Test
  void wrong_annotation() {
    context = new RulesDefinition.Context();
    repository = context.createRepository(REPOSITORY, Language.KOTLIN.toString());
    new RulesDefinitionAnnotationLoader().load(repository, WrongAnnotationUsage.class);

    assertThatThrownBy( () -> RulesDefinitionUtils.setDefaultValuesForParameters(repository, Collections.singletonList(WrongAnnotationUsage.class), Language.RUBY))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Invalid @PropertyDefaultValue on WrongAnnotationUsage for language RUBY");

    assertThatThrownBy( () -> RulesDefinitionUtils.setDefaultValuesForParameters(repository, Collections.singletonList(WrongAnnotationUsage.class), Language.KOTLIN))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Invalid @PropertyDefaultValue on WrongAnnotationUsage for language KOTLIN");
  }

  private void initRepository() {
    context = new RulesDefinition.Context();
    repository = context.createRepository(REPOSITORY, Language.KOTLIN.toString());
    new RulesDefinitionAnnotationLoader().load(repository, Check.class);
  }

  @Rule(key = "check", name = "Check", description = "Desc")
  static class Check {

    @RuleProperty(key = "param")
    @PropertyDefaultValue(language = Language.KOTLIN, defaultValue = "kotlin")
    @PropertyDefaultValue(language = Language.RUBY, defaultValue = "ruby")
    @PropertyDefaultValue(language = Language.SCALA, defaultValue = "scala")
    String param;

    String notAParamField;

    @RuleProperty(key = "paramNoDefault")
    String paramNoDefault;
  }

  @Rule(key = "invalid", name = "Check", description = "Desc")
  static class WrongAnnotationUsage {

    @RuleProperty(key = "param")
    @PropertyDefaultValue(language = Language.KOTLIN, defaultValue = "kotlin")
    @PropertyDefaultValue(language = Language.KOTLIN, defaultValue = "ruby")
    String param;
  }
}
