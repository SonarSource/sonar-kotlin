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

import io.mockk.spyk
import io.mockk.verify
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.junit.jupiter.api.Test
import org.sonar.api.batch.sensor.issue.internal.DefaultNoSonarFilter
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.testapi.AbstractSensorTest
import kotlin.time.ExperimentalTime

class CheckRegistrationTest : AbstractSensorTest() {

    @Rule(key = "S99999")
    class DummyCheck : AbstractCheck() {
        override fun visitNamedFunction(function: KtNamedFunction, data: KotlinFileContext?) {
        }
    }

    @ExperimentalTime
    @Test
    fun ensure_check_registration_works() {
        val inputFile = createInputFile("file1.kt", """
            fun main(args: Array<String>) {
                print (1 == 1);
            }
             """.trimIndent())
        context.fileSystem().add(inputFile)
        val dummyCheck = spyk(DummyCheck())
        KotlinSensor(checkFactory("S99999"), fileLinesContextFactory, DefaultNoSonarFilter(), language(), KotlinProjectSensor()).also { sensor ->
            sensor.checks.addAnnotatedChecks(dummyCheck)
            sensor.execute(context)
        }

        verify(exactly = 1) { dummyCheck.visitNamedFunction(any(), any()) }
    }
}
