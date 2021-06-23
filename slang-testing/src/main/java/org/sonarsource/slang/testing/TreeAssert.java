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
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.sonarsource.slang.api.AssignmentExpressionTree;
import org.sonarsource.slang.api.BinaryExpressionTree;
import org.sonarsource.slang.api.BlockTree;
import org.sonarsource.slang.api.FunctionDeclarationTree;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.LiteralTree;
import org.sonarsource.slang.api.ParameterTree;
import org.sonarsource.slang.api.StringLiteralTree;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.UnaryExpressionTree;
import org.sonarsource.slang.utils.SyntacticEquivalence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarsource.slang.testing.RangeAssert.assertRange;
import static org.sonarsource.slang.visitors.TreePrinter.tree2string;

public class TreeAssert extends AbstractAssert<TreeAssert, Tree> {

  public TreeAssert(Tree actual) {
    super(actual, TreeAssert.class);
  }

  public TreeAssert isIdentifier(String expectedName) {
    isNotNull();
    isInstanceOf(IdentifierTree.class);
    IdentifierTree actualIdentifier = (IdentifierTree) actual;
    if (!Objects.equals(actualIdentifier.name(), expectedName)) {
      failWithMessage("Expected identifier's name to be <%s> but was <%s>", expectedName, actualIdentifier.name());
    }
    return this;
  }

  public TreeAssert hasTokens(String... tokens) {
    isNotNull();
    List<String> expected = actual.metaData().tokens().stream().map(Token::text).collect(Collectors.toList());
    Assertions.assertThat(Arrays.asList(tokens)).isEqualTo(expected);
    return this;
  }

  public TreeAssert hasParameterName(String expectedIdentifierName) {
    isNotNull();
    isInstanceOf(ParameterTree.class);
    ParameterTree actualParameter = (ParameterTree) actual;
    assertTree(actualParameter.identifier()).isIdentifier(expectedIdentifierName);
    return this;
  }

  public TreeAssert hasParameterNames(String... names) {
    isNotNull();
    isInstanceOf(FunctionDeclarationTree.class);
    FunctionDeclarationTree actualFunction = (FunctionDeclarationTree) actual;
    List<ParameterTree> actualParameters = actualFunction.formalParameters().stream()
      .filter(ParameterTree.class::isInstance)
      .map(ParameterTree.class::cast)
      .collect(Collectors.toList());
    if (actualParameters.size() != names.length) {
      failWithMessage("Expected to have <%s> parameters but found <%s>", names.length, actualParameters.size());
    }
    for (int i = 0; i < actualParameters.size(); i++) {
      ParameterTree tree = actualParameters.get(i);
      assertTree(tree).hasParameterName(names[i]);
    }
    return this;
  }

  public TreeAssert isLiteral(String expected) {
    isNotNull();
    isInstanceOf(LiteralTree.class);
    LiteralTree actualLiteral = (LiteralTree) actual;
    if (!Objects.equals(actualLiteral.value(), expected)) {
      failWithMessage("Expected literal value to be <%s> but was <%s>", expected, actualLiteral.value());
    }
    return this;
  }

  public TreeAssert isUnaryExpression(UnaryExpressionTree.Operator expectedOperator) {
    isNotNull();
    isInstanceOf(UnaryExpressionTree.class);
    UnaryExpressionTree actualUnary = (UnaryExpressionTree) actual;
    if (!Objects.equals(actualUnary.operator(), expectedOperator)) {
      failWithMessage("Expected operator to be <%s> but was <%s>", expectedOperator, actualUnary.operator());
    }
    return this;
  }

  public TreeAssert isStringLiteral(String expected) {
    isNotNull();
    isInstanceOf(StringLiteralTree.class);
    StringLiteralTree actualLiteral = (StringLiteralTree) actual;
    if (!Objects.equals(actualLiteral.content(), expected)) {
      failWithMessage("Expected string content to be <%s> but was <%s>", expected, actualLiteral.content());
    }
    return this;
  }

  public TreeAssert isBinaryExpression(BinaryExpressionTree.Operator expectedOperator) {
    isNotNull();
    isInstanceOf(BinaryExpressionTree.class);
    BinaryExpressionTree actualBinary = (BinaryExpressionTree) actual;
    if (!Objects.equals(actualBinary.operator(), expectedOperator)) {
      failWithMessage("Expected operator to be <%s> but was <%s>", expectedOperator, actualBinary.operator());
    }
    return this;
  }

  public TreeAssert isAssignmentExpression(AssignmentExpressionTree.Operator expectedOperator) {
    isNotNull();
    isInstanceOf(AssignmentExpressionTree.class);
    AssignmentExpressionTree actualBinary = (AssignmentExpressionTree) actual;
    if (!Objects.equals(actualBinary.operator(), expectedOperator)) {
      failWithMessage("Expected assignment operator to be <%s> but was <%s>", expectedOperator, actualBinary.operator());
    }
    return this;
  }

  public TreeAssert isBlock(Class... classes) {
    isNotNull();
    isInstanceOf(BlockTree.class);
    hasChildren(classes);
    return this;
  }

  public TreeAssert hasChildren(Class... classes) {
    hasChildren(classes.length);
    for (int i = 0; i < actual.children().size(); i++) {
      Tree tree = actual.children().get(i);
      if (!classes[i].isAssignableFrom(tree.getClass())) {
        failWithMessage("Expected to find instance of <%s> but was <%s>", classes[i], tree.getClass());
      }
    }
    return this;
  }

  private boolean checkHasDescendant(Tree expected) {
    isNotNull();
    return actual.descendants().anyMatch(descendant -> SyntacticEquivalence.areEquivalent(descendant, expected));
  }

  public TreeAssert hasDescendant(Tree expected) {
    if (!checkHasDescendant(expected)) {
      failWithMessage("Expected tree <%s> to be a descendant of <%s>", expected, actual);
    }
    return this;
  }

  public TreeAssert hasNotDescendant(Tree expected) {
    if (checkHasDescendant(expected)) {
      failWithMessage("Expected tree <%s> not to be a descendant of <%s>", expected, actual);
    }
    return this;
  }

  public TreeAssert hasChildren(int count) {
    isNotNull();
    if (actual.children().size() != count) {
      failWithMessage("Expected to have <%s> children elements but found <%s>", count, actual.children().size());
    }
    return this;
  }

  public TreeAssert hasTextRange(int startLine, int startLineOffset, int endLine, int endLineOffset) {
    isNotNull();
    assertRange(actual.metaData().textRange()).hasRange(startLine, startLineOffset, endLine, endLineOffset);
    return this;
  }

  public TreeAssert isEquivalentTo(Tree expected) {
    isNotNull();
    boolean equivalent = SyntacticEquivalence.areEquivalent(actual, expected);
    if (!equivalent) {
      assertThat(tree2string(actual)).isEqualTo(tree2string(expected));
      failWithMessage("Expected tree: <%s>\nbut was: <%s>", tree2string(expected), tree2string(actual));
    }
    return this;
  }

  public TreeAssert isNotEquivalentTo(Tree expected) {
    isNotNull();
    boolean equivalent = SyntacticEquivalence.areEquivalent(actual, expected);
    if (equivalent) {
      failWithMessage("Expected <%s> to not be equivalent to <%s>", actual, expected);
    }
    return this;
  }

  public static TreeAssert assertTree(Tree actual) {
    return new TreeAssert(actual);
  }

}
