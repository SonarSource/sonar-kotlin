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

import org.sonarsource.slang.api.NativeKind;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import org.sonarsource.slang.api.UnaryExpressionTree;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.sonarsource.slang.utils.SyntacticEquivalence.areEquivalent;
import static org.assertj.core.api.Assertions.assertThat;

class UnaryExpressionTreeImplTest  {

  private class TypeNativeKind implements NativeKind {}

  @Test
  void test() {
    TreeMetaData meta = null;
    Tree condition = new IdentifierTreeImpl(meta, "x");
    Tree negCondition = new UnaryExpressionTreeImpl(meta, UnaryExpressionTree.Operator.NEGATE, condition);
    Tree negConditionCopy = new UnaryExpressionTreeImpl(meta, UnaryExpressionTree.Operator.NEGATE, condition);
    Tree nativeTree = new NativeTreeImpl(meta, new TypeNativeKind(), Arrays.asList(condition));
    Tree negNative = new UnaryExpressionTreeImpl(meta, UnaryExpressionTree.Operator.NEGATE, nativeTree);

    assertThat(negCondition.children()).containsExactly(condition);
    assertThat(areEquivalent(negCondition, negConditionCopy)).isTrue();
    assertThat(areEquivalent(negNative, negCondition)).isFalse();
  }

}
