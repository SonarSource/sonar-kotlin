/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2025 SonarSource SA
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
package org.sonarsource.kotlin.externalreport.common;

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
