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
package org.sonarsource.slang.externalreport;

import javax.annotation.Nullable;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.rules.RuleType;
import org.sonarsource.analyzer.commons.ExternalRuleLoader;

/**
 * Import external linter reports having "Checkstyle" xml format into SonarQube.
 * Use an "ExternalRuleLoader" to define type/severity/effort of each issue.
 */
public class CheckstyleFormatImporterWithRuleLoader extends CheckstyleFormatImporter {

  private final ExternalRuleLoader externalRuleLoader;

  /**
   * @param context, the context where issues will be sent
   * @param linterKey, used to specify the rule repository
   * @param externalRuleLoader, used to define type/severity/effort of issues
   */
  public CheckstyleFormatImporterWithRuleLoader(SensorContext context, String linterKey, ExternalRuleLoader externalRuleLoader) {
    super(context, linterKey);
    this.externalRuleLoader = externalRuleLoader;
  }

  @Override
  protected RuleType ruleType(String ruleKey, @Nullable String severity, String source) {
    return externalRuleLoader.ruleType(ruleKey);
  }

  @Override
  protected Severity severity(String ruleKey, @Nullable String severity) {
    return externalRuleLoader.ruleSeverity(ruleKey);
  }

  @Override
  protected Long effort(String ruleKey) {
    return externalRuleLoader.ruleConstantDebtMinutes(ruleKey);
  }

}
