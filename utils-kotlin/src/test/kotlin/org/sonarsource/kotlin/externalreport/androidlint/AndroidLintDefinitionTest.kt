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

        assertThat(actual).isEqualTo(expected)
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
