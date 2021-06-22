/*
 * SonarSource SLang
 * Copyright (C) 2018-2021 SonarSource SA
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
package org.sonarsource.kotlin.plugin

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.sonarsource.kotlin.converter.KotlinConverter

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
    val root = KotlinConverter(emptyList()).parse(content)
    val statementsVisitor = StatementsVisitor()
    root.psiFile.accept(statementsVisitor)
    return statementsVisitor.statements
}
