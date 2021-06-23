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
package org.sonarsource.slang.checks.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.sonarsource.slang.api.ASTConverter;
import org.sonarsource.slang.api.FunctionDeclarationTree;
import org.sonarsource.slang.api.FunctionInvocationTree;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.NativeKind;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import org.sonarsource.slang.impl.FunctionInvocationTreeImpl;
import org.sonarsource.slang.impl.IdentifierTreeImpl;
import org.sonarsource.slang.impl.MemberSelectTreeImpl;
import org.sonarsource.slang.impl.NativeTreeImpl;
import org.sonarsource.slang.parser.SLangConverter;

import static org.sonarsource.slang.checks.utils.FunctionUtils.hasFunctionCallFullNameIgnoreCase;
import static org.sonarsource.slang.checks.utils.FunctionUtils.hasFunctionCallNameIgnoreCase;
import static org.assertj.core.api.Assertions.assertThat;

class FunctionUtilsTest {
  private class TypeNativeKind implements NativeKind {}

  private static TreeMetaData meta = null;
  private static IdentifierTree identifierTree = new IdentifierTreeImpl(meta, "function");
  private static List<Tree> args = new ArrayList<>();

  private static final ASTConverter CONVERTER = new SLangConverter();

  @Test
  void test_has_function_name_identifier() {
    FunctionInvocationTree tree = new FunctionInvocationTreeImpl(meta, identifierTree, args);
    assertThat(hasFunctionCallNameIgnoreCase(tree, "function")).isTrue();
    assertThat(hasFunctionCallNameIgnoreCase(tree, "FuNcTiOn")).isTrue();
    assertThat(hasFunctionCallNameIgnoreCase(tree, "mySuperFunction")).isFalse();
  }

  @Test
  void test_has_function_name_method_select() {
    Tree member = new IdentifierTreeImpl(meta, "A");
    Tree methodSelect = new MemberSelectTreeImpl(meta, member, identifierTree);
    FunctionInvocationTree tree = new FunctionInvocationTreeImpl(meta, methodSelect, args);
    assertThat(hasFunctionCallNameIgnoreCase(tree, "function")).isTrue();
    assertThat(hasFunctionCallNameIgnoreCase(tree, "A")).isFalse();
  }

  @Test
  void test_has_function_name_unknown() {
    Tree nativeNode = new NativeTreeImpl(meta, new TypeNativeKind(), null);
    FunctionInvocationTree tree = new FunctionInvocationTreeImpl(meta, nativeNode, args);
    assertThat(hasFunctionCallNameIgnoreCase(tree, "function")).isFalse();
  }

  @Test
  void test_has_function_full_name_identifier() {
    FunctionInvocationTree tree = new FunctionInvocationTreeImpl(meta, identifierTree, args);
    assertThat(hasFunctionCallFullNameIgnoreCase(tree, "function")).isTrue();
    assertThat(hasFunctionCallFullNameIgnoreCase(tree, "FuNcTioN")).isTrue();
    assertThat(hasFunctionCallFullNameIgnoreCase(tree, "mySuperFunction")).isFalse();
    assertThat(hasFunctionCallFullNameIgnoreCase(tree)).isFalse();
  }

  @Test
  void test_has_function_full_name_method_select() {
    IdentifierTree memberA = new IdentifierTreeImpl(meta, "A");
    IdentifierTree memberB = new IdentifierTreeImpl(meta, "B");
    Tree methodSelectAB = new MemberSelectTreeImpl(meta, memberA, memberB);
    Tree methodSelect = new MemberSelectTreeImpl(meta, methodSelectAB, identifierTree);
    FunctionInvocationTree tree = new FunctionInvocationTreeImpl(meta, methodSelect, args);

    assertThat(hasFunctionCallFullNameIgnoreCase(tree)).isFalse();
    assertThat(hasFunctionCallFullNameIgnoreCase(tree, "function")).isFalse();
    assertThat(hasFunctionCallFullNameIgnoreCase(tree, "A")).isFalse();
    assertThat(hasFunctionCallFullNameIgnoreCase(tree, "B")).isFalse();
    assertThat(hasFunctionCallFullNameIgnoreCase(tree, "A", "B")).isFalse();
    assertThat(hasFunctionCallFullNameIgnoreCase(tree, "A", "function")).isFalse();
    assertThat(hasFunctionCallFullNameIgnoreCase(tree, "B", "function")).isFalse();
    assertThat(hasFunctionCallFullNameIgnoreCase(tree, "A", "B", "function")).isTrue();
    assertThat(hasFunctionCallFullNameIgnoreCase(tree, "A", "B", "function" , "C")).isFalse();
  }

  @Test
  void test_has_function_full_name_unknown() {
    Tree nativeNode = new NativeTreeImpl(meta, new TypeNativeKind(), null);
    FunctionInvocationTree tree = new FunctionInvocationTreeImpl(meta, nativeNode, args);
    assertThat(hasFunctionCallFullNameIgnoreCase(tree, "function")).isFalse();
  }

  @Test
  void test_get_strings_tokens_returns_tokens() {
    String code = "void fun fooBar() {\n" +
      "    val a = \"one,two,three\"; \n"+
      "    foo(\"one,two$four\"); \n"+
      "}";

    FunctionDeclarationTree root = (FunctionDeclarationTree)CONVERTER.parse(code, null).children().get(0);

    Set<String> tokens = FunctionUtils.getStringsTokens(root, ",|\\$");

    assertThat(tokens).containsExactlyInAnyOrder("one", "two", "three", "four");
  }
}
