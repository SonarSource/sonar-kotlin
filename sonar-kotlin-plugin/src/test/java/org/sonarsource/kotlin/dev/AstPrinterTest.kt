package org.sonarsource.kotlin.dev

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.util.Files
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.sonarsource.kotlin.converter.Environment
import java.nio.file.Path
import kotlin.io.path.readText

private val INPUT_FILE = Path.of("src", "test", "resources", "ast-printing", "TestFileToPrintAstFor.kt").toAbsolutePath()
private val EXPECTED_DOT_OUTPUT = Path.of("src", "test", "resources", "ast-printing", "TestFileToPrintAstFor.dot")
private val EXPECTED_TXT_OUTPUT = Path.of("src", "test", "resources", "ast-printing", "TestFileToPrintAstFor.txt")
private val INPUT_KT = Environment(emptyList()).ktPsiFactory.createFile(INPUT_FILE.readText())

class AstPrinterTest {

    @BeforeEach
    fun `reset DotNode ID counter`() {
        DotNode.nextId = 0
    }

    @Test
    fun `ensure that dot file matches input file`() {
        val tmpFile = Files.newTemporaryFile()
        AstPrinter.dotPrint(INPUT_KT, tmpFile.toPath())

        assertThat(tmpFile.readText()).isEqualTo(EXPECTED_DOT_OUTPUT.readText())

        tmpFile.delete()
    }

    @Test
    fun `ensure that txt file matches input file`() {
        val tmpFile = Files.newTemporaryFile()
        AstPrinter.txtPrint(INPUT_KT, tmpFile.toPath(), INPUT_KT.viewProvider.document)

        assertThat(tmpFile.readText()).isEqualTo(EXPECTED_TXT_OUTPUT.readText())

        tmpFile.delete()
    }
}
