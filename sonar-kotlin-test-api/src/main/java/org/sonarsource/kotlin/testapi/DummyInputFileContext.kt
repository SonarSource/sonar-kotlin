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
package org.sonarsource.kotlin.testapi

import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.fs.TextPointer
import org.sonar.api.batch.fs.TextRange
import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.rule.RuleKey
import org.sonarsource.kotlin.api.checks.InputFileContext
import org.sonarsource.kotlin.api.reporting.Message
import org.sonarsource.kotlin.api.reporting.SecondaryLocation

// TODO: make sure it is not used anywhere and then remove
class DummyInputFileContext : InputFileContext {
    override var filteredRules: Map<String, Set<TextRange>> = emptyMap()
    override val inputFile: InputFile = DummyInputFile()
    override val sensorContext: SensorContext
        get() = throw NotImplementedError()
    override val isAndroid: Boolean = false

    val issuesReported = mutableListOf<ReportedIssue>()

    override fun reportIssue(
        ruleKey: RuleKey,
        textRange: TextRange?,
        message: Message,
        secondaryLocations: List<SecondaryLocation>,
        gap: Double?
    ) {
        issuesReported.add(ReportedIssue(ruleKey, textRange, message, secondaryLocations, gap))
    }

    override fun reportAnalysisParseError(repositoryKey: String, inputFile: InputFile, location: TextPointer?) {
        throw NotImplementedError()
    }

    override fun reportAnalysisError(message: String?, location: TextPointer?) {
        throw NotImplementedError()
    }

}

data class ReportedIssue(
    val ruleKey: RuleKey,
    val textRange: TextRange?,
    val message: Message,
    val secondaryLocations: List<SecondaryLocation>,
    val gap: Double?
)
