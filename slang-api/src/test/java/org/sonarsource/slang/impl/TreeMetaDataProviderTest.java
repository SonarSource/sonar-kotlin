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

import java.util.Collections;
import java.util.List;
import org.sonarsource.slang.api.Annotation;
import org.sonarsource.slang.api.Comment;
import org.sonarsource.slang.api.Token;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.sonarsource.slang.api.TreeMetaData;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonarsource.slang.impl.TextRanges.range;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

class TreeMetaDataProviderTest {

  @Test
  void commentsInside() {
    Comment comment = new CommentImpl("// comment1", "comment1", range(2, 5, 2, 12), range(2, 7, 2, 12));
    TreeMetaDataProvider provider = new TreeMetaDataProvider(singletonList(comment), emptyList());
    assertThat(provider.allComments()).hasSize(1);
    assertThat(provider.metaData(new TextRangeImpl(1, 1, 1, 20)).commentsInside()).isEmpty();
    assertThat(provider.metaData(new TextRangeImpl(2, 1, 2, 20)).commentsInside()).containsExactly(comment);
    assertThat(provider.metaData(new TextRangeImpl(2, 5, 2, 20)).commentsInside()).containsExactly(comment);
  }

  @Test
  void single_annotation() {
    Annotation annotation = new AnnotationImpl("MyAnnotation", emptyList(), range(2, 5, 2, 13));
    Token token1 = new TokenImpl(new TextRangeImpl(2, 5, 2, 6), "@", Token.Type.OTHER);
    Token token2 = new TokenImpl(new TextRangeImpl(2, 6, 2, 13), "MyAnnotation", Token.Type.OTHER);

    Token token3 = new TokenImpl(new TextRangeImpl(3, 5, 3, 6), "class", Token.Type.OTHER);

    TreeMetaDataProvider provider = new TreeMetaDataProvider(emptyList(), Arrays.asList(token1, token2, token3), singletonList(annotation));

    List<Annotation> oneAnnotation = provider.metaData(new TextRangeImpl(2, 5, 25, 25)).annotations();
    assertThat(oneAnnotation).containsExactly(annotation);
  }

  @Test
  void multiple_annotations() {
    Annotation annotation1 = new AnnotationImpl("MyAnnotation", emptyList(), range(2, 5, 2, 13));
    Token token1 = new TokenImpl(new TextRangeImpl(2, 5, 2, 6), "@", Token.Type.OTHER);
    Token token2 = new TokenImpl(new TextRangeImpl(2, 6, 2, 13), "MyAnnotation", Token.Type.OTHER);

    Annotation annotation2 = new AnnotationImpl("MyAnnotation2", Collections.singletonList("abc"), range(3, 5, 3, 14));
    Token token3 = new TokenImpl(new TextRangeImpl(3, 5, 3, 6), "@", Token.Type.OTHER);
    Token token4 = new TokenImpl(new TextRangeImpl(3, 6, 3, 14), "MyAnnotation2", Token.Type.OTHER);

    // A token between the second and the third annotation
    Token token5 = new TokenImpl(new TextRangeImpl(4, 6, 4, 9), "fun", Token.Type.OTHER);

    Annotation annotation3 = new AnnotationImpl("MyAnnotation3", emptyList(), range(5, 5, 5, 14));
    Token token6 = new TokenImpl(new TextRangeImpl(5, 5, 5, 6), "@", Token.Type.OTHER);
    Token token7 = new TokenImpl(new TextRangeImpl(5, 6, 5, 14), "MyAnnotation3", Token.Type.OTHER);

    TreeMetaDataProvider provider = new TreeMetaDataProvider(emptyList(),
      Arrays.asList(token1, token2, token3, token4, token5, token6, token7),
      Arrays.asList(annotation1, annotation2, annotation3));
    // Annotations have to start the same place as the range. All annotations directly following the first one will be returned.

    List<Annotation> twoAnnotations = provider.metaData(new TextRangeImpl(2, 5, 10, 13)).annotations();
    assertThat(twoAnnotations).containsExactly(annotation1, annotation2);
    Annotation firstAnnotation = twoAnnotations.get(0);
    assertThat(firstAnnotation.shortName()).isEqualTo("MyAnnotation");
    assertThat(firstAnnotation.argumentsText()).isEmpty();
    Annotation secondAnnotation = twoAnnotations.get(1);
    assertThat(secondAnnotation.shortName()).isEqualTo("MyAnnotation2");
    assertThat(secondAnnotation.argumentsText()).containsExactly("abc");
    // The end position does not matters
    assertThat(provider.metaData(new TextRangeImpl(2, 5, 25, 14)).annotations()).containsExactly(annotation1, annotation2);
    assertThat(provider.metaData(new TextRangeImpl(2, 6, 10, 13)).annotations()).isEmpty();
    assertThat(provider.metaData(new TextRangeImpl(3, 5, 10, 13)).annotations()).containsExactly(annotation2);
    assertThat(provider.metaData(new TextRangeImpl(3, 1, 10, 13)).annotations()).isEmpty();
    assertThat(provider.metaData(new TextRangeImpl(9, 1, 10, 13)).annotations()).isEmpty();
    assertThat(provider.metaData(new TextRangeImpl(5, 5, 10, 6)).annotations()).containsExactly(annotation3);
  }

  @Test
  void annotations_are_cached() {
    Annotation annotation = new AnnotationImpl("MyAnnotation", emptyList(), range(2, 5, 2, 13));
    TreeMetaDataProvider provider = new TreeMetaDataProvider(emptyList(), emptyList(), singletonList(annotation));
    TreeMetaData metaData = provider.metaData(new TextRangeImpl(2, 5, 2, 13));
    List<Annotation> firstCall = metaData.annotations();
    List<Annotation> secondCall = metaData.annotations();
    assertThat(firstCall).isSameAs(secondCall);
  }

  @Test
  void tokens() {
    Token token1 = new TokenImpl(new TextRangeImpl(1, 3, 1, 6), "abc", Token.Type.OTHER);
    Token token2 = new TokenImpl(new TextRangeImpl(1, 9, 1, 12), "abc", Token.Type.OTHER);
    TreeMetaDataProvider provider = new TreeMetaDataProvider(emptyList(), Arrays.asList(token1, token2));
    assertThat(provider.allTokens()).hasSize(2);
    assertThat(provider.metaData(new TextRangeImpl(1, 1, 1, 20)).tokens()).containsExactly(token1, token2);
    assertThat(provider.metaData(new TextRangeImpl(1, 3, 1, 8)).tokens()).containsExactly(token1);
    assertThat(provider.metaData(new TextRangeImpl(1, 3, 1, 6)).tokens()).containsExactly(token1);
  }

  @Test
  void lines_of_code() {
    Token token1 = new TokenImpl(new TextRangeImpl(1, 3, 1, 6), "abc", Token.Type.OTHER);
    Token token2 = new TokenImpl(new TextRangeImpl(1, 9, 1, 12), "def", Token.Type.OTHER);
    Token token3 = new TokenImpl(new TextRangeImpl(2, 1, 2, 4), "abc", Token.Type.OTHER);
    Token token4 = new TokenImpl(new TextRangeImpl(4, 1, 6, 2), "ab\ncd\nef", Token.Type.OTHER);
    TreeMetaDataProvider provider = new TreeMetaDataProvider(emptyList(), Arrays.asList(token1, token2, token3, token4));
    TreeMetaData metaData = provider.metaData(new TextRangeImpl(1, 1, 1, 20));
    assertThat(metaData.linesOfCode()).containsExactly(1);
    assertThat(metaData.linesOfCode()).containsExactly(1);
    assertThat(metaData.textRange()).hasToString("TextRange[1, 1, 1, 20]");
    assertThat(provider.metaData(new TextRangeImpl(1, 1, 2, 20)).linesOfCode()).containsExactly(1, 2);
    assertThat(provider.metaData(new TextRangeImpl(1, 1, 3, 20)).linesOfCode()).containsExactly(1, 2);
    assertThat(provider.metaData(new TextRangeImpl(1, 1, 6, 20)).linesOfCode()).containsExactly(1, 2, 4, 5, 6);
  }

  @Test
  void keyword() {
    Token token1 = new TokenImpl(range(1, 1, 1, 3), "ab", Token.Type.KEYWORD);
    Token token2 = new TokenImpl(range(1, 4, 1, 6), "cd", Token.Type.KEYWORD);
    Token token3 = new TokenImpl(range(1, 6, 1, 7), "{",  Token.Type.OTHER);
    Token token4 = new TokenImpl(range(1, 7, 1, 8), "ef", Token.Type.OTHER);
    TreeMetaDataProvider provider = new TreeMetaDataProvider(emptyList(), Arrays.asList(token1, token2, token3, token4));
    assertThat(provider.keyword(range(1, 3, 1, 7))).isEqualTo(token2);
    assertThat(provider.keyword(range(1, 3, 1, 8))).isEqualTo(token2);
    assertThatThrownBy(() -> provider.keyword(range(1, 3, 1, 4)))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Cannot find single keyword in TextRange[1, 3, 1, 4]");
    assertThatThrownBy(() -> provider.keyword(range(1, 1, 1, 7)))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Cannot find single keyword in TextRange[1, 1, 1, 7]");
  }

  @Test
  void all_tokens() {
    Token token1 = new TokenImpl(range(1, 1, 1, 3), "ab", Token.Type.KEYWORD);
    Token token2 = new TokenImpl(range(1, 4, 1, 6), "cd", Token.Type.KEYWORD);
    TreeMetaDataProvider provider = new TreeMetaDataProvider(emptyList(), Arrays.asList(token1, token2));
    List<Token> allTokens = provider.allTokens();
    assertThat(allTokens).hasSize(2);
    assertThat(allTokens.get(0).text()).isEqualTo("ab");
    assertThat(allTokens.get(1).text()).isEqualTo("cd");
  }

  @Test
  void index_of_first_token() {
    Token token1 = new TokenImpl(range(1, 1, 1, 3), "ab", Token.Type.KEYWORD);
    Token token2 = new TokenImpl(range(1, 4, 1, 6), "cd", Token.Type.KEYWORD);
    TreeMetaDataProvider provider = new TreeMetaDataProvider(emptyList(), Arrays.asList(token1, token2));
    assertThat(provider.indexOfFirstToken(range(1, 0, 1, 1))).isEqualTo(-1);
    assertThat(provider.indexOfFirstToken(range(1, 0, 1, 2))).isEqualTo(-1);
    assertThat(provider.indexOfFirstToken(range(1, 0, 1, 3))).isZero();
    assertThat(provider.indexOfFirstToken(range(1, 1, 1, 3))).isZero();
    assertThat(provider.indexOfFirstToken(range(1, 2, 1, 3))).isEqualTo(-1);
    assertThat(provider.indexOfFirstToken(range(1, 2, 1, 6))).isEqualTo(1);
    assertThat(provider.indexOfFirstToken(range(1, 4, 1, 6))).isEqualTo(1);
    assertThat(provider.indexOfFirstToken(range(1, 4, 2, 0))).isEqualTo(1);
    assertThat(provider.indexOfFirstToken(range(1, 4, 1, 5))).isEqualTo(-1);
    assertThat(provider.indexOfFirstToken(range(1, 5, 1, 10))).isEqualTo(-1);
    assertThat(provider.indexOfFirstToken(range(1, 20, 1, 22))).isEqualTo(-1);
  }

  @Test
  void first_token() {
    Token token1 = new TokenImpl(range(1, 1, 1, 3), "ab", Token.Type.KEYWORD);
    Token token2 = new TokenImpl(range(1, 4, 1, 6), "cd", Token.Type.KEYWORD);
    TreeMetaDataProvider provider = new TreeMetaDataProvider(emptyList(), Arrays.asList(token1, token2));
    assertThat(provider.firstToken(range(1, 0, 1, 1))).isNotPresent();
    assertThat(provider.firstToken(range(1, 1, 1, 3)).get().text()).isEqualTo("ab");
    assertThat(provider.firstToken(range(1, 2, 1, 20)).get().text()).isEqualTo("cd");
    assertThat(provider.firstToken(range(1, 5, 1, 20))).isNotPresent();
  }

  @Test
  void previous_token() {
    Token token1 = new TokenImpl(range(1, 1, 1, 3), "ab", Token.Type.KEYWORD);
    Token token2 = new TokenImpl(range(1, 4, 1, 6), "cd", Token.Type.KEYWORD);
    TreeMetaDataProvider provider = new TreeMetaDataProvider(emptyList(), Arrays.asList(token1, token2));

    assertThat(provider.previousToken(range(1, 0, 1, 1))).isNotPresent();
    assertThat(provider.previousToken(range(1, 1, 1, 3))).isNotPresent();
    assertThat(provider.previousToken(range(1, 2, 1, 20)).get().text()).isEqualTo("ab");
    assertThat(provider.previousToken(range(1, 5, 1, 20))).isNotPresent();
  }

  @Test
  void previous_token_with_expected_value() {
    Token token1 = new TokenImpl(range(1, 1, 1, 3), "ab", Token.Type.KEYWORD);
    Token token2 = new TokenImpl(range(1, 4, 1, 6), "cd", Token.Type.KEYWORD);
    TreeMetaDataProvider provider = new TreeMetaDataProvider(emptyList(), Arrays.asList(token1, token2));

    assertThat(provider.previousToken(range(1, 2, 1, 20), "ef")).isNotPresent();
    assertThat(provider.previousToken(range(1, 2, 1, 20), "ab")).isPresent();
    assertThat(provider.previousToken(range(1, 2, 1, 20), "AB")).isNotPresent();

    assertThat(provider.previousToken(range(1, 2, 1, 20), token -> true)).isPresent();
    assertThat(provider.previousToken(range(1, 2, 1, 20), token -> false)).isNotPresent();
  }

  @Test
  void update_token_type() {
    Token token1 = new TokenImpl(range(1, 1, 1, 3), "ab", Token.Type.OTHER);
    Token token2 = new TokenImpl(range(1, 4, 1, 6), "cd", Token.Type.OTHER);
    TreeMetaDataProvider provider = new TreeMetaDataProvider(emptyList(), Arrays.asList(token1, token2));
    List<Token> allTokens = provider.allTokens();
    assertThat(allTokens).hasSize(2);
    provider.updateTokenType(allTokens.get(0), Token.Type.KEYWORD);
    assertThat(allTokens.get(0).text()).isEqualTo("ab");
    assertThat(allTokens.get(0).type()).isEqualTo(Token.Type.KEYWORD);
    assertThat(allTokens.get(1).text()).isEqualTo("cd");
    assertThat(allTokens.get(1).type()).isEqualTo(Token.Type.OTHER);
  }

  @Test
  void error_when_updating_token_type() {
    Token token1 = new TokenImpl(range(1, 1, 1, 3), "ab", Token.Type.OTHER);
    TreeMetaDataProvider provider = new TreeMetaDataProvider(emptyList(), Arrays.asList(token1));

    Token tokenNotInMetaData1 = new TokenImpl(range(1, 0, 1, 3), "xyz", Token.Type.OTHER);
    assertThatThrownBy(() -> provider.updateTokenType(tokenNotInMetaData1, Token.Type.KEYWORD))
      .hasMessage("token 'xyz' not found in metadata, TextRange[1, 0, 1, 3]");

    Token tokenNotInMetaData2 = new TokenImpl(range(1, 20, 1, 23), "xyz", Token.Type.OTHER);
    assertThatThrownBy(() -> provider.updateTokenType(tokenNotInMetaData2, Token.Type.KEYWORD))
      .hasMessage("token 'xyz' not found in metadata, TextRange[1, 20, 1, 23]");

  }
}
