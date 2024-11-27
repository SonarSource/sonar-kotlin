/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2024 SonarSource SA
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
package org.sonarsource.kotlin.externalreport.ktlint

import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.rule.RuleKey
import org.sonarsource.kotlin.externalreport.ktlint.KtlintRulesDefinition.Companion.EXPERIMENTAL_RULE_PREFIX
import org.sonarsource.kotlin.externalreport.common.CheckstyleFormatImporterWithRuleLoader

internal class CheckstyleReportParser(context: SensorContext) : CheckstyleFormatImporterWithRuleLoader(
    context,
    KtlintSensor.LINTER_KEY,
    KtlintRulesDefinition.RULE_LOADER,
) {
    override fun createRuleKey(source: String): RuleKey? {
        val ruleKey =
            if (source.startsWith(EXPERIMENTAL_RULE_PREFIX)) source.substring(EXPERIMENTAL_RULE_PREFIX.length)
            else source

        return super.createRuleKey(ruleKey)
    }
}
