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
package org.sonarsource.kotlin.externalreport.detekt

import com.google.gson.GsonBuilder
import io.gitlab.arturbosch.detekt.api.MultiRule
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.RuleSetProvider
import io.gitlab.arturbosch.detekt.api.Severity
import io.gitlab.arturbosch.detekt.cli.ClasspathResourceConverter
import io.gitlab.arturbosch.detekt.core.config.YamlConfig.Companion.loadResource
import org.apache.commons.text.StringEscapeUtils
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.EnumMap
import java.util.ServiceLoader
import kotlin.io.path.exists

internal val DEFAULT_RULES_FILE = Paths.get("sonar-kotlin-plugin", "src", "main", "resources",
    "org", "sonar", "l10n", "kotlin", "rules", "detekt", "rules.json")

fun main(vararg args: String) {
    val rulesFile =
        if (args.isNotEmpty() && !args[0].isNullOrBlank()) Path.of(args[0])
        else DEFAULT_RULES_FILE

    val rules = DetektRuleDefinitionGenerator.generateRuleDefinitionJson()
    var projectPath = Paths.get(".").toRealPath()
    while (!projectPath.resolve(rulesFile).exists()) {
        projectPath = projectPath.parent
    }
    Files.write(projectPath.resolve(rulesFile), rules.toByteArray(StandardCharsets.UTF_8))
}

internal object DetektRuleDefinitionGenerator {
    private const val BASE_URL = "https://arturbosch.github.io/detekt/"
    private const val BASE_PKG = "io.gitlab.arturbosch.detekt.rules."
    private const val STYLE = "style"
    private val PACKAGE_TO_URL = mapOf(
        "${BASE_PKG}documentation" to "${BASE_URL}comments",
        "${BASE_PKG}complexity" to "${BASE_URL}complexity",
        "${BASE_PKG}empty" to "${BASE_URL}empty",
        "${BASE_PKG}exceptions" to "${BASE_URL}exceptions",
        "${BASE_PKG}naming" to "${BASE_URL}naming",
        "${BASE_PKG}performance" to "${BASE_URL}performance",
        "${BASE_PKG}bugs" to "${BASE_URL}potential-bugs",
        "$BASE_PKG$STYLE" to "$BASE_URL$STYLE",
        "${BASE_PKG}style.optional" to "$BASE_URL$STYLE",
    )

    private val SEVERITY_TRANSLATIONS_MAP = buildSeverityTranslationsMap()
    private fun buildSeverityTranslationsMap() = EnumMap<Severity, String>(Severity::class.java)
        .apply {
            this[Severity.CodeSmell] = "MAJOR"
            this[Severity.Defect] = "CRITICAL"
            this[Severity.Maintainability] = "MAJOR"
            this[Severity.Minor] = "MINOR"
            this[Severity.Security] = "BLOCKER"
            this[Severity.Style] = "INFO"
            this[Severity.Warning] = "INFO"
            this[Severity.Performance] = "CRITICAL"
        }

    fun generateRuleDefinitionJson(): String {
        val rules: MutableList<Rule> = ArrayList()
        val configUrl = ClasspathResourceConverter().convert("default-detekt-config.yml")
        for (provider in ServiceLoader.load(RuleSetProvider::class.java)) {
            val config = loadResource(configUrl)
            rules.addAll(provider.instance(config).rules.asSequence()
                .flatMap { rule -> if (rule is MultiRule) rule.rules.asSequence() else sequenceOf(rule) }
                .filterIsInstance<Rule>()
            )
        }
        val externalRules = rules.map { rule ->
            ExternalRule(
                key = rule.ruleId,
                name = pascalCaseToTitle(rule.ruleId),
                type = "CODE_SMELL",
                severity = SEVERITY_TRANSLATIONS_MAP.getOrDefault(rule.issue.severity, "MINOR"),
                description = StringEscapeUtils.escapeHtml4(rule.issue.description),
                url = ruleDocumentation(rule),
                tags = setOf(rule.issue.severity.name.lowercase()),
                constantDebtMinutes = rule.issue.debt.toString().replace("min", "").toLong(),
            )
        }.sortedBy { it.key }

        val gson = GsonBuilder().setPrettyPrinting().create()
        return gson.toJson(externalRules)
    }

    private fun pascalCaseToTitle(id: String): String {
        return id.replace("([^A-Z])([A-Z])".toRegex(), "$1 $2")
    }

    private fun ruleDocumentation(rule: Rule): String? {
        val packageName = rule.javaClass.getPackage().name
        val url = PACKAGE_TO_URL[packageName]
        return if (url != null) {
            "$url.html#${rule.ruleId.lowercase()}"
        } else null
    }

    private data class ExternalRule(
        var key: String,
        var name: String,
        var description: String?,
        var url: String?,
        var tags: Set<String>,
        var type: String,
        var severity: String? = null,
        var constantDebtMinutes: Long,
    )
}
