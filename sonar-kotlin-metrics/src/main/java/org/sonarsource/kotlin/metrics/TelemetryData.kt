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
package org.sonarsource.kotlin.metrics

import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.utils.Version

private val MIN_SQS_SUPPORTED: Version = Version.create(10, 9)

class TelemetryData {
    var hasAndroidImports = false
    var hasAndroidImportsReportedAsTrue = false

    fun report(sensorContext: SensorContext) {
        if (hasAndroidImportsReportedAsTrue || !sensorContext.isTelemetrySupported()) return
        sensorContext.addTelemetryProperty("kotlin.android", if (hasAndroidImports) "1" else "0")
        hasAndroidImportsReportedAsTrue = hasAndroidImports
    }
}

private fun SensorContext.isTelemetrySupported() =
    this.runtime().apiVersion.isGreaterThanOrEqual(MIN_SQS_SUPPORTED)

