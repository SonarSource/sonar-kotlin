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
package org.sonarsource.kotlin.metrics

import org.assertj.core.api.Assertions
import org.jetbrains.kotlin.config.LanguageVersion
import org.junit.jupiter.api.Test
import org.sonarsource.kotlin.api.frontend.Environment

internal class StatementsVisitorTest {

    @Test
    fun `should count top level without declarations and blocks`() {
        val content = """
            package abc
            import xyz
            
            class A{}
            fun bar() = if (a) println()
            fun foo() {}
            """.trimIndent()
        Assertions.assertThat(statements(content)).isEqualTo(2)
    }

    @Test
    fun `should count statements inside blocks`() {
        val content = """
            fun foo(a: Boolean) {
              foo()
              if (a) { 
                foo()
                bar()
              }
              class A{}
              fun bar(){}
            }
            fun f() {
              var a = 2
              var b = 3 
            }""".trimIndent()
        Assertions.assertThat(statements(content)).isEqualTo(6)
    }
}

private fun statements(content: String): Int {
    val ktFile = Environment(emptyList(), LanguageVersion.LATEST_STABLE).ktPsiFactory.createFile(content)
    val statementsVisitor = StatementsVisitor()
    ktFile.accept(statementsVisitor)
    return statementsVisitor.statements
}
