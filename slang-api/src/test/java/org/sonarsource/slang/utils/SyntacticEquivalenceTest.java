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
package org.sonarsource.slang.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.sonarsource.slang.api.AssignmentExpressionTree;
import org.sonarsource.slang.api.BinaryExpressionTree.Operator;
import org.sonarsource.slang.api.LoopTree;
import org.sonarsource.slang.api.NativeKind;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import org.sonarsource.slang.impl.IdentifierTreeImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarsource.slang.api.ModifierTree.Kind.PRIVATE;
import static org.sonarsource.slang.api.ModifierTree.Kind.PUBLIC;
import static org.sonarsource.slang.utils.SyntacticEquivalence.areEquivalent;
import static org.sonarsource.slang.utils.SyntacticEquivalence.findDuplicatedGroups;
import static org.sonarsource.slang.utils.TreeCreationUtils.assignment;
import static org.sonarsource.slang.utils.TreeCreationUtils.binary;
import static org.sonarsource.slang.utils.TreeCreationUtils.identifier;
import static org.sonarsource.slang.utils.TreeCreationUtils.integerLiteral;
import static org.sonarsource.slang.utils.TreeCreationUtils.literal;
import static org.sonarsource.slang.utils.TreeCreationUtils.loop;
import static org.sonarsource.slang.utils.TreeCreationUtils.placeHolderTree;
import static org.sonarsource.slang.utils.TreeCreationUtils.simpleModifier;
import static org.sonarsource.slang.utils.TreeCreationUtils.simpleNative;
import static org.sonarsource.slang.utils.TreeCreationUtils.value;
import static org.sonarsource.slang.utils.TreeCreationUtils.variable;

class SyntacticEquivalenceTest {
  private static NativeKind KIND = new NativeKind() {
    @Override
    public boolean equals(Object obj) {
      return this == obj;
    }
  };

  @Test
  void test_equivalence() {
    Tree literal1 = integerLiteral("1");
    Tree literal2 = integerLiteral("2");
    assertThat(areEquivalent((Tree) null, null)).isTrue();
    assertThat(areEquivalent(literal1, null)).isFalse();
    assertThat(areEquivalent(null, literal1)).isFalse();
    assertThat(areEquivalent(literal1, literal1)).isTrue();
    assertThat(areEquivalent(literal1, integerLiteral("1"))).isTrue();
    assertThat(areEquivalent(literal1, literal2)).isFalse();

    Tree identifierA = identifier("a");
    assertThat(areEquivalent(identifierA, identifierA)).isTrue();
    assertThat(areEquivalent(identifierA, identifier("a"))).isTrue();
    assertThat(areEquivalent(identifierA, identifier("b"))).isFalse();
    assertThat(areEquivalent(identifierA, literal1)).isFalse();

    Tree variableA = variable("a");
    assertThat(areEquivalent(variableA, variableA)).isTrue();
    assertThat(areEquivalent(variableA, variable("a"))).isTrue();
    assertThat(areEquivalent(variableA, variable("b"))).isFalse();

    Tree valueA = value("a");
    assertThat(areEquivalent(valueA, valueA)).isTrue();
    assertThat(areEquivalent(valueA, value("a"))).isTrue();
    assertThat(areEquivalent(valueA, value("b"))).isFalse();
    assertThat(areEquivalent(valueA, variableA)).isFalse();

    Tree binaryAEquals1 = binary(Operator.EQUAL_TO, identifierA, literal1);
    assertThat(areEquivalent(binaryAEquals1, binaryAEquals1)).isTrue();
    assertThat(areEquivalent(binaryAEquals1, binary(Operator.EQUAL_TO, identifierA, literal1))).isTrue();
    assertThat(areEquivalent(binaryAEquals1, binary(Operator.EQUAL_TO, identifierA, literal2))).isFalse();
    assertThat(areEquivalent(binaryAEquals1, binary(Operator.GREATER_THAN_OR_EQUAL_TO, identifierA, literal1))).isFalse();

    AssignmentExpressionTree.Operator plusEqualOperator = AssignmentExpressionTree.Operator.PLUS_EQUAL;
    Tree assignmentAPlusEqual1 = assignment(plusEqualOperator, identifierA, literal1);
    assertThat(areEquivalent(assignmentAPlusEqual1, assignmentAPlusEqual1)).isTrue();
    assertThat(areEquivalent(assignmentAPlusEqual1, assignment(plusEqualOperator, identifierA, literal1))).isTrue();
    assertThat(areEquivalent(assignmentAPlusEqual1, assignment(plusEqualOperator, identifierA, literal2))).isFalse();
    assertThat(areEquivalent(assignmentAPlusEqual1, assignment(AssignmentExpressionTree.Operator.EQUAL, identifierA, literal1))).isFalse();
    assertThat(areEquivalent(assignmentAPlusEqual1, binaryAEquals1)).isFalse();

    Tree native1 = simpleNative(KIND, Collections.singletonList("@a"), Collections.emptyList());
    assertThat(areEquivalent(native1, native1)).isTrue();
    assertThat(areEquivalent(native1, simpleNative(KIND, Collections.singletonList("@a"), Collections.emptyList()))).isTrue();
    assertThat(areEquivalent(native1, simpleNative(KIND, Arrays.asList("@a", "@b"), Collections.emptyList()))).isFalse();
    assertThat(areEquivalent(native1, simpleNative(KIND, Collections.singletonList("1"), Collections.singletonList(literal1)))).isFalse();
    assertThat(areEquivalent(native1, simpleNative(null, Collections.singletonList("@a"), Collections.emptyList()))).isFalse();
    assertThat(areEquivalent(native1, literal1)).isFalse();

    Tree native2 = simpleNative(KIND, Collections.singletonList("1"), Collections.singletonList(literal1));
    assertThat(areEquivalent(native2, native1)).isFalse();
    assertThat(areEquivalent(native2, native2)).isTrue();

    Tree modifier1 = simpleModifier(PRIVATE);
    assertThat(areEquivalent(modifier1, modifier1)).isTrue();
    assertThat(areEquivalent(modifier1, simpleModifier(PRIVATE))).isTrue();
    assertThat(areEquivalent(modifier1, simpleModifier(PUBLIC))).isFalse();
    assertThat(areEquivalent(modifier1, literal1)).isFalse();

    Tree placeHolder1 = placeHolderTree();
    Tree placeHolder2 = placeHolderTree();
    assertThat(areEquivalent(placeHolder1, identifierA)).isFalse();
    assertThat(areEquivalent(placeHolder1, identifier("_"))).isFalse();
    assertThat(areEquivalent(placeHolder1, placeHolder2)).isTrue();
  }

  @Test
  void test_equivalence_list() {
    List<Tree> list1 = Arrays.asList(identifier("a"), integerLiteral("2"));
    List<Tree> list2 = Arrays.asList(identifier("a"), integerLiteral("2"));
    List<Tree> list3 = Arrays.asList(identifier("a"), integerLiteral("3"));
    List<Tree> list4 = Collections.singletonList(identifier("a"));

    assertThat(areEquivalent((List<Tree>) null, null)).isTrue();
    assertThat(areEquivalent(list1, null)).isFalse();
    assertThat(areEquivalent(null, list1)).isFalse();
    assertThat(areEquivalent(list1, list1)).isTrue();
    assertThat(areEquivalent(list1, list2)).isTrue();
    assertThat(areEquivalent(list1, list3)).isFalse();
    assertThat(areEquivalent(list1, list4)).isFalse();
  }

  @Test
  void duplicateGroups() {
    Tree a1 = identifier("a");
    Tree a2 = identifier("a");
    Tree a3 = a1;
    Tree b1 = identifier("b");
    assertThat(findDuplicatedGroups(Arrays.asList(a1, b1, a2, a3))).containsExactly(Arrays.asList(a1, a2, a3));
    assertThat(findDuplicatedGroups(Arrays.asList(a1, b1, null))).isEmpty();
  }

  @Test
  void loops() {
    Tree condition1 = literal("true");
    Tree condition2 = literal("false");
    Tree body1 = integerLiteral("1");
    Tree body2 = integerLiteral("2");

    LoopTree loop1 = loop(condition1, body1, LoopTree.LoopKind.WHILE, "while");

    assertThat(areEquivalent(loop1, loop1)).isTrue();
    assertThat(areEquivalent(loop1, loop(condition1, body1, LoopTree.LoopKind.WHILE, "while"))).isTrue();
    assertThat(areEquivalent(loop1, loop(condition2, body1, LoopTree.LoopKind.WHILE, "while"))).isFalse();
    assertThat(areEquivalent(loop1, loop(condition1, body2, LoopTree.LoopKind.WHILE, "while"))).isFalse();
    assertThat(areEquivalent(loop1, loop(condition1, body1, LoopTree.LoopKind.FOR, "while"))).isFalse();
    assertThat(areEquivalent(loop1, loop(condition1, body1, LoopTree.LoopKind.WHILE, "until"))).isFalse();
  }

  @Test
  void test_unique_identifier_equivalence() {
    IdentifierTreeImpl id = new CustomIdentifierTreeImpl(null, "abc");
    assertThat(areEquivalent(id, new IdentifierTreeImpl(null, "abc"))).isFalse();
    assertThat(areEquivalent(id, id)).isTrue();
    assertThat(areEquivalent(id, new CustomIdentifierTreeImpl(null, "abc"))).isTrue();
    assertThat(areEquivalent(id, new CustomIdentifierTreeImpl(null, "ABc"))).isTrue();
    assertThat(areEquivalent(id, new CustomIdentifierTreeImpl(null, "ABC"))).isTrue();
    assertThat(areEquivalent(id, new CustomIdentifierTreeImpl(null, "a"))).isFalse();
  }

  class CustomIdentifierTreeImpl extends IdentifierTreeImpl {
    CustomIdentifierTreeImpl(TreeMetaData metaData, String name) {
      super(metaData, name);
    }

    @Override
    public String identifier() {
      return name().toUpperCase(Locale.ENGLISH);
    }
  }

}
