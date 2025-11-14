/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.streams.asSequence

class CheckConfigurationIntegrityTest {

    companion object {
        private val CHECKS_PACKAGE_DIRECTORIES = listOf(
            Path.of("checks")
        ).map { Path.of("..", "sonar-kotlin-checks", "src", "main", "java", "org", "sonarsource", "kotlin").resolve(it) }
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
