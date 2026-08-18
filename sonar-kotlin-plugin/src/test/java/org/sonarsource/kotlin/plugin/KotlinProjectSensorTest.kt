/*
 * SonarSource Kotlin
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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
import org.junit.jupiter.api.extension.RegisterExtension
import com.sonarsource.scanner.engine.sensor.test.fixtures.SensorContextTester
import java.nio.file.Path
import org.slf4j.event.Level
import org.sonar.api.testfixtures.log.LogTesterJUnit5
import org.sonar.scanner.plugin.api.impl.config.MapSettings
import org.sonarsource.kotlin.metrics.TelemetryData

class KotlinProjectSensorTest {

    private val pluginVersion = "1.2.3-TEST"

    @RegisterExtension
    val logTester = LogTesterJUnit5().setLevel(Level.DEBUG)

    @Test
    fun `execute reports telemetry`() {
        val sensor = KotlinProjectSensor(TelemetryData())
        val telemetry = mutableMapOf<String, String>()
        val context = spyk(SensorContextTester.create(Path.of("."))) {
            val key = slot<String>()
            val value = slot<String>()
            every { addTelemetryProperty(capture(key), capture(value)) } answers { telemetry[key.captured] = value.captured }
        }

        sensor.execute(context)
        assertThat(telemetry)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    "kotlin.pluginVersion" to pluginVersion,
                    "kotlin.android" to "0",
                    "kotlin.reports.surefire.classes.failed" to "0",
                    "kotlin.reports.surefire.classes.imported" to "0",
                    "kotlin.reports.surefire.classes.duplicated" to "0",
                    "kotlin.reports.surefire.classes.overlapping" to "0",
                    "kotlin.files.processed" to "0",
                    "kotlin.files.read.failures" to "0",
                    "kotlin.files.parse.failures" to "0",
                    "kotlin.files.analysis.crashes" to "0",
                    "kotlin.scripts.processed" to "0",
                    "kotlin.scripts.read.failures" to "0",
                    "kotlin.scripts.parse.failures" to "0",
                    "kotlin.scripts.analysis.crashes" to "0",
                )
            )

        sensor.telemetryData.hasAndroidImports = true
        sensor.execute(context)
        assertThat(telemetry).containsExactlyInAnyOrderEntriesOf(mapOf(
            "kotlin.pluginVersion" to pluginVersion,
            "kotlin.android" to "1",
            "kotlin.reports.surefire.classes.failed" to "0",
            "kotlin.reports.surefire.classes.imported" to "0",
            "kotlin.reports.surefire.classes.duplicated" to "0",
            "kotlin.reports.surefire.classes.overlapping" to "0",
            "kotlin.files.processed" to "0",
            "kotlin.files.read.failures" to "0",
            "kotlin.files.parse.failures" to "0",
            "kotlin.files.analysis.crashes" to "0",
            "kotlin.scripts.processed" to "0",
            "kotlin.scripts.read.failures" to "0",
            "kotlin.scripts.parse.failures" to "0",
            "kotlin.scripts.analysis.crashes" to "0",
        ))
    }

    @Test
    fun `execute does not log telemetry by default`() {
        val context = SensorContextTester.create(Path.of("."))

        KotlinProjectSensor(TelemetryData()).execute(context)

        assertThat(context.telemetryProperties).isNotEmpty()
        assertThat(logTester.logs(Level.DEBUG)).noneMatch { it.startsWith("TELEMETRY:") }
    }

    @Test
    fun `execute logs telemetry when extended logging is enabled`() {
        val context = SensorContextTester.create(Path.of("."))
        context.setSettings(MapSettings().apply {
            setProperty(KotlinProjectSensor.EXTENDED_LOGGING_PROPERTY_NAME, true)
        })

        KotlinProjectSensor(TelemetryData()).execute(context)

        assertThat(logTester.logs(Level.DEBUG))
            .contains(
                "TELEMETRY: kotlin.pluginVersion=$pluginVersion",
                "TELEMETRY: kotlin.android=0",
                "TELEMETRY: kotlin.files.processed=0",
                "TELEMETRY: kotlin.scripts.analysis.crashes=0",
            )
    }

    @Test
    fun `resolvePluginVersion reads version from resource`() {
        assertThat(KotlinProjectSensor.resolvePluginVersion()).isEqualTo(pluginVersion)
    }

    @Test
    fun `resolvePluginVersion falls back to unknown when resource is missing`() {
        assertThat(KotlinProjectSensor.resolvePluginVersion("does/not/exist.properties")).isEqualTo("unknown")
    }

    @Test
    fun `resolvePluginVersion falls back to unknown when placeholder was not substituted`() {
        assertThat(KotlinProjectSensor.resolvePluginVersion("org/sonar/plugins/kotlin/pluginVersionUnsubstituted.properties")).isEqualTo("unknown")
    }

}
