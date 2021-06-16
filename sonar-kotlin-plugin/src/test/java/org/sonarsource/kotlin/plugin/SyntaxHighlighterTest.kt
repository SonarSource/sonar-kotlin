package org.sonarsource.kotlin.plugin

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ObjectAssert
import org.junit.jupiter.api.Test
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.sensor.highlighting.TypeOfText
import org.sonar.api.batch.sensor.highlighting.TypeOfText.ANNOTATION
import org.sonar.api.batch.sensor.highlighting.TypeOfText.COMMENT
import org.sonar.api.batch.sensor.highlighting.TypeOfText.CONSTANT
import org.sonar.api.batch.sensor.highlighting.TypeOfText.KEYWORD
import org.sonar.api.batch.sensor.highlighting.TypeOfText.STRING
import org.sonar.api.batch.sensor.highlighting.TypeOfText.STRUCTURED_COMMENT
import org.sonar.api.batch.sensor.internal.SensorContextTester
import org.sonar.api.config.internal.MapSettings
import org.sonar.api.issue.NoSonarFilter
import org.sonarsource.slang.testing.AbstractSensorTest
import java.nio.file.Path
import kotlin.io.path.readText

class SyntaxHighlighterTest : AbstractSensorTest() {
    override fun repositoryKey() = KotlinPlugin.KOTLIN_REPOSITORY_KEY
    override fun language(): KotlinLanguage = KotlinLanguage(MapSettings().asConfig())

    @Test
    fun `file with class`() {
        val fileToTest = Path.of("src", "test", "resources", "highlighting", "fileWithClass.kt")

        val inputFile: InputFile = createInputFile("file1.kt", fileToTest.readText())

        context.fileSystem().add(inputFile)
        KotlinSensor(checkFactory(), fileLinesContextFactory, NoSonarFilter(), language()).execute(context)

        assertThat(context, inputFile)
            .isHighlighted(1, 1, KEYWORD)
            .isNotHighlighted(1, 9)
            .isHighlighted(3, 1, KEYWORD)
            .isNotHighlighted(3, 8)
            .isHighlighted(5, 1, KEYWORD)
            .isNotHighlighted(5, 7)
            .isHighlighted(6, 5, KEYWORD)
            .isHighlighted(6, 15, KEYWORD)
            .isNotHighlighted(6, 22)
            .isHighlighted(7, 9, KEYWORD)
            .isHighlighted(7, 15, KEYWORD)
            .isNotHighlighted(7, 19)
            .isNotHighlighted(7, 26)
            .isHighlighted(7, 32, CONSTANT)
            .isHighlighted(8, 9, KEYWORD)
            .isNotHighlighted(8, 13)
            .isHighlighted(8, 21, CONSTANT)
            .isHighlighted(9, 9, KEYWORD)
            .isNotHighlighted(9, 13)
            .isHighlighted(9, 21, CONSTANT)
            .isHighlighted(10, 9, KEYWORD)
            .isHighlighted(10, 15, KEYWORD)
            .isNotHighlighted(10, 19)
            .isHighlighted(10, 27, STRING)
            .isHighlighted(10, 28, STRING)
            .isHighlighted(11, 9, KEYWORD)
            .isNotHighlighted(11, 13)
            .isHighlighted(11, 21, STRING)
            .isHighlighted(11, 22, STRING)
            .isHighlighted(12, 9, KEYWORD)
            .isNotHighlighted(12, 13)
            .isHighlighted(12, 21, STRING)
            .isHighlighted(12, 22, STRING)
            .isHighlighted(15, 5, STRUCTURED_COMMENT)
            .isHighlighted(16, 8, STRUCTURED_COMMENT)
            .isHighlighted(17, 6, STRUCTURED_COMMENT)
            .isHighlighted(18, 5, KEYWORD)
            .isNotHighlighted(18, 9)
            .isNotHighlighted(18, 14)
            .isNotHighlighted(18, 20)
            .isNotHighlighted(18, 26)
            .isNotHighlighted(19, 9)
            .isHighlighted(19, 16, CONSTANT)
            .isHighlighted(19, 21, CONSTANT)
            .isNotHighlighted(19, 25)
            .isHighlighted(19, 31, STRING)
            .isNotHighlighted(21, 9)
            .isNotHighlighted(21, 15)
            .isNotHighlighted(22, 13)
            .isNotHighlighted(22, 21)
            .isNotHighlighted(25, 9)
            .isNotHighlighted(25, 14)
            .isNotHighlighted(26, 13)
            .isHighlighted(26, 21, KEYWORD)
            .isNotHighlighted(27, 13)
            .isHighlighted(27, 21, KEYWORD)
            .isHighlighted(31, 5, ANNOTATION)
            .isHighlighted(31, 13, ANNOTATION)
            .isHighlighted(32, 5, KEYWORD)
            .isHighlighted(32, 13, KEYWORD)
            .isNotHighlighted(32, 17)
            .isNotHighlighted(32, 18)
            .isHighlighted(33, 9, COMMENT)
            .isHighlighted(33, 12, COMMENT)
            .isHighlighted(36, 5, KEYWORD)
            .isHighlighted(36, 14, KEYWORD)
            .isNotHighlighted(36, 18)
            .isHighlighted(38, 1, KEYWORD)
            .isHighlighted(38, 6, KEYWORD)
            .isNotHighlighted(38, 12)
            .isHighlighted(38, 14, KEYWORD)
            .isNotHighlighted(38, 18)
            .isNotHighlighted(38, 21)
    }
}

private class HighlightingAssert(
    context: SensorContextTester, inputFile: InputFile,
) : ObjectAssert<Pair<SensorContextTester, InputFile>>(context to inputFile) {
    fun isHighlighted(line: Int, offset: Int, type: TypeOfText): HighlightingAssert {
        val (context, inputFile) = actual
        assertThat(context.highlightingTypeAt(inputFile.key(), line, offset - 1)).containsExactly(type)

        return this
    }

    fun isNotHighlighted(line: Int, offset: Int): HighlightingAssert {
        val (context, inputFile) = actual
        assertThat(context.highlightingTypeAt(inputFile.key(), line, offset)).isEmpty()

        return this
    }
}

private fun assertThat(context: SensorContextTester, inputFile: InputFile) = HighlightingAssert(context, inputFile)
