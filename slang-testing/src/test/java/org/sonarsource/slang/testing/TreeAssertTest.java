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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.sonarsource.slang.api.Annotation;
import org.sonarsource.slang.api.AssignmentExpressionTree;
import org.sonarsource.slang.api.BinaryExpressionTree;
import org.sonarsource.slang.api.Comment;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.LiteralTree;
import org.sonarsource.slang.api.NativeTree;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.TreeMetaData;
import org.sonarsource.slang.impl.AssignmentExpressionTreeImpl;
import org.sonarsource.slang.impl.BinaryExpressionTreeImpl;
import org.sonarsource.slang.impl.BlockTreeImpl;
import org.sonarsource.slang.impl.FunctionDeclarationTreeImpl;
import org.sonarsource.slang.impl.IdentifierTreeImpl;
import org.sonarsource.slang.impl.LiteralTreeImpl;
import org.sonarsource.slang.impl.NativeTreeImpl;
import org.sonarsource.slang.impl.ParameterTreeImpl;
import org.sonarsource.slang.impl.StringLiteralTreeImpl;
import org.sonarsource.slang.impl.TextRangeImpl;
import org.sonarsource.slang.impl.TokenImpl;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.sonarsource.slang.testing.TreeAssert.assertTree;

class TreeAssertTest {

  private static final IdentifierTreeImpl IDENTIFIER_ABC = new IdentifierTreeImpl(null, "abc");
  private static final StringLiteralTreeImpl STRING_LITERAL_STR = new StringLiteralTreeImpl(null, "\"str\"");
  private static final LiteralTreeImpl LITERAL_42 = new LiteralTreeImpl(null, "42");
  public static final AssignmentExpressionTreeImpl ASSIGN_42_TO_ABC = new AssignmentExpressionTreeImpl(null, AssignmentExpressionTree.Operator.EQUAL, IDENTIFIER_ABC, LITERAL_42);
  private static final BinaryExpressionTreeImpl ABC_PLUS_42 = new BinaryExpressionTreeImpl(null, BinaryExpressionTree.Operator.PLUS, null, IDENTIFIER_ABC, LITERAL_42);
  private static final BinaryExpressionTreeImpl ABC_PLUS_ABC_PLUS_42 = new BinaryExpressionTreeImpl(null, BinaryExpressionTree.Operator.PLUS, null, IDENTIFIER_ABC, ABC_PLUS_42);
  private static final ParameterTreeImpl PARAMETER_ABC = new ParameterTreeImpl(null, IDENTIFIER_ABC, null);
  private static final FunctionDeclarationTreeImpl FUNCTION_ABC = new FunctionDeclarationTreeImpl(null, Collections.emptyList(), false, null, null,
    singletonList(PARAMETER_ABC), null, emptyList());

  @Test
  void identifier_ok() {
    assertTree(IDENTIFIER_ABC).isIdentifier("abc");
  }

  @Test
  void has_tokens() {
    Token token1 = new TokenImpl(new TextRangeImpl(1,0,1,1), "a", Token.Type.OTHER);
    Token token2 = new TokenImpl(new TextRangeImpl(1,2,1,3), "b", Token.Type.OTHER);
    TextRangeImpl textRange = new TextRangeImpl(token1.textRange().start(), token2.textRange().end());
    NativeTree nativeTree = new NativeTreeImpl(meta(textRange, token1, token2), null, Collections.emptyList());
    assertTree(nativeTree).hasTokens("a", "b");
  }

  @Test
  void identifier_with_wrong_name() {
    assertThrows(AssertionError.class,
      () -> assertTree(IDENTIFIER_ABC).isIdentifier("xxx"));
  }

  @Test
  void not_an_identifier() {
    assertThrows(AssertionError.class,
      () -> assertTree(LITERAL_42).isIdentifier("abc"));
  }

  @Test
  void parameter_has_identifier() {
    assertTree(PARAMETER_ABC).hasParameterName("abc");
  }

  @Test
  void parameter_does_not_have_identifier() {
    assertThrows(AssertionError.class,
      () -> assertTree(PARAMETER_ABC).hasParameterName("xxx"));
  }

  @Test
  void function_has_parameters() {
    assertTree(FUNCTION_ABC).hasParameterNames("abc");
  }

  @Test
  void function_does_not_have_two_parameters() {
    assertThrows(AssertionError.class,
      () -> assertTree(FUNCTION_ABC).hasParameterNames("abc", "xxx"));
  }

  @Test
  void function_does_not_have_parameters() {
    assertThrows(AssertionError.class,
      () -> assertTree(FUNCTION_ABC).hasParameterNames("xxx"));
  }

  @Test
  void literal_ok() {
    assertTree(LITERAL_42).isLiteral("42");
  }

  @Test
  void literal_with_wrong_value() {
    assertThrows(AssertionError.class,
      () -> assertTree(LITERAL_42).isLiteral("123"));
  }

  @Test
  void not_a_literal() {
    assertThrows(AssertionError.class,
      () -> assertTree(new LiteralTreeImpl(null, "42")).isLiteral("123"));
  }

  @Test
  void string_literal_ok() {
    assertTree(STRING_LITERAL_STR).isStringLiteral("str");
  }

  @Test
  void string_literal_failure() {
    assertThrows(AssertionError.class,
      () -> assertTree(STRING_LITERAL_STR).isStringLiteral("abc"));
  }

  @Test
  void not_a_string_literal() {
    assertThrows(AssertionError.class,
      () -> assertTree(IDENTIFIER_ABC).isStringLiteral("abc"));
  }

  @Test
  void binary_ok() {
    assertTree(ABC_PLUS_42).isBinaryExpression(BinaryExpressionTree.Operator.PLUS);
  }

  @Test
  void binary_with_wrong_operator() {
    assertThrows(AssertionError.class,
      () -> assertTree(ABC_PLUS_42).isBinaryExpression(BinaryExpressionTree.Operator.MINUS));
  }

  @Test
  void not_a_binary() {
    assertThrows(AssertionError.class,
      () -> assertTree(LITERAL_42).isBinaryExpression(BinaryExpressionTree.Operator.PLUS));
  }

  @Test
  void assignment_ok() {
    assertTree(ASSIGN_42_TO_ABC).isAssignmentExpression(AssignmentExpressionTree.Operator.EQUAL);
  }

  @Test
  void assignment_with_wrong_operator() {
    assertThrows(AssertionError.class,
      () -> assertTree(ASSIGN_42_TO_ABC).isAssignmentExpression(AssignmentExpressionTree.Operator.PLUS_EQUAL));
  }

  @Test
  void not_an_assignment() {
    assertThrows(AssertionError.class,
      () -> assertTree(LITERAL_42).isAssignmentExpression(AssignmentExpressionTree.Operator.EQUAL));
  }

  @Test
  void empty_block() {
    assertTree(new BlockTreeImpl(null, Collections.emptyList())).isBlock();
  }

  @Test
  void non_empty_block() {
    assertTree(new BlockTreeImpl(null, singletonList(LITERAL_42))).isBlock(LiteralTree.class);
  }

  @Test
  void block_with_wrong_child_class() {
    assertThrows(AssertionError.class,
      () -> assertTree(new BlockTreeImpl(null, singletonList(LITERAL_42))).isBlock(IdentifierTree.class));
  }

  @Test
  void block_with_too_many_children() {
    assertThrows(AssertionError.class,
      () -> assertTree(new BlockTreeImpl(null, singletonList(LITERAL_42))).isBlock());
  }

  @Test
  void not_a_block() {
    assertThrows(AssertionError.class,
      () -> assertTree(LITERAL_42).isBlock(LiteralTree.class));
  }

  @Test
  void text_range() {
    assertTree(new IdentifierTreeImpl(meta(new TextRangeImpl(1, 2, 3, 4)), "a")).hasTextRange(1, 2, 3, 4);
  }

  @Test
  void wrong_text_range() {
    assertThrows(AssertionError.class,
      () -> assertTree(new IdentifierTreeImpl(meta(new TextRangeImpl(1, 2, 3, 4)), "a")).hasTextRange(1, 2, 3, 42));
  }

  @Test
  void equivalent_ok() {
    assertTree(LITERAL_42).isEquivalentTo(new LiteralTreeImpl(null, "42"));
  }

  @Test
  void equivalent_failure() {
    assertThrows(AssertionError.class,
      () -> assertTree(LITERAL_42).isEquivalentTo(new LiteralTreeImpl(null, "43")));
  }

  @Test
  void notequivalent_ok() {
    assertTree(LITERAL_42).isNotEquivalentTo(new LiteralTreeImpl(null, "43"));
  }

  @Test
  void notequivalent_failure() {
    assertThrows(AssertionError.class,
      () -> assertTree(LITERAL_42).isNotEquivalentTo(new LiteralTreeImpl(null, "42")));
  }

  @Test
  void hasdescendant_ok() {
    assertTree(ABC_PLUS_ABC_PLUS_42).hasDescendant(new LiteralTreeImpl(null, "42"));
  }

  @Test
  void hasdescendant_failure() {
    assertThrows(AssertionError.class,
      () -> assertTree(ABC_PLUS_ABC_PLUS_42).hasDescendant(new LiteralTreeImpl(null, "43")));
  }

  @Test
  void hasnotdescendant_ok() {
    assertTree(ABC_PLUS_ABC_PLUS_42).hasNotDescendant(new LiteralTreeImpl(null, "43"));
  }

  @Test
  void hasnotdescendant_failure() {
    assertThrows(AssertionError.class,
      () -> assertTree(ABC_PLUS_ABC_PLUS_42).hasNotDescendant(new LiteralTreeImpl(null, "42")));
  }

  private TreeMetaData meta(TextRange textRange, Token... tokens) {
    return new TreeMetaData() {
      @Override
      public TextRange textRange() {
        return textRange;
      }

      @Override
      public List<Comment> commentsInside() {
        return null;
      }

      @Override
      public List<Annotation> annotations() {
        return Collections.emptyList();
      }

      @Override
      public List<Token> tokens() {
        return Arrays.asList(tokens);
      }

      @Override
      public Set<Integer> linesOfCode() {
        return Collections.emptySet();
      }
    };
  }
}
