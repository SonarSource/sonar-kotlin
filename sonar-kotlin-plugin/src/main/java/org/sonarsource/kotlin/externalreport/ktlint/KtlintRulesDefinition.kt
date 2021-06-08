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
package org.sonarsource.kotlin.externalreport.ktlint

import org.sonar.api.server.rule.RulesDefinition
import org.sonarsource.analyzer.commons.ExternalRuleLoader
import org.sonarsource.kotlin.plugin.KotlinPlugin
import java.nio.file.Path

class KtlintRulesDefinition : RulesDefinition {
    companion object {
        const val RULES_FILE = "org/sonar/l10n/kotlin/rules/ktlint/rules.json"

        val RULE_LOADER = ExternalRuleLoader(
            KtlintSensor.LINTER_KEY,
            KtlintSensor.LINTER_NAME,
            RULES_FILE,
            KotlinPlugin.KOTLIN_LANGUAGE_KEY)

        internal const val EXPERIMENTAL_RULE_PREFIX = "experimental:"
    }

    override fun define(context: RulesDefinition.Context) {
        RULE_LOADER.createExternalRuleRepository(context)
    }
}
