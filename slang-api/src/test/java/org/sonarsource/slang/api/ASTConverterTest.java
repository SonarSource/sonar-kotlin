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
package org.sonarsource.slang.api;

import org.junit.jupiter.api.Test;
import org.sonarsource.slang.impl.IdentifierTreeImpl;

import static org.junit.jupiter.api.Assertions.assertSame;

class ASTConverterTest {

  private static final ASTConverter DUMMY_CONVERTER = new ASTConverter() {
    private final Tree SIMPLE_TREE = new IdentifierTreeImpl(null, "name");
    @Override
    public Tree parse(String content) {
      return SIMPLE_TREE;
    }
  };

  @Test
  void parse_with_file_has_no_effect_by_default() {
    assertSame(DUMMY_CONVERTER.parse(""), DUMMY_CONVERTER.parse("", "file name"));
  }

}
