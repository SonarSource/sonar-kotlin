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
package org.sonarsource.kotlin.externalreport.ktlint

import com.google.gson.GsonBuilder
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.ruleset.standard.StandardRuleSetProvider
import org.sonarsource.kotlin.externalreport.ExternalRule
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

internal val DEFAULT_RULES_FILE = Paths.get("sonar-kotlin-external-linters", "src", "main", "resources")
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
    val standardRules = StandardRuleSetProvider().getRuleProviders().map {
        ktlintToExternalRule(it.createNewRuleInstance())
    }.also {
        println("Importing ${it.size} standard rules")
    }
    val experimentalRules = StandardRuleSetProvider().getRuleProviders().map {
        ktlintToExternalRule(it.createNewRuleInstance())
    }.also {
        println("Importing ${it.size} experimental rules")
    }

    return (standardRules + experimentalRules)
        .let { ktlintRules ->
            GsonBuilder().setPrettyPrinting().create().toJson(ktlintRules)
        }
}

fun ktlintToExternalRule(ktLintRule: Rule) =
    ExternalRule(
        key = ktLintRule.ruleId.value,
        name = generateRuleName(ktLintRule),
        description = null,
        url = KTLINT_RULES_WEBPAGE,
        tags = TAGS,
        type = DEFAULT_RULE_TYPE,
        severity = DEFAULT_RULE_SEVERITY,
        constantDebtMinutes = DEFAULT_DEBT,
    )

private fun generateRuleName(rule: Rule): String =
    rule.javaClass.simpleName.removeSuffix("Rule").replace("([^A-Z])([A-Z])".toRegex(), "$1 $2")
