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

import io.mockk.spyk
import io.mockk.verify
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.junit.jupiter.api.Test
import org.sonar.api.batch.sensor.issue.internal.DefaultNoSonarFilter
import org.sonar.api.issue.NoSonarFilter
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.testing.AbstractSensorTest
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
        KotlinSensor(checkFactory("S99999"), fileLinesContextFactory, DefaultNoSonarFilter(), language()).also { sensor ->
            sensor.checks.addAnnotatedChecks(dummyCheck)
            sensor.execute(context)
        }

        verify(exactly = 1) { dummyCheck.visitNamedFunction(any(), any()) }
    }
}
