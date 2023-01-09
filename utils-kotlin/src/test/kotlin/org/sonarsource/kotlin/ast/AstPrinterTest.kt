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
package org.sonarsource.kotlin.ast

import org.assertj.core.api.Assertions
import org.assertj.core.util.Files
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.sonarsource.kotlin.dev.DotNode
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.readText

private val INPUT_FILE =
    Path.of("..", "sonar-kotlin-plugin", "src", "test", "resources", "ast-printing", "TestFileToPrintAstFor.kt").toAbsolutePath()
private val EXPECTED_DOT_OUTPUT =
    Path.of("..", "sonar-kotlin-plugin", "src", "test", "resources", "ast-printing", "TestFileToPrintAstFor.dot")
private val EXPECTED_TXT_OUTPUT =
    Path.of("..", "sonar-kotlin-plugin", "src", "test", "resources", "ast-printing", "TestFileToPrintAstFor.txt")

class AstPrinterTest {
    @BeforeEach
    fun `reset DotNode ID counter`() {
        DotNode.nextId = 0
    }

    @Test
    fun `ensure that dot file matches input file`() {
        val tmpFile = Files.newTemporaryFile()

        main("dot", INPUT_FILE.absolutePathString(), tmpFile.absolutePath)
        Assertions.assertThat(tmpFile.readText()).isEqualToIgnoringNewLines(EXPECTED_DOT_OUTPUT.readText())

        tmpFile.delete()
    }

    @Test
    fun `ensure that txt file matches input file`() {
        val tmpFile = Files.newTemporaryFile()

        main("txt", INPUT_FILE.absolutePathString(), tmpFile.absolutePath)
        Assertions.assertThat(tmpFile.readText()).isEqualToIgnoringNewLines(EXPECTED_TXT_OUTPUT.readText())

        tmpFile.delete()
    }

    @Test
    fun `throw exception on invalid args`() {
        val msg = """Usage: main(<dot|txt>, <inputFile>[, outputFile])""""
        assertThrows<IllegalArgumentException>(message = msg) { main() }
        assertThrows<IllegalArgumentException>(message = msg) { main("invalid-mode", "foo") }
        assertThrows<IllegalArgumentException>(message = msg) { main("dot") }
    }
}
