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

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.slf4j.event.Level
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.batch.sensor.issue.MessageFormatting
import org.sonar.api.batch.sensor.issue.NewIssue
import org.sonar.api.batch.sensor.issue.NewIssueLocation
import org.sonar.api.batch.sensor.issue.NewMessageFormatting
import org.sonar.api.rule.RuleKey
import org.sonar.api.testfixtures.log.LogTesterJUnit5
import org.sonarsource.kotlin.api.Message
import org.sonarsource.kotlin.api.message

class InputFileContextImplTest {

    @JvmField
    @RegisterExtension
    var logTester = LogTesterJUnit5()

    private lateinit var newMessageFormatting: NewMessageFormatting
    private lateinit var newLocation: NewIssueLocation
    private lateinit var newIssue: NewIssue
    private lateinit var sensorContext: SensorContext
    private lateinit var inputFileContextImpl: InputFileContextImpl
    private val message = message {
        +"plain text "
        code("code")
        +" more text"
    }

    @BeforeEach
    fun initializeMocks() {
        newMessageFormatting = spyk<NewMessageFormatting>()
        newLocation = mockk<NewIssueLocation> {
            every { newMessageFormatting() } returns newMessageFormatting
            every { on(any()) } returns this
            every { message(any()) } returns this
            every { message(any(), any()) } returns this
        }
        newIssue = mockk<NewIssue> {
            every { newLocation() } returns newLocation
            every { forRule(any()) } returns this
            every { at(any()) } returns this
            every { gap(any()) } returns this
            every { save() } returns Unit
        }
        sensorContext = mockk<SensorContext> {
            every { newIssue() } returns newIssue
        }
        inputFileContextImpl = InputFileContextImpl(sensorContext, mockk<InputFile>(), false)
    }

    @Test
    fun `ensure message formatting is included in issue report`() {
        inputFileContextImpl.reportIssue(RuleKey.of("Kotlin", "S1"), null, message, emptyList(), null)

        verify(exactly = 1) { newLocation.message("plain text code more text", listOf(newMessageFormatting)) }
        verify(exactly = 0) { newLocation.message(any()) }
        verify(exactly = 1) { newMessageFormatting.start(11) }
        verify(exactly = 1) { newMessageFormatting.end(15) }
        verify(exactly = 1) { newMessageFormatting.type(MessageFormatting.Type.CODE) }
    }

    @Test
    fun `ensure we don't crash when running in old product version`() {
        every { newLocation.message(any(), any()) } throws NoSuchMethodError()

        inputFileContextImpl.reportIssue(RuleKey.of("Kotlin", "S1"), null, message, emptyList(), null)
        inputFileContextImpl.reportIssue(RuleKey.of("Kotlin", "S1"), null, Message("message2"), emptyList(), null)

        verify(exactly = 1) { newLocation.message("plain text code more text") }
        verify(exactly = 1) { newLocation.message("message2") }
        assertThat(logTester.logs(Level.WARN))
            .containsOnlyOnce("Could not report issue with code highlighting, using plain text instead. Check whether the product is outdated.")
    }
}
