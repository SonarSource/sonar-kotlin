/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2026 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1.0.1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonarsource.kotlin.externalreport.androidlint

import org.sonar.api.server.rule.RulesDefinition
import org.sonarsource.analyzer.commons.ExternalRuleLoader
import org.sonarsource.kotlin.api.common.RULE_REPOSITORY_LANGUAGE

const val RULES_FILE = "org/sonar/l10n/android/rules/androidlint/rules.json"

/**
 * Android lint scopes could be: ".xml", ".java", ".kt", ".kts", ".properties", ".gradle", "proguard.cfg", "proguard-project.txt", ".png", ".class"
 * ( https://android.googlesource.com/platform/tools/base/+/studio-master-dev/lint/libs/lint-api/src/main/java/com/android/tools/lint/detector/api/Scope.kt )
 * This sensor won't report any issue on the given file if it wasn't located in our file system
 */
private val TEXT_FILE_EXTENSIONS = listOf(".xml", ".java", ".kt", ".kts", ".properties", ".gradle", ".cfg", ".txt")

val RULE_LOADER = ExternalRuleLoader(
    AndroidLintSensor.LINTER_KEY,
    AndroidLintSensor.LINTER_NAME,
    RULES_FILE,
    RULE_REPOSITORY_LANGUAGE,
)

fun isTextFile(file: String) = TEXT_FILE_EXTENSIONS.stream().anyMatch { file.endsWith(it) }

class AndroidLintRulesDefinition : RulesDefinition {
    override fun define(context: RulesDefinition.Context) {
        RULE_LOADER.createExternalRuleRepository(context)
    }
}
