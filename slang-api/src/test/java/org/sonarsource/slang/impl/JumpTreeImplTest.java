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

import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.JumpTree;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.TreeMetaData;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;

import static org.sonarsource.slang.utils.SyntacticEquivalence.areEquivalent;
import static org.assertj.core.api.Assertions.assertThat;

class JumpTreeImplTest {

  @Test
  void test_break() {
    JumpTree breakWithLabel = getJumpTree(JumpTree.JumpKind.BREAK, "foo");
    assertThat(breakWithLabel.children()).hasSize(1);
    assertThat(breakWithLabel.label().name()).isEqualTo("foo");
    assertThat(breakWithLabel.kind()).isEqualTo(JumpTree.JumpKind.BREAK);

    JumpTree breakWithoutLabel = getJumpTree(JumpTree.JumpKind.BREAK, null);
    assertThat(breakWithoutLabel.children()).isEmpty();
    assertThat(breakWithoutLabel.label()).isNull();
    assertThat(breakWithLabel.kind()).isEqualTo(JumpTree.JumpKind.BREAK);
  }

  @Test
  void test_continue() {
    JumpTree continueWithLabel = getJumpTree(JumpTree.JumpKind.CONTINUE, "foo");
    assertThat(continueWithLabel.children()).hasSize(1);
    assertThat(continueWithLabel.label().name()).isEqualTo("foo");
    assertThat(continueWithLabel.kind()).isEqualTo(JumpTree.JumpKind.CONTINUE);

    JumpTree continueWithoutLabel = getJumpTree(JumpTree.JumpKind.CONTINUE, null);
    assertThat(continueWithoutLabel.children()).isEmpty();
    assertThat(continueWithoutLabel.label()).isNull();
    assertThat(continueWithoutLabel.kind()).isEqualTo(JumpTree.JumpKind.CONTINUE);
  }

  @Test
  void test_syntactic_equivalence() {
    JumpTree jumpTreeBreak = getJumpTree(JumpTree.JumpKind.BREAK, "foo");
    JumpTree jumpTreeContinue = getJumpTree(JumpTree.JumpKind.CONTINUE, "foo");
    assertThat(areEquivalent(jumpTreeBreak, getJumpTree(JumpTree.JumpKind.BREAK, "foo"))).isTrue();
    assertThat(areEquivalent(jumpTreeContinue, getJumpTree(JumpTree.JumpKind.CONTINUE, "foo"))).isTrue();

    assertThat(areEquivalent(jumpTreeBreak, jumpTreeContinue)).isFalse();

    assertThat(areEquivalent(jumpTreeBreak, getJumpTree(JumpTree.JumpKind.BREAK, "bar"))).isFalse();
    assertThat(areEquivalent(jumpTreeContinue, getJumpTree(JumpTree.JumpKind.CONTINUE, "bar"))).isFalse();

    assertThat(areEquivalent(jumpTreeBreak, getJumpTree(JumpTree.JumpKind.BREAK, null))).isFalse();
    assertThat(areEquivalent(jumpTreeContinue, getJumpTree(JumpTree.JumpKind.CONTINUE, null))).isFalse();
  }

  private static JumpTree getJumpTree(JumpTree.JumpKind kind, @Nullable String labelText) {
    TreeMetaData meta = null;
    String keywordText = kind == JumpTree.JumpKind.BREAK ? "break" : "continue";
    TokenImpl keyword = new TokenImpl(new TextRangeImpl(1, 0, 1, keywordText.length()), keywordText, Token.Type.KEYWORD);
    IdentifierTree label = null;
    if (labelText != null) {
      label = new IdentifierTreeImpl(meta, labelText);
    }
    return new JumpTreeImpl(meta, keyword, kind, label);
  }

}
