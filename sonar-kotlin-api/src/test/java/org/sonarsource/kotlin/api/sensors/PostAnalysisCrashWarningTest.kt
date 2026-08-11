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
package org.sonarsource.kotlin.api.sensors

import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.sonar.api.notifications.AnalysisWarnings

internal class PostAnalysisCrashWarningTest {

    @Test
    fun `posts nothing when no file crashed`() {
        val warnings = mockk<AnalysisWarnings>(relaxed = true)
        postAnalysisCrashWarning(warnings, emptyList())
        verify(exactly = 0) { warnings.addUnique(any()) }
    }

    @Test
    fun `single crashed file uses singular wording and flags the whole analysis`() {
        val warnings = mockk<AnalysisWarnings>(relaxed = true)
        val message = slot<String>()
        postAnalysisCrashWarning(warnings, listOf("a.kt"))
        verify(exactly = 1) { warnings.addUnique(capture(message)) }
        assertThat(message.captured)
            .isEqualTo(
                "1 file crashed during analysis; the Kotlin analyzer recovered and completed the scan, " +
                    "but the analysis may be incomplete: a.kt."
            )
    }

    @Test
    fun `multiple crashed files use plural wording`() {
        val warnings = mockk<AnalysisWarnings>(relaxed = true)
        val message = slot<String>()
        postAnalysisCrashWarning(warnings, listOf("a.kt", "b.kt"))
        verify(exactly = 1) { warnings.addUnique(capture(message)) }
        assertThat(message.captured).startsWith("2 files crashed during analysis;")
        assertThat(message.captured).endsWith("a.kt, b.kt.")
    }

    @Test
    fun `the listed file names are capped at ten with an overflow suffix`() {
        val warnings = mockk<AnalysisWarnings>(relaxed = true)
        val message = slot<String>()
        val files = (1..13).map { "f$it.kt" }
        postAnalysisCrashWarning(warnings, files)
        verify(exactly = 1) { warnings.addUnique(capture(message)) }
        assertThat(message.captured).startsWith("13 files crashed during analysis;")
        assertThat(message.captured).contains("f1.kt, f2.kt, f3.kt, f4.kt, f5.kt, f6.kt, f7.kt, f8.kt, f9.kt, f10.kt")
        assertThat(message.captured).doesNotContain("f11.kt")
        assertThat(message.captured).endsWith("(and 3 more).")
    }
}
