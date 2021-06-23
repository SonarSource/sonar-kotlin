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
import java.util.stream.Collectors;
import org.sonarsource.slang.api.AssignmentExpressionTree;
import org.sonarsource.slang.api.BinaryExpressionTree;
import org.sonarsource.slang.api.BlockTree;
import org.sonarsource.slang.api.FunctionDeclarationTree;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.IntegerLiteralTree;
import org.sonarsource.slang.api.LiteralTree;
import org.sonarsource.slang.api.LoopTree;
import org.sonarsource.slang.api.ModifierTree;
import org.sonarsource.slang.api.NativeKind;
import org.sonarsource.slang.api.NativeTree;
import org.sonarsource.slang.api.PlaceHolderTree;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.TopLevelTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import org.sonarsource.slang.api.VariableDeclarationTree;
import org.sonarsource.slang.impl.AssignmentExpressionTreeImpl;
import org.sonarsource.slang.impl.BinaryExpressionTreeImpl;
import org.sonarsource.slang.impl.BlockTreeImpl;
import org.sonarsource.slang.impl.FunctionDeclarationTreeImpl;
import org.sonarsource.slang.impl.IdentifierTreeImpl;
import org.sonarsource.slang.impl.IntegerLiteralTreeImpl;
import org.sonarsource.slang.impl.LiteralTreeImpl;
import org.sonarsource.slang.impl.LoopTreeImpl;
import org.sonarsource.slang.impl.ModifierTreeImpl;
import org.sonarsource.slang.impl.NativeTreeImpl;
import org.sonarsource.slang.impl.PlaceHolderTreeImpl;
import org.sonarsource.slang.impl.TextRangeImpl;
import org.sonarsource.slang.impl.TokenImpl;
import org.sonarsource.slang.impl.TopLevelTreeImpl;
import org.sonarsource.slang.impl.VariableDeclarationTreeImpl;

import static java.util.Collections.emptyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TreeCreationUtils {
  private TreeCreationUtils() {
  }

  public static LoopTree loop(Tree condition, Tree body, LoopTree.LoopKind kind, String keyword) {
    Token tokenKeyword = new TokenImpl(null, keyword, Token.Type.KEYWORD);
    return new LoopTreeImpl(null, condition, body, kind, tokenKeyword);
  }

  public static IntegerLiteralTree integerLiteral(String value) {
    return new IntegerLiteralTreeImpl(null, value);
  }

  public static PlaceHolderTree placeHolderTree() {
    return new PlaceHolderTreeImpl(null, null);
  }

  public static IntegerLiteralTree integerLiteral(String value, TextRange textRange, String ... tokens) {
    return new IntegerLiteralTreeImpl(metaData(textRange, tokens), value);
  }

  public static LiteralTree literal(String value) {
    return new LiteralTreeImpl(null, value);
  }

  public static IdentifierTree identifier(String name) {
    return new IdentifierTreeImpl(null, name);
  }

  public static IdentifierTree identifier(String name, TextRange textRange, String ... tokens) {
    return new IdentifierTreeImpl(metaData(textRange, tokens), name);
  }

  public static VariableDeclarationTree variable(String name) {
    return new VariableDeclarationTreeImpl(null, identifier(name), null, null, false);
  }

  public static VariableDeclarationTree value(String name) {
    return new VariableDeclarationTreeImpl(null, identifier(name), null, null, true);
  }

  public static BinaryExpressionTree binary(BinaryExpressionTree.Operator operator, Tree leftOperand, Tree rightOperand) {
    return new BinaryExpressionTreeImpl(null, operator, null, leftOperand, rightOperand);
  }

  public static BinaryExpressionTree binary(BinaryExpressionTree.Operator operator, Tree leftOperand, Tree rightOperand, TextRange textRange, String ... tokens) {
    return new BinaryExpressionTreeImpl(metaData(textRange, tokens), operator, new TokenImpl(new TextRangeImpl(1,0,1,0), operator.toString(), null), leftOperand, rightOperand);
  }

  public static AssignmentExpressionTree assignment(Tree leftOperand, Tree rightOperand) {
    return assignment(AssignmentExpressionTree.Operator.EQUAL, leftOperand, rightOperand);
  }

  public static AssignmentExpressionTree assignment(Tree leftOperand, Tree rightOperand, TextRange textRange, String ... tokens) {
    return assignment(AssignmentExpressionTree.Operator.EQUAL, leftOperand, rightOperand, textRange, tokens);
  }

  public static BlockTree block(List<Tree> body) {
    return new BlockTreeImpl(null, body);
  }

  public static BlockTree block(List<Tree> body, TextRange textRange, String ... tokens) {
    return new BlockTreeImpl(metaData(textRange, tokens), body);
  }

  public static FunctionDeclarationTree simpleFunction(IdentifierTree name, BlockTree body) {
    return new FunctionDeclarationTreeImpl(null, Collections.emptyList(), false, null, name, Collections.emptyList(), body, emptyList());
  }

  public static AssignmentExpressionTree assignment(AssignmentExpressionTree.Operator operator, Tree leftOperand, Tree rightOperand) {
    return new AssignmentExpressionTreeImpl(null, operator, leftOperand, rightOperand);
  }

  public static AssignmentExpressionTree assignment(AssignmentExpressionTree.Operator operator, Tree leftOperand, Tree rightOperand, TextRange textRange, String ... tokens) {
    return new AssignmentExpressionTreeImpl(metaData(textRange, tokens), operator, leftOperand, rightOperand);
  }

  public static NativeTree simpleNative(NativeKind kind, List<Tree> children) {
    return new NativeTreeImpl(null, kind, children);
  }

  public static NativeTree simpleNative(NativeKind kind, List<String> tokens, List<Tree> children) {
    return new NativeTreeImpl(metaData(tokens), kind, children);
  }

  public static ModifierTree simpleModifier(ModifierTree.Kind kind) {
    return new ModifierTreeImpl(null, kind);
  }

  public static TopLevelTree topLevel(List<Tree> declarations) {
    return new TopLevelTreeImpl(null, declarations, null);
  }

  private static TreeMetaData metaData(List<String> tokens) {
    TreeMetaData metaData = mock(TreeMetaData.class);
    mockTokens(metaData, tokens);
    return metaData;
  }

  private static TreeMetaData metaData(TextRange textRange, String ... tokens) {
    TreeMetaData metaData = mock(TreeMetaData.class);
    mockTokens(metaData, Arrays.asList(tokens));
    mockTextRange(metaData, textRange);
    return metaData;
  }

  private static void mockTokens(TreeMetaData metaData, List<String> tokens) {
    when(metaData.tokens()).thenReturn(tokens.stream()
        .map(text -> new TokenImpl(null, text, null))
        .collect(Collectors.toList()));
  }

  private static void mockTextRange(TreeMetaData metaData, TextRange textRange) {
    when(metaData.textRange()).thenReturn(textRange);
  }
}
