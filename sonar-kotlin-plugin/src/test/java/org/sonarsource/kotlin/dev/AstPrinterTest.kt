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
package org.sonarsource.kotlin.dev

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.util.Files
import org.jetbrains.kotlin.config.LanguageVersion
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.sonarsource.kotlin.converter.Environment
import java.nio.file.Path
import kotlin.io.path.readText

private val INPUT_FILE = Path.of("src", "test", "resources", "ast-printing", "TestFileToPrintAstFor.kt").toAbsolutePath()
private val EXPECTED_DOT_OUTPUT = Path.of("src", "test", "resources", "ast-printing", "TestFileToPrintAstFor.dot")
private val EXPECTED_TXT_OUTPUT = Path.of("src", "test", "resources", "ast-printing", "TestFileToPrintAstFor.txt")
private val INPUT_KT = Environment(emptyList(), LanguageVersion.LATEST_STABLE).ktPsiFactory.createFile(INPUT_FILE.readText())

class AstPrinterTest {

    @BeforeEach
    fun `reset DotNode ID counter`() {
        DotNode.nextId = 0
    }

    @Test
    fun `ensure that dot file matches input file`() {
        val tmpFile = Files.newTemporaryFile()
        AstPrinter.dotPrint(INPUT_KT, tmpFile.toPath())

        assertThat(tmpFile.readText()).isEqualToIgnoringNewLines(EXPECTED_DOT_OUTPUT.readText())

        tmpFile.delete()
    }

    @Test
    fun `ensure that txt file matches input file`() {
        val tmpFile = Files.newTemporaryFile()
        AstPrinter.txtPrint(INPUT_KT, tmpFile.toPath(), INPUT_KT.viewProvider.document)

        assertThat(tmpFile.readText()).isEqualToIgnoringNewLines(EXPECTED_TXT_OUTPUT.readText())

        tmpFile.delete()
    }
}
