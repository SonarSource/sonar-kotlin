package org.sonarsource.kotlin.externalreport.ast

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
        Assertions.assertThat(tmpFile.readText()).isEqualTo(EXPECTED_DOT_OUTPUT.readText())

        tmpFile.delete()
    }

    @Test
    fun `ensure that txt file matches input file`() {
        val tmpFile = Files.newTemporaryFile()

        main("txt", INPUT_FILE.absolutePathString(), tmpFile.absolutePath)
        Assertions.assertThat(tmpFile.readText()).isEqualTo(EXPECTED_TXT_OUTPUT.readText())

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
