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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.sonarsource.slang.api.Annotation;
import org.sonarsource.slang.api.Comment;
import org.sonarsource.slang.api.HasTextRange;
import org.sonarsource.slang.api.TextPointer;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.TreeMetaData;

public class TreeMetaDataProvider {

  public static final Comparator<HasTextRange> COMPARATOR = Comparator.comparing(e -> e.textRange().start());

  private final List<Comment> sortedComments;
  private final List<Annotation> sortedAnnotations;
  private final List<Token> sortedTokens;

  public TreeMetaDataProvider(List<Comment> comments, List<Token> tokens) {
    this(comments, tokens, Collections.emptyList());
  }

  public TreeMetaDataProvider(List<Comment> comments, List<Token> tokens, List<Annotation> annotations) {
    this.sortedComments = new ArrayList<>(comments);
    this.sortedComments.sort(COMPARATOR);
    this.sortedTokens = new ArrayList<>(tokens);
    this.sortedTokens.sort(COMPARATOR);
    this.sortedAnnotations = new ArrayList<>(annotations);
    this.sortedAnnotations.sort(COMPARATOR);
  }

  public List<Comment> allComments() {
    return sortedComments;
  }

  public List<Token> allTokens() {
    return sortedTokens;
  }

  public int indexOfFirstToken(TextRange textRange) {
    return indexOfFirstElement(sortedTokens, textRange);
  }

  public Optional<Token> firstToken(TextRange textRange) {
    int textRangeIndex = indexOfFirstElement(sortedTokens, textRange);
    if (textRangeIndex == -1) {
      return Optional.empty();
    } else {
      return Optional.of(sortedTokens.get(textRangeIndex));
    }
  }

  public Optional<Token> previousToken(TextRange textRange) {
    int textRangeIndex = indexOfFirstElement(sortedTokens, textRange);
    if (textRangeIndex <= 0) {
      return Optional.empty();
    } else {
      return Optional.of(sortedTokens.get(textRangeIndex - 1));
    }
  }

  public Optional<Token> previousToken(TextRange textRange, String expectedTokenValue) {
    return previousToken(textRange, token -> expectedTokenValue.equals(token.text()));
  }

  public Optional<Token> previousToken(TextRange textRange, Predicate<Token> expectedConditionToMatch) {
    return previousToken(textRange).filter(expectedConditionToMatch::test);
  }

  public void updateTokenType(Token token, Token.Type newType) {
    int tokenIndex = indexOfFirstToken(token.textRange());
    if (!isExistingToken(token, tokenIndex)) {
      throw new IllegalArgumentException("token '" + token.text() + "' not found in metadata, " + token.textRange());
    }
    this.sortedTokens.set(tokenIndex, new TokenImpl(token.textRange(), token.text(), newType));
  }

  private boolean isExistingToken(Token token, int tokenIndex) {
    return tokenIndex != -1 && this.sortedTokens.get(tokenIndex) == token;
  }

  public Token keyword(TextRange textRange) {
    List<Token> keywordsInRange = getElementsInRange(sortedTokens, textRange).stream()
      .filter(t -> t.type() == Token.Type.KEYWORD)
      .collect(Collectors.toList());
    if (keywordsInRange.size() != 1) {
      throw new IllegalArgumentException("Cannot find single keyword in " + textRange);
    }
    return keywordsInRange.get(0);
  }

  private static <T extends HasTextRange> int indexOfFirstElement(List<T> sortedList, TextRange textRange) {
    HasTextRange key = () -> textRange;
    int index = Collections.binarySearch(sortedList, key, COMPARATOR);
    if (index < 0) {
      index = -index - 1;
    }
    if (index < sortedList.size() && sortedList.get(index).textRange().isInside(textRange)) {
      return index;
    }
    return -1;
  }

  private static <T extends HasTextRange> List<T> getElementsInRange(List<T> sortedList, TextRange textRange) {
    int first = indexOfFirstElement(sortedList, textRange);
    if (first == -1) {
      return Collections.emptyList();
    }
    List<T> elementsInsideRange = new ArrayList<>();
    for (int i = first; i < sortedList.size(); i++) {
      T element = sortedList.get(i);
      if (!element.textRange().isInside(textRange)) {
        break;
      }
      elementsInsideRange.add(element);
    }
    return elementsInsideRange;
  }

  private static List<Annotation> getAnnotationStartingAtRange(List<Annotation> sortedList, List<Token> sortedToken, TextRange textRange) {
    int first = indexOfFirstElement(sortedList, textRange);
    if (first == -1) {
      return Collections.emptyList();
    }

    List<Annotation> elementsInsideRange = new ArrayList<>();
    TextPointer currentPointer = textRange.start();

    for (int i = first; i < sortedList.size(); i++) {
      Annotation currentAnnotation = sortedList.get(i);
      if (!currentAnnotation.textRange().start().equals(currentPointer)) {
        break;
      }
      // We found a first annotation starting at the beginning of the text range.
      elementsInsideRange.add(currentAnnotation);
      // In addition, we also want all annotations that are just after the current one.
      // A potential candidate is one starting at the position of the token following the current annotation.
      int nextAnnotation = indexOfFirstElement(sortedToken, new TextRangeImpl(currentAnnotation.textRange().end(), textRange.end()));
      if (nextAnnotation < 0) {
        break;
      } else {
        currentPointer = sortedToken.get(nextAnnotation).textRange().start();
      }
    }

    return elementsInsideRange;
  }

  public TreeMetaData metaData(TextRange textRange) {
    return new TreeMetaDataImpl(textRange);
  }

  private class TreeMetaDataImpl implements TreeMetaData {

    private final TextRange textRange;
    private Set<Integer> linesOfCode;
    private List<Annotation> annotations;

    private TreeMetaDataImpl(TextRange textRange) {
      this.textRange = textRange;
    }

    @Override
    public TextRange textRange() {
      return textRange;
    }

    @Override
    public List<Comment> commentsInside() {
      return getElementsInRange(sortedComments, textRange);
    }

    @Override
    public List<Annotation> annotations() {
      if (annotations == null) {
        annotations = getAnnotationStartingAtRange(sortedAnnotations, sortedTokens, textRange);
      }
      return annotations;
    }

    @Override
    public List<Token> tokens() {
      return getElementsInRange(sortedTokens, textRange);
    }

    @Override
    public Set<Integer> linesOfCode() {
      if (linesOfCode == null) {
        linesOfCode = computeLinesOfCode();
      }
      return linesOfCode;
    }

    private Set<Integer> computeLinesOfCode() {
      Set<Integer> loc = new HashSet<>();
      for (Token token : tokens()) {
        TextRange range = token.textRange();
        for (int i = range.start().line(); i <= range.end().line(); i++) {
          loc.add(i);
        }
      }
      return loc;
    }

  }

}
