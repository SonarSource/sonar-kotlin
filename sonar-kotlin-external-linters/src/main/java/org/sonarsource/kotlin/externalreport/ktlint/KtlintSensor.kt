/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2024 SonarSource SA
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

import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.notifications.AnalysisWarnings
import org.sonarsource.kotlin.api.frontend.AbstractPropertyHandlerSensor
import org.sonarsource.kotlin.api.common.RULE_REPOSITORY_LANGUAGE

class KtlintSensor(val analysisWarnings: AnalysisWarnings) : AbstractPropertyHandlerSensor(
    analysisWarnings,
    LINTER_KEY,
    LINTER_NAME,
    REPORT_PROPERTY_KEY,
    RULE_REPOSITORY_LANGUAGE,
) {
    companion object {
        const val LINTER_KEY = "ktlint"
        const val LINTER_NAME = "ktlint"
        const val REPORT_PROPERTY_KEY = "sonar.kotlin.ktlint.reportPaths"
    }

    override fun reportConsumer(context: SensorContext) = ReportImporter(analysisWarnings, context)::importFile
}
