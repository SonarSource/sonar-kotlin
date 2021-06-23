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
package org.sonarsource.slang.testing;

import org.sonarsource.slang.impl.LiteralTreeImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.sonarsource.slang.testing.TreesAssert.assertTrees;
import static java.util.Collections.singletonList;

class TreesAssertTest {

  private static final LiteralTreeImpl LITERAL_42 = new LiteralTreeImpl(null, "42");

  @Test
  void equivalent_ok() {
    assertTrees(singletonList(LITERAL_42)).isEquivalentTo(singletonList(new LiteralTreeImpl(null, "42")));
  }

  @Test
  void equivalent_failure() {
    assertThrows(AssertionError.class,
      () -> assertTrees(singletonList(LITERAL_42)).isEquivalentTo(singletonList(new LiteralTreeImpl(null, "43"))));
  }

  @Test
  void notequivalent_ok() {
    assertTrees(singletonList(LITERAL_42)).isNotEquivalentTo(singletonList(new LiteralTreeImpl(null, "43")));
  }

  @Test
  void notequivalent_failure() {
    assertThrows(AssertionError.class,
      () -> assertTrees(singletonList(LITERAL_42)).isNotEquivalentTo(singletonList(new LiteralTreeImpl(null, "42"))));
  }

}
