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
package org.sonarsource.kotlin.externalreport.androidlint

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.sonarsource.kotlin.externalreport.getActual
import java.nio.file.Path
import kotlin.io.path.readText

class AndroidLintDefinitionTest {
    @Test
    fun `ensure that the script generates the same result as the current mapping`() {
        val expected = Path.of("..").resolve(DEFAULT_RULES_FILE).readText()
        val androidLintHelperPath = "src/main/resources/android-lint-help.txt"

        val actual = getActual(androidLintHelperPath) { main(*it) }

        assertThat(actual).isEqualToIgnoringNewLines(expected)
    }

    @Test
    fun `ensure that the script throws exception if 'androidLintHelperPath' is not present`() {
        val exception = assertThrows<IllegalStateException> { getActual("does-not-exist.txt") { main(*it) } }
        assertThat(exception.message).isEqualTo("Can't load android-lint-help.txt")
    }

    @Test
    fun `ensure that the script throws exception if 'androidLintHelperPath' is invalid`() {
        val file = Path.of("src", "test", "resources").resolve("invalid-android-lint-help.txt").toString()
        val exception = assertThrows<IllegalStateException> { getActual(file) { main(*it) } }
        assertThat(exception.message).isEqualTo("Unexpected android-lint-help.txt first line: Correctness")
    }

    @Test
    fun `ensure that the script throws exception if 'androidLintHelperPath' has invalid header in issue`() {
        val file = Path.of("src", "test", "resources").resolve("android-lint-help-with-invalid-issue.txt").toString()
        val exception = assertThrows<IllegalStateException> { getActual(file) { main(*it) } }
        assertThat(exception.message).isEqualTo("Unexpected line at 8 instead of 'Summary:' header: NotASummary: AdapterView cannot have children in XML")
    }
}
