/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2026 SonarSource SA
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

import io.mockk.every
import io.mockk.slot
import io.mockk.spyk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.sonar.api.batch.sensor.internal.SensorContextTester
import java.nio.file.Path

class KotlinProjectSensorTest {

    @Test
    fun `execute reports telemetry`() {
        val sensor = KotlinProjectSensor()
        val telemetry = mutableMapOf<String, String>()
        val context = spyk(SensorContextTester.create(Path.of("."))) {
            val key = slot<String>()
            val value = slot<String>()
            every { addTelemetryProperty(capture(key), capture(value)) } answers { telemetry[key.captured] = value.captured }
        }

        sensor.execute(context)
        assertThat(telemetry).isEqualTo(mapOf("kotlin.android" to "0"))

        sensor.telemetryData.hasAndroidImports = true
        sensor.execute(context)
        assertThat(telemetry).isEqualTo(mapOf("kotlin.android" to "1"))
    }

}
