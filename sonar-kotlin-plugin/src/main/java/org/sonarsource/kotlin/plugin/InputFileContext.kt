package org.sonarsource.kotlin.plugin

import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.fs.TextPointer
import org.sonar.api.batch.fs.TextRange
import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.rule.RuleKey
import org.sonarsource.kotlin.api.InputFileContext
import org.sonarsource.kotlin.api.SecondaryLocation
import org.sonarsource.kotlin.converter.KotlinTextRanges.contains

private const val PARSING_ERROR_RULE_KEY = "ParsingError"

class InputFileContextImpl(
    override val sensorContext: SensorContext,
    override val inputFile: InputFile,
) : InputFileContext {

    override var filteredRules: Map<String, Set<TextRange>> = HashMap()

    override fun reportIssue(
        ruleKey: RuleKey,
        textRange: TextRange?,
        message: String,
        secondaryLocations: List<SecondaryLocation>,
        gap: Double?,
    ) {
        if (textRange != null && (filteredRules[ruleKey.toString()] ?: emptySet()).any { textRange in it }) {
            // Issue is filtered by one of the filter.
            return
        }

        with(sensorContext.newIssue()) {
            forRule(ruleKey)

            at(newLocation()
                .on(inputFile)
                .message(message).let {
                    if (textRange != null) it.at(textRange) else it
                }
            )

            gap(gap)

            for (secondary in secondaryLocations) {
                addLocation(newLocation()
                    .on(inputFile)
                    .at(secondary.textRange)
                    .message(secondary.message ?: "")
                )
            }

            save()
        }
    }

    override fun reportAnalysisParseError(repositoryKey: String?, inputFile: InputFile, location: TextPointer?) {
        reportAnalysisError("Unable to parse file: $inputFile", location)

        val parsingErrorRuleKey = RuleKey.of(repositoryKey, PARSING_ERROR_RULE_KEY)

        if (sensorContext.activeRules().find(parsingErrorRuleKey) == null) {
            return
        }

        with(sensorContext.newIssue()) {
            forRule(parsingErrorRuleKey)

            at(
                newLocation()
                    .on(inputFile)
                    .message("A parsing error occurred in this file.")
                    .let { if (location != null) it.at(inputFile.selectLine(location.line())) else it }
            )

            save()
        }
    }

    override fun reportAnalysisError(message: String?, location: TextPointer?) {
        with(sensorContext.newAnalysisError()) {
            if (message != null) message(message)
            onFile(inputFile)
            if (location != null) at(inputFile.newPointer(location.line(), location.lineOffset()))

            save()
        }
    }
}
