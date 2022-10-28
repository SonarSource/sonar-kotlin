package org.sonarsource.kotlin.externalreport.ktlint

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.sonarsource.kotlin.externalreport.getActual
import java.nio.file.Path
import kotlin.io.path.readText

class KtlintRuleDefinitionGeneratorTest {
    @Test
    fun `ensure that the script generates the same result as the current mapping`() {
        val expected = Path.of("..").resolve(DEFAULT_RULES_FILE).readText()

        val actual = getActual { main(*it) }

        Assertions.assertThat(actual).isEqualToIgnoringNewLines(expected)
    }
}
