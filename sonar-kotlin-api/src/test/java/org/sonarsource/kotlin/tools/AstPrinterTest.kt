/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2026 SonarSource SA
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
package org.sonarsource.kotlin.tools

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.config.LanguageVersion
import java.nio.file.Path
import kotlin.io.path.readText
import org.assertj.core.api.Assertions
import org.assertj.core.util.Files
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.sonarsource.kotlin.api.frontend.Environment

private val INPUT_FILE = Path.of("src", "test", "resources", "ast-printing", "TestFileToPrintAstFor.kt").toAbsolutePath()
private val EXPECTED_DOT_OUTPUT = Path.of("src", "test", "resources", "ast-printing", "TestFileToPrintAstFor.dot")
private val EXPECTED_TXT_OUTPUT = Path.of("src", "test", "resources", "ast-printing", "TestFileToPrintAstFor.txt")

class AstPrinterTest {
    private val disposable = Disposer.newDisposable()

    @AfterEach
    fun dispose() {
        Disposer.dispose(disposable)
    }

    private val inputKt = Environment(disposable, emptyList(), LanguageVersion.LATEST_STABLE).ktPsiFactory.createFile(INPUT_FILE.readText())

    @BeforeEach
    fun `reset DotNode ID counter`() {
        DotNode.nextId = 0
    }

    @Test
    fun `ensure that dot file matches input file`() {
        val tmpFile = Files.newTemporaryFile()
        AstPrinter.dotPrint(inputKt, tmpFile.toPath())

        Assertions.assertThat(tmpFile.readText()).isEqualToIgnoringNewLines(EXPECTED_DOT_OUTPUT.readText())

        tmpFile.delete()
    }

    @Test
    fun `ensure that txt file matches input file`() {
        val tmpFile = Files.newTemporaryFile()
        AstPrinter.txtPrint(inputKt, tmpFile.toPath(), inputKt.viewProvider.document)

        Assertions.assertThat(tmpFile.readText()).isEqualToIgnoringNewLines(EXPECTED_TXT_OUTPUT.readText())

        tmpFile.delete()
    }
}