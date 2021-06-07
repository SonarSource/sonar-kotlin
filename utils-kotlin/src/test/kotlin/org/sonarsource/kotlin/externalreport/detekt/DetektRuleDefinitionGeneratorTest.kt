package org.sonarsource.kotlin.externalreport.detekt

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.sonarsource.kotlin.externalreport.getActual
import java.nio.file.Path
import kotlin.io.path.readText

class DetektRuleDefinitionGeneratorTest {
    @Test
    fun `ensure that the script generates the same result as the current mapping`() {
        val expected = Path.of("..").resolve(DEFAULT_RULES_FILE).readText()

        val actual = getActual { main(*it) }

        assertThat(actual).isEqualTo(expected)
    }
}
