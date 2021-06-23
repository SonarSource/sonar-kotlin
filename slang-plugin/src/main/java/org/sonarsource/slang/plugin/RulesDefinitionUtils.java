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

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.check.RuleProperty;
import org.sonarsource.slang.checks.utils.Language;
import org.sonarsource.slang.checks.utils.PropertyDefaultValue;
import org.sonarsource.slang.checks.utils.PropertyDefaultValues;

public class RulesDefinitionUtils {

  private RulesDefinitionUtils() {
  }

  public static void setDefaultValuesForParameters(RulesDefinition.NewRepository repository, List<Class<?>> checks, Language language) {
    for (Class<?> check : checks) {
      org.sonar.check.Rule ruleAnnotation = AnnotationUtils.getAnnotation(check, org.sonar.check.Rule.class);
      String ruleKey = ruleAnnotation.key();
      for (Field field : check.getDeclaredFields()) {
        RuleProperty ruleProperty = field.getAnnotation(RuleProperty.class);
        PropertyDefaultValues defaultValues = field.getAnnotation(PropertyDefaultValues.class);
        if (ruleProperty == null || defaultValues == null) {
          continue;
        }
        String paramKey = ruleProperty.key();

        List<PropertyDefaultValue> valueForLanguage = Arrays.stream(defaultValues.value())
          .filter(defaultValue -> defaultValue.language() == language)
          .collect(Collectors.toList());
        if (valueForLanguage.size() != 1) {
          throw new IllegalStateException("Invalid @PropertyDefaultValue on " + check.getSimpleName() +
            " for language " + language);
        }
        valueForLanguage
          .forEach(defaultValue -> repository.rule(ruleKey).param(paramKey).setDefaultValue(defaultValue.defaultValue()));
      }
    }
  }
}
