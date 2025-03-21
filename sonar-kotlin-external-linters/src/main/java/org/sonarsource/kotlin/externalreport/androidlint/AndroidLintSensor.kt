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
package org.sonarsource.kotlin.externalreport.androidlint

import org.slf4j.LoggerFactory
import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.batch.sensor.SensorDescriptor
import org.sonar.api.config.Configuration
import org.sonar.api.notifications.AnalysisWarnings
import org.sonarsource.kotlin.api.common.RULE_REPOSITORY_LANGUAGE
import org.sonarsource.kotlin.api.frontend.AbstractPropertyHandlerSensor
import java.io.File
import java.io.FileInputStream

private val LOG = LoggerFactory.getLogger(AndroidLintSensor::class.java)
const val NO_ISSUES_ERROR_MESSAGE = "No issues information will be saved as the report file '{}' can't be read."

class AndroidLintSensor(analysisWarnings: AnalysisWarnings) : AbstractPropertyHandlerSensor(
    analysisWarnings,
    LINTER_KEY,
    LINTER_NAME,
    REPORT_PROPERTY_KEY,
    RULE_REPOSITORY_LANGUAGE,
) {
    companion object {
        const val LINTER_KEY = "android-lint"
        const val LINTER_NAME = "Android Lint"
        const val REPORT_PROPERTY_KEY = "sonar.androidLint.reportPaths"
    }

    override fun describe(descriptor: SensorDescriptor) {
        descriptor // potentially covers multiple languages, not only kotlin
            .onlyWhenConfiguration { conf: Configuration -> conf.hasKey(REPORT_PROPERTY_KEY) }
            .name("Import of $LINTER_NAME issues")
    }

    override fun reportConsumer(context: SensorContext) = { file: File -> importReport(file, context) }
}

private fun importReport(reportPath: File, context: SensorContext) {
    try {
        FileInputStream(reportPath).use {
            AndroidLintXmlReportReader.read(it) { id, file, line, message -> saveIssue(context, id, file, line, message) }
        }
    } catch (e: Exception) {
        LOG.error(NO_ISSUES_ERROR_MESSAGE, reportPath, e)
    }
}

private fun saveIssue(context: SensorContext, id: String, file: String, line: String, message: String) {
    if (id.isEmpty() || message.isEmpty() || file.isEmpty() || !isTextFile(file)) {
        LOG.debug(
            "Missing information or unsupported file type for id:'{}', file:'{}', message:'{}'",
            id,
            file,
            message,
        )
        return
    }
    val predicates = context.fileSystem().predicates()
    val inputFile = context.fileSystem().inputFile(predicates.or(
        predicates.hasAbsolutePath(file),
        predicates.hasRelativePath(file)))
    if (inputFile == null) {
        LOG.warn("No input file found for {}. No android lint issues will be imported on this file.", file)
        return
    }
    val externalRuleLoader = RULE_LOADER


    val newExternalIssue = context.newExternalIssue()
    newExternalIssue
        .type(externalRuleLoader.ruleType(id))
        .severity(externalRuleLoader.ruleSeverity(id))
        .remediationEffortMinutes(externalRuleLoader.ruleConstantDebtMinutes(id))
    val primaryLocation = newExternalIssue.newLocation()
        .message(message)
        .on(inputFile)
    if (line.isNotEmpty()) {
        primaryLocation.at(inputFile.selectLine(line.toInt()))
    }
    newExternalIssue
        .at(primaryLocation)
        .engineId(AndroidLintSensor.LINTER_KEY)
        .ruleId(id)
        .save()
}
