/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2023 SonarSource SA
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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class KotlinCodeVerifierTest {
    @Test
    fun testContainsCode() {
        assertThat(KotlinCodeVerifier.containsCode("This is a normal sentence: definitely not code")).isFalse
        assertThat(KotlinCodeVerifier.containsCode("this is a normal comment")).isFalse
        assertThat(KotlinCodeVerifier.containsCode("just three words")).isFalse
        assertThat(KotlinCodeVerifier.containsCode("SNAPSHOT")).isFalse
        assertThat(KotlinCodeVerifier.containsCode("")).isFalse
        assertThat(KotlinCodeVerifier.containsCode(" ")).isFalse
        assertThat(KotlinCodeVerifier.containsCode(" continue ")).isFalse
        assertThat(KotlinCodeVerifier.containsCode("description for foo(\"hello world\")")).isFalse
        assertThat(KotlinCodeVerifier.containsCode("foo(\"hello world\")")).isTrue
        assertThat(KotlinCodeVerifier.containsCode("foo.rs")).isFalse

        // containing some keywords or operations
        assertThat(KotlinCodeVerifier.containsCode("this is a + b")).isFalse
        assertThat(KotlinCodeVerifier.containsCode("The user name is empty")).isFalse
        assertThat(KotlinCodeVerifier.containsCode("exposed as public")).isFalse
        assertThat(KotlinCodeVerifier.containsCode(" --- check")).isFalse
        assertThat(KotlinCodeVerifier.containsCode("Loops --- ")).isFalse
        assertThat(
            KotlinCodeVerifier.containsCode(
                "Generic Query tests (combining both FungibleState and LinearState contract types)"
            )
        ).isFalse
        assertThat(KotlinCodeVerifier.containsCode("\"E0308 cyclic type of infinite size\"")).isFalse
        assertThat(KotlinCodeVerifier.containsCode("(just remove it)")).isFalse

        // infix
        assertThat(KotlinCodeVerifier.containsCode("1 shl 2")).isFalse
        assertThat(KotlinCodeVerifier.containsCode("1 shl foo")).isFalse

        // kdoc tags
        assertThat(KotlinCodeVerifier.containsCode("* @return foo(bar)")).isFalse
        assertThat(KotlinCodeVerifier.containsCode("only unlocked states")).isFalse

        // numeral literals
        assertThat(KotlinCodeVerifier.containsCode(" 0\n 1")).isFalse

        // Code within text comments is not counted, as it is probably used for explanation purposes
        assertThat(KotlinCodeVerifier.containsCode("if (foo) { doSomething() } else { somethingElse }")).isTrue
        assertThat(
            KotlinCodeVerifier.containsCode(
                "An if-else statement is written as follows: if (foo) { doSomething() } else { somethingElse }"
            )
        ).isFalse

        // Short code
        assertThat(KotlinCodeVerifier.containsCode("            ctx.stopLoop()")).isTrue

        // more complex string expression
        assertThat(KotlinCodeVerifier.containsCode("""println("${'$'}foo says ${'$'}bar")""")).isTrue

        // long sentence with a little bit that looks like code
        assertThat(
            KotlinCodeVerifier.containsCode(
                "only unlocked states only soft locked states only those soft locked states specified by lock id(s) all unlocked " +
                        "states plus those soft locked states specified by lock id(s)"
            )
        ).isFalse
    }

    @Test
    fun `parsing error returns false`() {
        assertThat(KotlinCodeVerifier.containsCode("if (foo) { doSomething() } else { somethingElse } }")).isFalse
    }

    @Test
    fun `constructs that may be used in natural language are only parsed as code if someone is writing a very strange comment`() {
        assertThat(
            KotlinCodeVerifier.containsCode(
                """"public abstract class. return throw private. internal enum continue assert. Float super true false object companion """"
            )
        ).isTrue // Okay FP
    }


}
