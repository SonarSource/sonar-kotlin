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
package org.sonarsource.kotlin.converter

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

internal class KotlinCodeVerifierTest {
    private val kotlinCodeVerifier = KotlinCodeVerifier()
    @Test
    fun testContainsCode() {
        Assertions.assertThat(kotlinCodeVerifier.containsCode("This is a normal sentence: definitely not code")).isFalse
        Assertions.assertThat(kotlinCodeVerifier.containsCode("this is a normal comment")).isFalse
        Assertions.assertThat(kotlinCodeVerifier.containsCode("just three words")).isFalse
        Assertions.assertThat(kotlinCodeVerifier.containsCode("SNAPSHOT")).isFalse
        Assertions.assertThat(kotlinCodeVerifier.containsCode("")).isFalse
        Assertions.assertThat(kotlinCodeVerifier.containsCode(" ")).isFalse
        Assertions.assertThat(kotlinCodeVerifier.containsCode(" continue ")).isFalse
        Assertions.assertThat(kotlinCodeVerifier.containsCode("description for foo(\"hello world\")")).isFalse
        Assertions.assertThat(kotlinCodeVerifier.containsCode("foo(\"hello world\")")).isTrue
        Assertions.assertThat(kotlinCodeVerifier.containsCode("foo.rs")).isFalse

        // containing some keywords or operations
        Assertions.assertThat(kotlinCodeVerifier.containsCode("this is a + b")).isFalse
        Assertions.assertThat(kotlinCodeVerifier.containsCode("The user name is empty")).isFalse
        Assertions.assertThat(kotlinCodeVerifier.containsCode("exposed as public")).isFalse
        Assertions.assertThat(kotlinCodeVerifier.containsCode(" --- check")).isFalse
        Assertions.assertThat(kotlinCodeVerifier.containsCode("Loops --- ")).isFalse
        Assertions.assertThat(kotlinCodeVerifier.containsCode(
            "Generic Query tests (combining both FungibleState and LinearState contract types)")).isFalse
        Assertions.assertThat(kotlinCodeVerifier.containsCode("\"E0308 cyclic type of infinite size\"")).isFalse
        Assertions.assertThat(kotlinCodeVerifier.containsCode("(just remove it)")).isFalse

        // infix
        Assertions.assertThat(kotlinCodeVerifier.containsCode("1 shl 2")).isFalse
        Assertions.assertThat(kotlinCodeVerifier.containsCode("1 shl foo")).isFalse

        // kdoc tags
        Assertions.assertThat(kotlinCodeVerifier.containsCode("* @return foo(bar)")).isFalse
        Assertions.assertThat(kotlinCodeVerifier.containsCode("only unlocked states")).isFalse

        // numeral literals
        Assertions.assertThat(kotlinCodeVerifier.containsCode(" 0\n 1")).isFalse
    }
}
