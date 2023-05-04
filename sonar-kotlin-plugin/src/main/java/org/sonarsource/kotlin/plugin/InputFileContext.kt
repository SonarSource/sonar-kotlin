/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2023 SonarSource SA
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
package org.sonarsource.kotlin.plugin

import org.slf4j.LoggerFactory
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.fs.TextPointer
import org.sonar.api.batch.fs.TextRange
import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.batch.sensor.issue.MessageFormatting
import org.sonar.api.batch.sensor.issue.NewIssueLocation
import org.sonar.api.rule.RuleKey
import org.sonarsource.kotlin.api.InputFileContext
import org.sonarsource.kotlin.api.Message
import org.sonarsource.kotlin.api.SecondaryLocation
import org.sonarsource.kotlin.converter.KotlinTextRanges.contains

private const val PARSING_ERROR_RULE_KEY = "ParsingError"

private val LOG = LoggerFactory.getLogger(InputFileContextImpl::class.java)

// To avoid many duplicate log messages when reporting issues with message code highlighting, we statically remember when we've already
// logged this message and can refrain from logging it again. Mutable static state is not a great pattern in general. Here, however, it is
// acceptable and will eventually be removed when we remove the entire API version guard.
private var alreadyLoggedIssueMessageWithHighlightingFailed = false

class InputFileContextImpl(
    override val sensorContext: SensorContext,
    override val inputFile: InputFile,
    override val isAndroid: Boolean,
) : InputFileContext {

    override var filteredRules: Map<String, Set<TextRange>> = HashMap()

    override fun reportIssue(
        ruleKey: RuleKey,
        textRange: TextRange?,
        message: Message,
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

    private fun NewIssueLocation.message(message: Message): NewIssueLocation {
        // We catch exceptions here in case the product we are running in does not support this relatively new API endpoint. This is
        // especially relevant for old SL versions running in connected mode, which can download a newer analyzers from the server.
        // Eventually, when we don't support these old product versions anymore, we can remove this guard.
        runCatching {
            val formatting = message.ranges.map { (start, end) ->
                newMessageFormatting().apply {
                    start(start)
                    end(end)
                    type(MessageFormatting.Type.CODE)
                }
            }

            message(message.text, formatting)
        }.onFailure { e ->
            if (!alreadyLoggedIssueMessageWithHighlightingFailed) {
                alreadyLoggedIssueMessageWithHighlightingFailed = true
                LOG.warn(
                    "Could not report issue with code highlighting, using plain text instead. Check whether the product is outdated.",
                    e
                )
            }
            message(message.text)
        }

        return this
    }

    override fun reportAnalysisParseError(repositoryKey: String, inputFile: InputFile, location: TextPointer?) {
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
