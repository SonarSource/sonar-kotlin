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
package org.sonarsource.slang.visitors;

import org.sonarsource.slang.api.BinaryExpressionTree;
import org.sonarsource.slang.api.BinaryExpressionTree.Operator;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.LiteralTree;
import org.sonarsource.slang.api.NativeKind;
import org.sonarsource.slang.api.NativeTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.impl.BinaryExpressionTreeImpl;
import org.sonarsource.slang.impl.IdentifierTreeImpl;
import org.sonarsource.slang.impl.LiteralTreeImpl;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sonarsource.slang.impl.NativeTreeImpl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TreeVisitorTest {

  private class DummyNativeKind implements NativeKind {}

  private IdentifierTree var1 = new IdentifierTreeImpl(null, "var1");
  private LiteralTree number1 = new LiteralTreeImpl(null, "1");
  private BinaryExpressionTree binary = new BinaryExpressionTreeImpl(null, Operator.PLUS, null, var1, number1);
  private BinaryExpressionTree binminus = new BinaryExpressionTreeImpl(null, Operator.MINUS, null, var1, var1);

  private DummyNativeKind nkind = new DummyNativeKind();
  private NativeTree nativeNode = new NativeTreeImpl(null, nkind, Arrays.asList(binary, binminus));
  
  private TreeVisitor<TreeContext> visitor = new TreeVisitor<>();

  @Test
  void visitSimpleTree() {
    List<Tree> visited = new ArrayList<>();
    visitor.register(Tree.class, (ctx, tree) -> visited.add(tree));
    visitor.scan(new TreeContext(), binary);
    assertThat(visited).containsExactly(binary, var1, number1);
  }

  @Test
  void visitNativeTree() {
    List<Tree> visited = new ArrayList<>();
    visitor.register(Tree.class, (ctx, tree) -> visited.add(tree));
    visitor.scan(new TreeContext(), nativeNode);
    assertThat(visited).containsExactly(nativeNode, binary, var1, number1, binminus, var1, var1);
  }

  @Test
  void ancestors() {
    Map<Tree, List<Tree>> ancestors = new HashMap<>();
    visitor.register(Tree.class, (ctx, tree) -> ancestors.put(tree, new ArrayList<Tree>(ctx.ancestors())));
    visitor.scan(new TreeContext(), binary);
    assertThat(ancestors.get(binary)).isEmpty();
    assertThat(ancestors.get(var1)).containsExactly(binary);
    assertThat(ancestors.get(number1)).containsExactly(binary);
  }
}
