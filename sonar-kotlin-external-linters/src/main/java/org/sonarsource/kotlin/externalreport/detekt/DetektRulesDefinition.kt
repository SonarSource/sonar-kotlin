/*
 * SonarSource Kotlin
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonarsource.kotlin.externalreport.detekt

import org.sonar.api.server.rule.RulesDefinition
import org.sonarsource.analyzer.commons.ExternalRuleLoader
import org.sonarsource.kotlin.api.common.RULE_REPOSITORY_LANGUAGE

class DetektRulesDefinition : RulesDefinition {

    companion object {
        const val RULES_FILE = "org/sonar/l10n/kotlin/rules/detekt/rules.json"

        val RULE_LOADER = ExternalRuleLoader(
            DetektSensor.LINTER_KEY,
            DetektSensor.LINTER_NAME,
            RULES_FILE,
            RULE_REPOSITORY_LANGUAGE,
        )
    }

    override fun define(context: RulesDefinition.Context) {
        RULE_LOADER.createExternalRuleRepository(context)
    }
}
