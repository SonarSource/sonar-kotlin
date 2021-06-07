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
package org.sonarsource.kotlin.externalreport.androidlint

import com.google.gson.GsonBuilder
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.apache.commons.text.StringEscapeUtils
import org.sonarsource.kotlin.externalreport.ExternalRule

internal val RULES_FILE = Paths.get("sonar-kotlin-plugin", "src", "main", "resources",
    "org", "sonar", "l10n", "android", "rules", "androidlint", "rules.json")
private val ANDROID_LINT_HELP = Paths.get( "utils-kotlin", "src", "main", "resources", "android-lint-help.txt")

private val NON_SPACES = Regex("\\S+")
private val WORDS = Regex("\\w+")

/**
 * Android lint source code: https://android.googlesource.com/platform/tools/base/+/studio-master-dev/lint
 */
fun main(vararg args: String?) {
    val rulesFile =
        if (args.isNotEmpty() && !args[0].isNullOrBlank()) Path.of(args[0])
        else RULES_FILE

    val androidLintHelpPath =
        if (args.size > 1 && !args[1].isNullOrBlank()) Path.of(args[1])
        else ANDROID_LINT_HELP


    var projectPath = Paths.get(".").toRealPath()
    while (!projectPath.resolve(rulesFile).toFile().exists()) {
        projectPath = projectPath.parent
    }
    val rules = AndroidLintDefinitionGenerator.generateRuleDefinitionJson(projectPath.resolve(androidLintHelpPath))
    Files.write(projectPath.resolve(rulesFile), rules.toByteArray(StandardCharsets.UTF_8))
}

private class AndroidLint {
    object Severity {
        const val INFORMATION = "Information"
        const val WARNING = "Warning"
        const val ERROR = "Error"
        const val FATAL = "Fatal"
    }

    object Category {
        const val ACCESSIBILITY = "Accessibility"
        const val CORRECTNESS = "Correctness"
        const val INTERNATIONALIZATION = "Internationalization"
        const val PERFORMANCE = "Performance"
        const val SECURITY = "Security"
        const val USABILITY = "Usability"
        const val COMPLIANCE = "Compliance"
        const val INTEROPERABILITY = "Interoperability"
        const val PRODUCTIVITY = "Productivity"
        const val TESTING = "Testing"
        const val LINT_IMPL = "Lint Implementation"
    }
}

private object AndroidLintDefinitionGenerator {
    fun generateRuleDefinitionJson(androidLintHelpPath: Path): String {
        val externalRules = ArrayList<ExternalRule>()
        val help = AndroidLintHelp(androidLintHelpPath)
        var externalRule = help.read()
        while (externalRule != null) {
            externalRules.add(externalRule)
            externalRule = help.read()
        }
        externalRules.sortBy { it.key }
        val gson = GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()
        return gson.toJson(externalRules)
    }
}

private class AndroidLintHelp(androidLintHelpPath: Path) {
    private var lines: List<String> = emptyList()
    private var pos: Int

    init {
        lines = try {
            Files.readAllLines(androidLintHelpPath, StandardCharsets.UTF_8)
        } catch (e: IOException) {
            throw IllegalStateException("Can't load android-lint-help.txt", e)
        }
        check(!(lines.isEmpty() || lines.get(0) != "Available issues:")) {
            "Unexpected android-lint-help.txt first line: " + lines.get(0)
        }
        pos = 1
    }

    fun read(): ExternalRule? {
        if (pos >= lines.size) {
            return null
        }
        consumeCategorySections()
        val ruleKey = consumeRuleSection() ?: return null
        val name = consumeHeader("Summary")
        val priority = consumeHeader("Priority")
        val severity = consumeHeader("Severity")
        val category = consumeHeader("Category")
        return ExternalRule(
            key = ruleKey,
            name = name,
            description = consumeDescription(),
            type = mapType(severity, category, priority),
            severity = mapSeverity(severity),
            tags = mapTags(category).toSet() + "android",
            constantDebtMinutes = 5L,
            url = null,
        )
    }

    private fun consumeCategorySections() {
        while (isSection("=+")) {
            pos += 3
        }
    }

    private fun consumeRuleSection(): String? {
        if (isSection("-+")) {
            val key = lines[pos + 1]
            pos += 3
            return key
        }
        return null
    }

    private fun consumeHeader(name: String): String {
        check(!(pos >= lines.size || !lines[pos].startsWith("$name: "))) {
            "Unexpected line at " + (pos + 1) +
                " instead of '" + name + ":' header: " + lines[pos]
        }
        var header = lines[pos].substring(lines[pos].indexOf(':') + 2)
        pos++
        if (name == "Summary" && pos < lines.size && !lines[pos].isEmpty()) {
            header += " " + lines[pos]
            pos++
        }
        while (pos < lines.size && lines[pos].isEmpty()) {
            pos++
        }
        return header
    }

    private fun consumeDescription(): String {
        val html = StringBuilder()
        html.append("<p>\n")
        var lastLineWasMoreInformation = false
        while (pos < lines.size && !isSection("=+|-+")) {
            val line = StringEscapeUtils.escapeHtml4(lines[pos])
            if (line.isEmpty()) {
                html.append(line).append("</p>\n<p>\n")
            } else if (lastLineWasMoreInformation && line.startsWith("http") && line.matches(NON_SPACES)) {
                html.append("<a href=\"").append(line).append("\">").append(line).append("</a><br />\n")
            } else if (line.endsWith(":") || line.endsWith(": ")) {
                html.append(line).append("<br />\n")
            } else {
                html.append(line).append("\n")
            }
            lastLineWasMoreInformation = line == "More information: "
            pos++
        }
        html.append("</p>\n")
        return html.toString()
    }

    private fun isSection(underlineRegex: String): Boolean {
        return pos + 2 < lines.size &&
            lines[pos].isEmpty() &&
            lines[pos + 1].matches(WORDS) &&
            lines[pos + 2].matches(Regex(underlineRegex)) && lines[pos + 1].length == lines[pos + 2].length
    }
}

private fun mapSeverity(severity: String): String {
    return when (severity) {
        AndroidLint.Severity.INFORMATION -> "INFO"
        AndroidLint.Severity.WARNING -> "MINOR"
        AndroidLint.Severity.ERROR -> "MAJOR"
        AndroidLint.Severity.FATAL -> "CRITICAL"
        else -> throw IllegalStateException("Unexpected severity: $severity")
    }
}

private fun mapType(severity: String, category: String, priority: String): String {
    val priorityValue = priority.substring(0, priority.indexOf(' ')).toInt()
    if (priorityValue >= 7 && category == AndroidLint.Category.SECURITY && severity == AndroidLint.Severity.FATAL) {
        return "VULNERABILITY"
    } else if (priorityValue >= 5 &&
        (category == AndroidLint.Category.SECURITY || category.startsWith(AndroidLint.Category.CORRECTNESS)) &&
        (severity == AndroidLint.Severity.ERROR || severity == AndroidLint.Severity.FATAL)
    ) {
        return "BUG"
    }
    return "CODE_SMELL"
}

private fun mapTags(category: String): List<String> {
    return when {
        category == AndroidLint.Category.ACCESSIBILITY -> listOf("accessibility")
        category.startsWith(AndroidLint.Category.CORRECTNESS) -> emptyList()
        category.startsWith(AndroidLint.Category.INTERNATIONALIZATION) -> listOf("i18n")
        category.startsWith(AndroidLint.Category.PERFORMANCE) -> listOf("performance")
        category == AndroidLint.Category.SECURITY -> listOf("security")
        category.startsWith(AndroidLint.Category.USABILITY) -> listOf("user-experience")
        category.startsWith(AndroidLint.Category.COMPLIANCE) -> listOf("compliance")
        category.startsWith(AndroidLint.Category.INTEROPERABILITY) -> listOf("interoperability")
        category.startsWith(AndroidLint.Category.PRODUCTIVITY) -> listOf("productivity")
        category.startsWith(AndroidLint.Category.TESTING) -> listOf("testing")
        category.startsWith(AndroidLint.Category.LINT_IMPL) -> listOf("lint-implementation")
        else -> throw IllegalStateException("Unexpected category: $category")
    }
}
