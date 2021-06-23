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
package org.sonarsource.slang.checks.api;

import org.junit.jupiter.api.Test;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.impl.TextRangeImpl;
import org.sonarsource.slang.persistence.JsonTree;

import static org.assertj.core.api.Assertions.assertThat;

class SecondaryLocationTest {

  private static final Tree IDENTIFIER = JsonTree.fromJson("" +
    "{\n" +
    "  \"treeMetaData\": {\"tokens\": [{\"textRange\": \"1:0:1:3\", \"text\": \"foo\", \"type\": \"OTHER\"}]},\n" +
    "  \"tree\": {\"@type\": \"Identifier\", \"metaData\": \"1:0:1:3\", \"name\": \"foo\"}\n" +
    "}");

  @Test
  void constructor_with_tree() {
    SecondaryLocation location = new SecondaryLocation(IDENTIFIER);
    assertThat(location.textRange).isEqualTo(new TextRangeImpl(1, 0, 1, 3));
    assertThat(location.message).isNull();
  }

  @Test
  void constructor_with_tree_and_message() {
    SecondaryLocation location = new SecondaryLocation(IDENTIFIER, "because");
    assertThat(location.textRange).isEqualTo(new TextRangeImpl(1, 0, 1, 3));
    assertThat(location.message).isEqualTo("because");
  }

  @Test
  void constructor_with_text_range_and_message() {
    SecondaryLocation location = new SecondaryLocation(IDENTIFIER.textRange(), "because");
    assertThat(location.textRange).isEqualTo(new TextRangeImpl(1, 0, 1, 3));
    assertThat(location.message).isEqualTo("because");
  }

}
