/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2024 SonarSource SA
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
