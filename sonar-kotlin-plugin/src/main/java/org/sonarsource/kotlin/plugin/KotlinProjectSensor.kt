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
package org.sonarsource.kotlin.plugin

import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.batch.sensor.SensorDescriptor
import org.sonar.api.scanner.sensor.ProjectSensor
import org.sonar.api.utils.Version
import org.sonarsource.kotlin.api.common.KOTLIN_LANGUAGE_KEY
import org.sonarsource.kotlin.metrics.TelemetryData

class KotlinProjectSensor : ProjectSensor {
    val telemetryData: TelemetryData = TelemetryData()

    override fun describe(descriptor: SensorDescriptor) {
        descriptor.onlyOnLanguage(KOTLIN_LANGUAGE_KEY).name("KotlinProjectSensor")
    }

    /**
     * Executed once for entire project after all executions of [KotlinSensor.execute] for individual modules.
     */
    override fun execute(context: SensorContext) {
        if (context.runtime().apiVersion.isGreaterThanOrEqual(Version.create(10, 9))) {
            context.addTelemetryProperty("kotlin.android", if (telemetryData.hasAndroidImports) "1" else "0")
        }
    }
}
