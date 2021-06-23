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
package org.sonarsource.slang.impl;

import org.junit.jupiter.api.Test;
import org.sonarsource.slang.api.PlaceHolderTree;
import org.sonarsource.slang.api.Token;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceHolderTreeImplTest {

  @Test
  void test_place_holder() {
    TokenImpl keyword = new TokenImpl(new TextRangeImpl(1, 0, 1, 1), "_", Token.Type.OTHER);
    PlaceHolderTree placeHolderTree = new PlaceHolderTreeImpl(null, keyword);
    assertThat(placeHolderTree.children()).isEmpty();
    assertThat(placeHolderTree.placeHolderToken().text()).isEqualTo("_");
    assertThat(placeHolderTree.placeHolderToken().type()).isEqualTo(Token.Type.OTHER);
  }
}
