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

import com.google.gson.GsonBuilder
import com.pinterest.ktlint.ruleset.experimental.ExperimentalRuleSetProvider
import com.pinterest.ktlint.ruleset.standard.StandardRuleSetProvider
import org.sonarsource.kotlin.externalreport.ExternalReporting
import org.sonarsource.kotlin.externalreport.ExternalRule
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import com.pinterest.ktlint.core.Rule as KtlintRule

internal val DEFAULT_RULES_FILE = Paths.get("sonar-kotlin-plugin", "src", "main", "resources")
    .resolve(Path.of(KtlintRulesDefinition.RULES_FILE))

private val TAGS = setOf("ktlint", "style")
private const val KTLINT_RULES_WEBPAGE = "https://ktlint.github.io/#rules"
private const val DEFAULT_RULE_TYPE = "CODE_SMELL"
private const val DEFAULT_RULE_SEVERITY = "MAJOR"
private const val DEFAULT_DEBT = 0L

fun main(vararg args: String?) {
    val rulesFile =
        if (args.isNotEmpty() && !args[0].isNullOrBlank()) Path.of(args[0])
        else DEFAULT_RULES_FILE

    val rules = generateRuleDefinitionsJson()
    var projectPath = Paths.get(".").toRealPath()
    while (!projectPath.resolve(rulesFile).toFile().exists()) {
        projectPath = projectPath.parent
    }
    Files.writeString(projectPath.resolve(rulesFile), rules)
}

fun generateRuleDefinitionsJson(): String {
    val standardRules = StandardRuleSetProvider().get().map(::ktlintToExternalRule).also {
        println("Importing ${it.size} standard rules")
    }
    val experimentalRules = ExperimentalRuleSetProvider().get().map(::ktlintToExternalRule).also {
        println("Importing ${it.size} experimental rules")
    }

    val fallbackRule = ExternalRule(
        key = ExternalReporting.FALLBACK_RULE_KEY,
        name = "Ktlint Rule",
        description = "This reporting may be triggered by a custom ktlint rule or by a default ktlint rule that has not yet " +
            "been added to the Sonar Kotlin plugin.",
        url = KTLINT_RULES_WEBPAGE,
        tags = TAGS,
        type = DEFAULT_RULE_TYPE,
        severity = DEFAULT_RULE_SEVERITY,
        constantDebtMinutes = DEFAULT_DEBT,
    )

    return (standardRules + experimentalRules + fallbackRule)
        .let { ktlintRules ->
            GsonBuilder().setPrettyPrinting().create().toJson(ktlintRules)
        }
}

fun ktlintToExternalRule(ktLintRule: KtlintRule) =
    ExternalRule(
        key = ktLintRule.id,
        name = generateRuleName(ktLintRule),
        description = null,
        url = KTLINT_RULES_WEBPAGE,
        tags = TAGS,
        type = DEFAULT_RULE_TYPE,
        severity = DEFAULT_RULE_SEVERITY,
        constantDebtMinutes = DEFAULT_DEBT,
    )

private fun generateRuleName(rule: KtlintRule): String =
    rule.javaClass.simpleName.removeSuffix("Rule").replace("([^A-Z])([A-Z])".toRegex(), "$1 $2")
