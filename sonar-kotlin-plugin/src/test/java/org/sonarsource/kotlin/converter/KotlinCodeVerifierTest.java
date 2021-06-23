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
package org.sonarsource.kotlin.converter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KotlinCodeVerifierTest {
  private KotlinCodeVerifier kotlinCodeVerifier = new KotlinCodeVerifier();
  @Test
  void testContainsCode() {

    assertThat(kotlinCodeVerifier.containsCode("This is a normal sentence: definitely not code")).isFalse();
    assertThat(kotlinCodeVerifier.containsCode("this is a normal comment")).isFalse();
    assertThat(kotlinCodeVerifier.containsCode("just three words")).isFalse();
    assertThat(kotlinCodeVerifier.containsCode("SNAPSHOT")).isFalse();
    assertThat(kotlinCodeVerifier.containsCode("")).isFalse();
    assertThat(kotlinCodeVerifier.containsCode(" ")).isFalse();
    assertThat(kotlinCodeVerifier.containsCode(" continue ")).isFalse();

    assertThat(kotlinCodeVerifier.containsCode("description for foo(\"hello world\")")).isFalse();
    assertThat(kotlinCodeVerifier.containsCode("foo(\"hello world\")")).isTrue();
    assertThat(kotlinCodeVerifier.containsCode("foo.rs")).isFalse();

    // containing some keywords or operations
    assertThat(kotlinCodeVerifier.containsCode("this is a + b")).isFalse();
    assertThat(kotlinCodeVerifier.containsCode("The user name is empty")).isFalse();
    assertThat(kotlinCodeVerifier.containsCode("exposed as public")).isFalse();
    assertThat(kotlinCodeVerifier.containsCode(" --- check")).isFalse();
    assertThat(kotlinCodeVerifier.containsCode("Loops --- ")).isFalse();
    assertThat(kotlinCodeVerifier.containsCode(
      "Generic Query tests (combining both FungibleState and LinearState contract types)")).isFalse();
    assertThat(kotlinCodeVerifier.containsCode("\"E0308 cyclic type of infinite size\"")).isFalse();
    assertThat(kotlinCodeVerifier.containsCode("(just remove it)")).isFalse();

    // infix
    assertThat(kotlinCodeVerifier.containsCode("1 shl 2")).isFalse();
    assertThat(kotlinCodeVerifier.containsCode("1 shl foo")).isFalse();

    // kdoc tags
    assertThat(kotlinCodeVerifier.containsCode("* @return foo(bar)")).isFalse();

    assertThat(kotlinCodeVerifier.containsCode("only unlocked states")).isFalse();

    // numeral literals
    assertThat(kotlinCodeVerifier.containsCode(" 0\n 1")).isFalse();

  }
}
