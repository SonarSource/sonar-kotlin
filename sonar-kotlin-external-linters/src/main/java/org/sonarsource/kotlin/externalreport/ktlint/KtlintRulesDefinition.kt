/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2026 SonarSource SA
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

import org.sonar.api.server.rule.RulesDefinition
import org.sonarsource.analyzer.commons.ExternalRuleLoader
import org.sonarsource.kotlin.api.common.RULE_REPOSITORY_LANGUAGE

class KtlintRulesDefinition : RulesDefinition {
    companion object {
        const val RULES_FILE = "org/sonar/l10n/kotlin/rules/ktlint/rules.json"

        val RULE_LOADER = ExternalRuleLoader(
            KtlintSensor.LINTER_KEY,
            KtlintSensor.LINTER_NAME,
            RULES_FILE,
            RULE_REPOSITORY_LANGUAGE,
        )

        internal const val EXPERIMENTAL_RULE_PREFIX = "experimental:"
    }

    override fun define(context: RulesDefinition.Context) {
        RULE_LOADER.createExternalRuleRepository(context)
    }
}
