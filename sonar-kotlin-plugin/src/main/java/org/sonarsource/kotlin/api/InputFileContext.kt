package org.sonarsource.kotlin.api

import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.fs.TextPointer
import org.sonar.api.batch.fs.TextRange
import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.rule.RuleKey

interface InputFileContext {
    var filteredRules: Map<String, Set<TextRange>>
    val inputFile: InputFile
    val sensorContext: SensorContext

    fun reportIssue(
        ruleKey: RuleKey,
        textRange: TextRange?,
        message: String,
        secondaryLocations: List<SecondaryLocation>,
        gap: Double?,
    )

    fun reportAnalysisParseError(repositoryKey: String?, inputFile: InputFile, location: TextPointer?)

    fun reportAnalysisError(message: String?, location: TextPointer?)
}
