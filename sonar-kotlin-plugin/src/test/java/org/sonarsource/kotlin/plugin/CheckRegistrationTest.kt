/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2026 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1.0.1, as published by SonarSource Sàrl.
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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.sonar.api.batch.sensor.issue.internal.DefaultNoSonarFilter
import org.sonar.api.rule.RuleKey
import org.sonarsource.kotlin.metrics.TelemetryData
import org.sonarsource.kotlin.testapi.AbstractSensorTest

class CheckRegistrationTest : AbstractSensorTest() {

    @Test
    fun ensure_check_registration_works() {
        val inputFile = createInputFile("file1.kt", """
            fun main(args: Array<String>) {
                print (1 == 1);
            }
             """.trimIndent())
        context.fileSystem().add(inputFile)
        KotlinSensor(
            checkFactory(listOf(RuleKey.of("dummy", "DummyRule"))),
            fileLinesContextFactory,
            DefaultNoSonarFilter(),
            language(),
            TelemetryData(),
            arrayOf(DummyKotlinPluginExtensionsProvider()),
        ).execute(context)

        assertEquals(1, context.allIssues().size)
    }
}
