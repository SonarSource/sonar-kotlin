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
package org.sonarsource.slang.plugin.converter;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.config.Configuration;
import org.sonar.api.internal.google.common.annotations.VisibleForTesting;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonarsource.slang.api.ASTConverter;
import org.sonarsource.slang.api.Comment;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.TextPointer;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.TopLevelTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import org.sonarsource.slang.impl.LiteralTreeImpl;
import org.sonarsource.slang.impl.NativeTreeImpl;
import org.sonarsource.slang.impl.PlaceHolderTreeImpl;
import org.sonarsource.slang.impl.TextPointerImpl;

public class ASTConverterValidation implements ASTConverter {

  private static final Logger LOG = Loggers.get(ASTConverterValidation.class);

  private static final Pattern PUNCTUATOR_PATTERN = Pattern.compile("[^0-9A-Za-z]++");

  private static final Set<String> ALLOWED_MISPLACED_TOKENS_OUTSIDE_PARENT_RANGE = new HashSet<>(Collections.singleton("implicit"));

  private final ASTConverter wrapped;

  private final Map<String, String> firstErrorOfEachKind = new TreeMap<>();

  private final ValidationMode mode;

  public enum ValidationMode {
    THROW_EXCEPTION,
    LOG_ERROR
  }

  @Nullable
  private String currentFile = null;

  public ASTConverterValidation(ASTConverter wrapped, ValidationMode mode) {
    this.wrapped = wrapped;
    this.mode = mode;
  }

  public static ASTConverter wrap(ASTConverter converter, Configuration configuration) {
    String mode = configuration.get("sonar.slang.converter.validation").orElse(null);
    if (mode == null) {
      return converter;
    } else if (mode.equals("throw")) {
      return new ASTConverterValidation(converter, ValidationMode.THROW_EXCEPTION);
    } else if (mode.equals("log")) {
      return new ASTConverterValidation(converter, ValidationMode.LOG_ERROR);
    } else {
      throw new IllegalStateException("Unsupported mode: " + mode);
    }
  }

  @Override
  public Tree parse(String content) {
    return parse(content, null);
  }

  @Override
  public Tree parse(String content, @Nullable String currentFile) {
    this.currentFile = currentFile;
    Tree tree = wrapped.parse(content, currentFile);
    assertTreeIsValid(tree);
    assertTokensMatchSourceCode(tree, content);
    return tree;
  }

  @Override
  public void terminate() {
    List<String> errors = errors();
    if (!errors.isEmpty()) {
      LOG.error("AST Converter Validation detected " + errors.size() + " errors:\n  [AST ERROR] "
        + String.join("\n  [AST ERROR] ", errors));
    }
    wrapped.terminate();
  }

  @VisibleForTesting
  ValidationMode mode() {
    return mode;
  }

  @VisibleForTesting
  List<String> errors() {
    return firstErrorOfEachKind.entrySet().stream()
      .map(entry -> entry.getKey() + entry.getValue())
      .collect(Collectors.toList());
  }

  private void raiseError(String messageKey, String messageDetails, TextPointer position) {
    if (mode == ValidationMode.THROW_EXCEPTION) {
      throw new IllegalStateException("ASTConverterValidationException: " + messageKey + messageDetails +
        " at  " + position.line() + ":" + position.lineOffset());
    } else {
      String positionDetails = String.format(" (line: %d, column: %d)", position.line(), (position.lineOffset() + 1));
      if (currentFile != null) {
        positionDetails += " in file: " + currentFile;
      }
      firstErrorOfEachKind.putIfAbsent(messageKey, messageDetails + positionDetails);
    }
  }

  private static String kind(Tree tree) {
    return tree.getClass().getSimpleName();
  }

  private void assertTreeIsValid(Tree tree) {
    assertTextRangeIsValid(tree);
    assertTreeHasAtLeastOneToken(tree);
    assertTokensAndChildTokens(tree);
    for (Tree child : tree.children()) {
      if (child == null) {
        raiseError(kind(tree) + " has a null child", "", tree.textRange().start());
      } else if (child.metaData() == null) {
        raiseError(kind(child) + " metaData is null", "", tree.textRange().start());
      } else {
        assertTreeIsValid(child);
      }
    }
  }

  private void assertTextRangeIsValid(Tree tree) {
    TextPointer start = tree.metaData().textRange().start();
    TextPointer end = tree.metaData().textRange().end();

    boolean startOffsetAfterEndOffset =  !(tree instanceof TopLevelTree) &&
      start.line() == end.line() &&
      start.lineOffset() >= end.lineOffset();

    if (start.line() <= 0 || end.line() <= 0 ||
      start.line() > end.line() ||
      start.lineOffset() < 0 || end.lineOffset() < 0 ||
      startOffsetAfterEndOffset) {
      raiseError(kind(tree) + " invalid range ", tree.metaData().textRange().toString(), start);
    }
  }

  private void assertTreeHasAtLeastOneToken(Tree tree) {
    if (!(tree instanceof TopLevelTree) && tree.metaData().tokens().isEmpty()) {
      raiseError(kind(tree) + " has no token", "", tree.textRange().start());
    }
  }

  private void assertTokensMatchSourceCode(Tree tree, String code) {
    CodeFormToken codeFormToken = new CodeFormToken(tree.metaData());
    codeFormToken.assertEqualTo(code);
  }

  private void assertTokensAndChildTokens(Tree tree) {
    assertTokensAreInsideRange(tree);
    Set<Token> parentTokens = new HashSet<>(tree.metaData().tokens());
    Map<Token, Tree> childByToken = new HashMap<>();
    for (Tree child : tree.children()) {
      if (child != null && child.metaData() != null && !isAllowedMisplacedTree(child)) {
        assertChildRangeIsInsideParentRange(tree, child);
        assertChildTokens(parentTokens, childByToken, tree, child);
      }
    }
    parentTokens.removeAll(childByToken.keySet());
    assertUnexpectedTokenKind(tree, parentTokens);
  }

  private static boolean isAllowedMisplacedTree(Tree tree) {
    List<Token> tokens = tree.metaData().tokens();
    return tokens.size() == 1 && ALLOWED_MISPLACED_TOKENS_OUTSIDE_PARENT_RANGE.contains(tokens.get(0).text());
  }

  private void assertUnexpectedTokenKind(Tree tree, Set<Token> tokens) {
    if (tree instanceof NativeTreeImpl || tree instanceof LiteralTreeImpl || tree instanceof PlaceHolderTreeImpl) {
      return;
    }
    List<Token> unexpectedTokens;
    if (tree instanceof IdentifierTree) {
      unexpectedTokens = tokens.stream()
        .filter(token -> token.type() == Token.Type.KEYWORD || token.type() == Token.Type.STRING_LITERAL)
        .collect(Collectors.toList());
    } else {
      unexpectedTokens = tokens.stream()
        .filter(token -> token.type() != Token.Type.KEYWORD)
        .filter(token -> !PUNCTUATOR_PATTERN.matcher(token.text()).matches())
        .filter(token -> !ALLOWED_MISPLACED_TOKENS_OUTSIDE_PARENT_RANGE.contains(token.text()))
        .collect(Collectors.toList());
    }
    if (!unexpectedTokens.isEmpty()) {
      String tokenList = unexpectedTokens.stream()
        .sorted(Comparator.comparing(token -> token.textRange().start()))
        .map(Token::text)
        .collect(Collectors.joining("', '"));
      raiseError("Unexpected tokens in " + kind(tree), ": '" + tokenList + "'", tree.textRange().start());
    }
  }

  private void assertTokensAreInsideRange(Tree tree) {
    TextRange parentRange = tree.metaData().textRange();
    tree.metaData().tokens().stream()
      .filter(token -> !ALLOWED_MISPLACED_TOKENS_OUTSIDE_PARENT_RANGE.contains(token.text()))
      .filter(token -> !token.textRange().isInside(parentRange))
      .findFirst()
      .ifPresent(token -> raiseError(
        kind(tree) + " contains a token outside its range",
        " range: " + parentRange + " tokenRange: " + token.textRange() + " token: '" + token.text() + "'",
        token.textRange().start()));
  }

  private void assertChildRangeIsInsideParentRange(Tree parent, Tree child) {
    TextRange parentRange = parent.metaData().textRange();
    TextRange childRange = child.metaData().textRange();
    if (!childRange.isInside(parentRange)) {
      raiseError(kind(parent) + " contains a child " + kind(child) + " outside its range",
        ", parentRange: " + parentRange + " childRange: " + childRange,
        childRange.start());
    }
  }

  private void assertChildTokens(Set<Token> parentTokens, Map<Token, Tree> childByToken, Tree parent, Tree child) {
    for (Token token : child.metaData().tokens()) {
      if (!parentTokens.contains(token)) {
        raiseError(kind(child) + " contains a token missing in its parent " + kind(parent),
          ", token: '" + token.text() + "'",
          token.textRange().start());
      }
      Tree intersectingChild = childByToken.get(token);
      if (intersectingChild != null) {
        raiseError(kind(parent) + " has a token used by both children " + kind(intersectingChild) + " and " + kind(child),
          ", token: '" + token.text() + "'",
          token.textRange().start());
      } else {
        childByToken.put(token, child);
      }
    }
  }

  private class CodeFormToken {

    private final StringBuilder code = new StringBuilder();
    private final List<Comment> commentsInside;
    private int lastLine = 1;
    private int lastLineOffset = 0;
    private int lastComment = 0;

    private CodeFormToken(TreeMetaData metaData) {
      this.commentsInside = metaData.commentsInside();
      metaData.tokens().forEach(this::add);
      addRemainingComments();
    }

    private void add(Token token) {
      while (lastComment < commentsInside.size() &&
        commentsInside.get(lastComment).textRange().start().compareTo(token.textRange().start()) < 0) {
        Comment comment = commentsInside.get(lastComment);
        addTextAt(comment.text(), comment.textRange());
        lastComment++;
      }
      addTextAt(token.text(), token.textRange());
    }

    private void addRemainingComments() {
      for (int i = lastComment; i < commentsInside.size(); i++) {
        addTextAt(commentsInside.get(i).text(), commentsInside.get(i).textRange());
      }
    }

    private void addTextAt(String text, TextRange textRange) {
      while (lastLine < textRange.start().line()) {
        code.append("\n");
        lastLine++;
        lastLineOffset = 0;
      }
      while (lastLineOffset < textRange.start().lineOffset()) {
        code.append(' ');
        lastLineOffset++;
      }
      code.append(text);
      lastLine = textRange.end().line();
      lastLineOffset = textRange.end().lineOffset();
    }

    private void assertEqualTo(String expectedCode) {
      String[] actualLines = lines(this.code.toString());
      String[] expectedLines = lines(expectedCode);
      for (int i = 0; i < actualLines.length && i < expectedLines.length; i++) {
        if (!actualLines[i].equals(expectedLines[i])) {
          raiseError("Unexpected AST difference", ":\n" +
            "      Actual   : " + actualLines[i] + "\n" +
            "      Expected : " + expectedLines[i] + "\n",
            new TextPointerImpl(i + 1, 0));
        }
      }
      if (actualLines.length != expectedLines.length) {
        raiseError(
          "Unexpected AST number of lines",
          " actual: " + actualLines.length + ", expected: " + expectedLines.length,
          new TextPointerImpl(Math.min(actualLines.length, expectedLines.length), 0));
      }
    }

    private String[] lines(String code) {
      return code
        .replace('\t', ' ')
        .replaceFirst("[\r\n ]+$", "")
        .split(" *(\r\n|\n|\r)", -1);
    }
  }

}
