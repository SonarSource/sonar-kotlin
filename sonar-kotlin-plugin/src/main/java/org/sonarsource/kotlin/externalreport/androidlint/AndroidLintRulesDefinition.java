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
package org.sonarsource.kotlin.externalreport.androidlint;

import java.util.Arrays;
import java.util.List;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonarsource.analyzer.commons.ExternalRuleLoader;
import org.sonarsource.kotlin.plugin.KotlinPlugin;

import static org.sonarsource.kotlin.externalreport.androidlint.AndroidLintSensor.LINTER_KEY;
import static org.sonarsource.kotlin.externalreport.androidlint.AndroidLintSensor.LINTER_NAME;

public class AndroidLintRulesDefinition implements RulesDefinition {

  private static final String RULES_JSON = "org/sonar/l10n/android/rules/androidlint/rules.json";

  /**
   * Android lint scopes could be: ".xml", ".java", ".kt", ".kts", ".properties", ".gradle", "proguard.cfg", "proguard-project.txt", ".png", ".class"
   * ( https://android.googlesource.com/platform/tools/base/+/studio-master-dev/lint/libs/lint-api/src/main/java/com/android/tools/lint/detector/api/Scope.kt )
   * But this sensor provides rule descriptions only for ".xml", ".java", ".kt"
   */
  private static final String RULE_REPOSITORY_LANGUAGE = KotlinPlugin.KOTLIN_LANGUAGE_KEY;

  private static final List<String> TEXT_FILE_EXTENSIONS = Arrays.asList(".xml", ".java", ".kt", ".kts", ".properties", ".gradle", ".cfg", ".txt");

  static final ExternalRuleLoader RULE_LOADER = new ExternalRuleLoader(LINTER_KEY, LINTER_NAME, RULES_JSON, RULE_REPOSITORY_LANGUAGE);

  @Override
  public void define(Context context) {
    RULE_LOADER.createExternalRuleRepository(context);
  }

  static boolean isTextFile(String file) {
    return TEXT_FILE_EXTENSIONS.stream().anyMatch(file::endsWith);
  }
}
