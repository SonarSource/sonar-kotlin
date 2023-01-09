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
package org.sonarsource.kotlin.checks.testing

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.sonarsource.kotlin.plugin.KOTLIN_CHECKS
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.streams.asSequence

class CheckConfigurationIntegrityTest {

    companion object {
        private val CHECKS_PACKAGE_DIRECTORIES = listOf(
            Path.of("checks")
        ).map { Path.of("src", "main", "java", "org", "sonarsource", "kotlin").resolve(it) }
    }

    @Test
    fun `ensure all checks are actually registered in KotlinCheckList`() {
        val expectedChecks = CHECKS_PACKAGE_DIRECTORIES.flatMap { checksDir ->
            Files.walk(checksDir, 1).asSequence()
        }.filter {
            isCheckFile(it)
        }.map {
            it.fileName.toString().substringBefore(".kt")
        }

        val actualChecks = KOTLIN_CHECKS.map { it.simpleName }

        Assertions.assertThat(actualChecks).hasSameElementsAs(expectedChecks)
    }

    private fun isCheckFile(candidate: Path) = candidate.isRegularFile() && candidate.fileName.toString().endsWith("Check.kt")
}
