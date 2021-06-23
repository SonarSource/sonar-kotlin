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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.assertj.core.api.ListAssert;
import org.junit.jupiter.api.Test;
import org.sonar.api.config.Configuration;
import org.sonarsource.slang.api.ASTConverter;
import org.sonarsource.slang.api.Annotation;
import org.sonarsource.slang.api.Comment;
import org.sonarsource.slang.api.HasTextRange;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.NativeKind;
import org.sonarsource.slang.api.TextPointer;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.TopLevelTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import org.sonarsource.slang.impl.BlockTreeImpl;
import org.sonarsource.slang.impl.CommentImpl;
import org.sonarsource.slang.impl.IdentifierTreeImpl;
import org.sonarsource.slang.impl.NativeTreeImpl;
import org.sonarsource.slang.impl.StringLiteralTreeImpl;
import org.sonarsource.slang.impl.TextRangeImpl;
import org.sonarsource.slang.impl.TokenImpl;
import org.sonarsource.slang.impl.TopLevelTreeImpl;
import org.sonarsource.slang.plugin.converter.ASTConverterValidation.ValidationMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ASTConverterValidationTest {

  private static final NativeKind NATIVE_KIND = new NativeKind() {
  };

  @Test
  void delegate_calls() {
    Tree tree = identifier(1, 0, "code");
    SimpleConverter wrappedConverter = new SimpleConverter(tree);
    ASTConverterValidation validationConverter = new ASTConverterValidation(wrappedConverter, ValidationMode.LOG_ERROR);
    assertThat(validationConverter.parse("code")).isSameAs(tree);
    assertThat(wrappedConverter.isTerminated).isFalse();
    validationConverter.terminate();
    assertThat(wrappedConverter.isTerminated).isTrue();
    assertThat(validationConverter.errors()).isEmpty();

    assertThat(validationConverter.parse("BOOM")).isSameAs(tree);
    validationConverter.terminate();
    assertThat(wrappedConverter.isTerminated).isTrue();
    assertThat(validationConverter.errors()).containsExactly("Unexpected AST difference:\n" +
      "      Actual   : code\n" +
      "      Expected : BOOM\n" +
      " (line: 1, column: 1)");
  }

  @Test
  void wrap() {
    SimpleConverter wrappedConverter = new SimpleConverter(null);
    String configKey = "sonar.slang.converter.validation";
    assertThat(ASTConverterValidation.wrap(wrappedConverter, new SimpleConfig(configKey, null))).isSameAs(wrappedConverter);

    ASTConverter converter = ASTConverterValidation.wrap(wrappedConverter, new SimpleConfig(configKey, "throw"));
    assertThat(converter).isInstanceOf(ASTConverterValidation.class);
    assertThat(((ASTConverterValidation) converter).mode()).isEqualTo(ValidationMode.THROW_EXCEPTION);

    converter = ASTConverterValidation.wrap(wrappedConverter, new SimpleConfig(configKey, "log"));
    assertThat(converter).isInstanceOf(ASTConverterValidation.class);
    assertThat(((ASTConverterValidation) converter).mode()).isEqualTo(ValidationMode.LOG_ERROR);
  }

  @Test
  void wrap_error() {
    SimpleConverter wrappedConverter = new SimpleConverter(null);
    String configKey = "sonar.slang.converter.validation";
    IllegalStateException e = assertThrows(IllegalStateException.class,
      () -> ASTConverterValidation.wrap(wrappedConverter, new SimpleConfig(configKey, "BOOM")));
    assertThat(e).hasMessage("Unsupported mode: BOOM");
  }

  @Test
  void text_range() {
    assertValidationErrors("    a", identifier(1, 4, 2, 0, "a"))
      .isEmpty();

    assertValidationErrors("a", identifier(0, 0, 1, 0, "a"))
      .containsExactly("IdentifierTreeImpl invalid range TextRange[0, 0, 1, 0] (line: 0, column: 1)");

    assertValidationErrors("a", identifier(1, -1, 1, 0, "a"))
      .containsExactly("IdentifierTreeImpl invalid range TextRange[1, -1, 1, 0] (line: 1, column: 0)");

    assertValidationErrors("    a", identifier(1, 4, 2, -1, "a"))
      .containsExactly("IdentifierTreeImpl invalid range TextRange[1, 4, 2, -1] (line: 1, column: 5)");

    assertValidationErrors("    a", identifier(1, 4, 1, 4, "a"))
      .containsExactly(
        "IdentifierTreeImpl contains a token outside its range range: TextRange[1, 4, 1, 4] tokenRange: TextRange[1, 4, 1, 5] token: 'a' (line: 1, column: 5)",
        "IdentifierTreeImpl invalid range TextRange[1, 4, 1, 4] (line: 1, column: 5)");

    assertValidationErrors("a", identifier(1, 0, 0, 0, "a"))
      .containsExactly(
        "IdentifierTreeImpl contains a token outside its range range: TextRange[1, 0, 0, 0] tokenRange: TextRange[1, 0, 1, 1] token: 'a' (line: 1, column: 1)",
        "IdentifierTreeImpl invalid range TextRange[1, 0, 0, 0] (line: 1, column: 1)");

    assertValidationErrors("\n\na", identifier(3, 0, 2, 0, "a"))
      .containsExactly(
        "IdentifierTreeImpl contains a token outside its range range: TextRange[3, 0, 2, 0] tokenRange: TextRange[3, 0, 3, 1] token: 'a' (line: 3, column: 1)",
        "IdentifierTreeImpl invalid range TextRange[3, 0, 2, 0] (line: 3, column: 1)");
  }

  @Test
  void missing_token() {
    TextRange range = new TextRangeImpl(1, 0, 1, 1);
    IllegalStateException e = assertThrows(IllegalStateException.class,
      () -> assertValidationErrors("", new IdentifierTreeImpl(metaData(range), "a"), ValidationMode.THROW_EXCEPTION));
    assertThat(e).hasMessageContaining("IdentifierTreeImpl has no token");
  }

  @Test
  void top_level_tree_can_have_zero_token() {
    TopLevelTree topLevelTree = new TopLevelTreeImpl(
      metaData(new TextRangeImpl(1, 0, 1, 0)),
      Collections.emptyList(),
      Collections.emptyList());
    assertValidationErrors("", topLevelTree).isEmpty();
  }

  @Test
  void tokens_and_comments_match_source_code() {
    Token token1 = keyword(1, 0, "package");
    Token token2 = token(2, 2, "abc");
    Comment comment1 = comment(1, 8, "/* comment1 */");
    Comment comment2 = comment(2, 6, "/* comment2 */");
    Tree tree = new NativeTreeImpl(
      metaData(Arrays.asList(token1, token2), Arrays.asList(comment1, comment2)),
      NATIVE_KIND,
      Collections.emptyList());
    String code = "package /* comment1 */ \n  abc /* comment2 */";
    assertValidationErrors(code, tree).isEmpty();
  }

  @Test
  void missing_token_compare_to_source_code() {
    Token token = keyword(1, 0, "package");
    Tree tree = new NativeTreeImpl(
      metaData(Collections.singletonList(token), Collections.emptyList()),
      NATIVE_KIND,
      Collections.emptyList());
    String code = "package /* comment */";
    assertValidationErrors(code, tree)
      .containsExactly("Unexpected AST difference:\n" +
        "      Actual   : package\n" +
        "      Expected : package /* comment */\n" +
        " (line: 1, column: 1)");
  }

  @Test
  void source_code_has_unexpected_lines() {
    Token token1 = keyword(1, 0, "package");
    Tree tree = new NativeTreeImpl(metaData(Collections.singletonList(token1), Collections.emptyList()), NATIVE_KIND, Collections.emptyList());
    String code = "package\n/* comment */";
    assertValidationErrors(code, tree)
      .containsExactly("Unexpected AST number of lines actual: 1, expected: 2 (line: 1, column: 1)");
  }

  @Test
  void extra_token_not_in_source_code() {
    Token token1 = keyword(1, 0, "package");
    Token token2 = token(2, 2, "abc");
    Tree tree = new NativeTreeImpl(
      metaData(Arrays.asList(token1, token2), Collections.emptyList()),
      NATIVE_KIND,
      Collections.emptyList());
    String code = "package";
    assertValidationErrors(code, tree)
      .containsExactly("Unexpected AST number of lines actual: 2, expected: 1 (line: 1, column: 1)");
  }

  @Test
  void native_tree_and_literal_accept_any_tokens() {
    TreeMetaData metaData = metaData(
      keyword(1, 0, "if"),
      token(1, 3, "value"),
      token(1, 9, "=="),
      stringLiteral(1, 12, "\"a\""));

    assertValidationErrors("if value == \"a\"", new NativeTreeImpl(metaData, NATIVE_KIND, Collections.emptyList()))
      .isEmpty();

    assertValidationErrors("if value == \"a\"", new StringLiteralTreeImpl(metaData, "\"a\""))
      .isEmpty();
  }

  @Test
  void identifier_tree_does_not_accept_keyword_and_string_literal_tokens() {
    TreeMetaData metaData = metaData(
      keyword(1, 0, "if"),
      token(1, 3, "value"),
      token(1, 9, "=="),
      stringLiteral(1, 12, "\"a\""));
    assertValidationErrors("if value == \"a\"", new IdentifierTreeImpl(metaData, "value"))
      .containsExactly("Unexpected tokens in IdentifierTreeImpl: 'if', '\"a\"' (line: 1, column: 1)");
  }

  @Test
  void non_identifier_tree_does_not_accept_identifier_tokens() {
    TreeMetaData metaData = metaData(
      keyword(1, 0, "if"),
      token(1, 3, "value"),
      token(1, 9, "=="),
      stringLiteral(1, 12, "\"a\""));
    assertValidationErrors("if value == \"a\"", new BlockTreeImpl(metaData, Collections.emptyList()))
      .containsExactly("Unexpected tokens in BlockTreeImpl: 'value', '\"a\"' (line: 1, column: 1)");
  }

  @Test
  void child_range_or_token_inside_parent_range() {
    Token identifierToken = token(1, 0, "value");
    IdentifierTreeImpl identifier = new IdentifierTreeImpl(metaData(identifierToken), "value");
    BlockTreeImpl block = new BlockTreeImpl(metaData(identifierToken), Collections.singletonList(identifier));

    assertValidationErrors("value", block).isEmpty();
  }

  @Test
  void allowed_misplaced_token() {
    Token misplacedToken = token(1, 0, "implicit");
    Tree misplacedTree = new NativeTreeImpl(metaData(misplacedToken), NATIVE_KIND, Collections.emptyList());

    Token identifierToken = token(1, 9, "value");
    IdentifierTreeImpl identifierTree = new IdentifierTreeImpl(metaData(identifierToken), "value");

    TreeMetaData blockMetaData = metaData(identifierTree.textRange(), misplacedToken, identifierToken);
    BlockTreeImpl block = new BlockTreeImpl(blockMetaData, Arrays.asList(misplacedTree, identifierTree));

    assertValidationErrors("implicit value", block).isEmpty();
  }

  @Test
  void child_range_or_token_outside_parent_range() {
    Token ifToken = keyword(1, 0, "if");
    Token identifierToken = token(1, 3, "value");
    IdentifierTreeImpl identifier = new IdentifierTreeImpl(metaData(identifierToken), "value");
    BlockTreeImpl block = new BlockTreeImpl(metaData(ifToken), Collections.singletonList(identifier));

    assertValidationErrors("if", block).containsExactly(
        "BlockTreeImpl contains a child IdentifierTreeImpl outside its range, parentRange: TextRange[1, 0, 1, 2] childRange: TextRange[1, 3, 1, 8] (line: 1, column: 4)",
        "IdentifierTreeImpl contains a token missing in its parent BlockTreeImpl, token: 'value' (line: 1, column: 4)");
  }

  @Test
  void several_children_outside_parent_range() {
    Token ifToken = keyword(1, 0, "if");
    Token annotationToken = token(1, 3, "@transient");
    Token identifierToken = token(1, 14, "value");
    IdentifierTreeImpl identifier = new IdentifierTreeImpl(metaData(annotationToken, identifierToken), "value");
    BlockTreeImpl block = new BlockTreeImpl(metaData(ifToken.textRange(), ifToken, annotationToken, identifierToken), Collections.singletonList(identifier));

    assertValidationErrors("if @transient value", block).containsExactly(
      "BlockTreeImpl contains a child IdentifierTreeImpl outside its range, parentRange: TextRange[1, 0, 1, 2] childRange: TextRange[1, 3, 1, 19] (line: 1, column: 4)",
      "BlockTreeImpl contains a token outside its range range: TextRange[1, 0, 1, 2] tokenRange: TextRange[1, 3, 1, 13] token: '@transient' (line: 1, column: 4)");
  }

  @Test
  void same_token_in_two_children() {
    Token identifierToken = token(1, 0, "value");
    IdentifierTreeImpl identifier1 = new IdentifierTreeImpl(metaData(identifierToken), "value");
    IdentifierTreeImpl identifier2 = new IdentifierTreeImpl(metaData(identifierToken), "value");
    BlockTreeImpl block = new BlockTreeImpl(metaData(identifierToken), Arrays.asList(identifier1, identifier2));

    assertValidationErrors("value", block)
      .containsExactly("BlockTreeImpl has a token used by both children IdentifierTreeImpl and IdentifierTreeImpl, token: 'value' (line: 1, column: 1)");
  }

  @Test
  void report_null_child_and_null_metaData() {
    Token token = keyword(1, 0, "if");
    IdentifierTreeImpl identifier = new IdentifierTreeImpl(null, "if");
    BlockTreeImpl block = new BlockTreeImpl(metaData(token), Arrays.asList(identifier, null));

    assertValidationErrors("if", block)
      .containsExactly(
        "BlockTreeImpl has a null child (line: 1, column: 1)",
        "IdentifierTreeImpl metaData is null (line: 1, column: 1)");
  }

  @Test
  void source_file_name_in_logs_when_set() {
    String fileName = "my/file/name.java";
    String code = "a";
    Tree tree = identifier(0, 0, 1, 0, "a");
    SimpleConverter wrappedConverter = new SimpleConverter(tree);
    ASTConverterValidation validationConverter = new ASTConverterValidation(wrappedConverter, ValidationMode.LOG_ERROR);
    assertThat(validationConverter.parse(code, fileName)).isSameAs(tree);
    assertThat(validationConverter.errors())
      .containsExactly("IdentifierTreeImpl invalid range TextRange[0, 0, 1, 0] (line: 0, column: 1) in file: " + fileName);
  }

  private ListAssert<String> assertValidationErrors(String code, Tree tree) {
    return assertValidationErrors(code, tree, ValidationMode.LOG_ERROR);
  }

  private ListAssert<String> assertValidationErrors(String code, Tree tree, ValidationMode mode) {
    SimpleConverter wrappedConverter = new SimpleConverter(tree);
    ASTConverterValidation validationConverter = new ASTConverterValidation(wrappedConverter, mode);
    assertThat(validationConverter.parse(code)).isSameAs(tree);
    return assertThat(validationConverter.errors());
  }

  public static IdentifierTree identifier(int startLine, int startLineOffset, int endLine, int endLineOffset, String text) {
    Token token = token(startLine, startLineOffset, text);
    TextRange range = new TextRangeImpl(startLine, startLineOffset, endLine, endLineOffset);
    return new IdentifierTreeImpl(metaData(range, token), text);
  }

  public static IdentifierTree identifier(int line, int lineOffset, String text) {
    Token token = token(line, lineOffset, text);
    return new IdentifierTreeImpl(metaData(token), text);
  }

  public static TreeMetaData metaData(Token... tokens) {
    return metaData(Arrays.asList(tokens), Collections.emptyList());
  }

  public static TreeMetaData metaData(List<Token> tokens, List<Comment> comments) {
    List<TextRange> textRanges = Stream.of(tokens, comments)
      .flatMap(List::stream)
      .map(HasTextRange::textRange)
      .collect(Collectors.toList());

    TextPointer start = textRanges.stream().map(TextRange::start).min(Comparator.naturalOrder()).orElse(null);
    TextPointer end = textRanges.stream().map(TextRange::end).max(Comparator.naturalOrder()).orElse(null);
    TextRange textRange = new TextRangeImpl(start, end);

    return metaData(textRange, tokens, comments);
  }

  public static TreeMetaData metaData(TextRange textRange, Token... tokens) {
    return metaData(textRange, Arrays.asList(tokens), Collections.emptyList());
  }

  private static TreeMetaData metaData(TextRange textRange, List<Token> tokens, List<Comment> comments) {
    return new TreeMetaData() {
      @Override
      public TextRange textRange() {
        return textRange;
      }

      @Override
      public List<Comment> commentsInside() {
        return comments;
      }

      @Override
      public List<Annotation> annotations() {
        return Collections.emptyList();
      }

      @Override
      public List<Token> tokens() {
        return tokens;
      }

      @Override
      public Set<Integer> linesOfCode() {
        throw new UnsupportedOperationException();
      }
    };
  }

  public static Comment comment(int line, int lineOffset, String text) {
    String textContent = text.substring(2, text.length() - 2);
    return new CommentImpl(text, textContent, range(line, lineOffset, text), range(line, lineOffset + 2, textContent));
  }

  public static Token keyword(int line, int lineOffset, String text) {
    return new TokenImpl(range(line, lineOffset, text), text, Token.Type.KEYWORD);
  }

  public static Token stringLiteral(int line, int lineOffset, String text) {
    return new TokenImpl(range(line, lineOffset, text), text, Token.Type.STRING_LITERAL);
  }

  public static Token token(int line, int lineOffset, String text) {
    return new TokenImpl(range(line, lineOffset, text), text, Token.Type.OTHER);
  }

  private static TextRange range(int line, int lineOffset, String text) {
    String[] lines = text.split("\r\n|\n|\r", -1);
    int endLineOffset = lines.length == 1 ? lineOffset + text.length() : lines[lines.length - 1].length();
    return new TextRangeImpl(line, lineOffset, line + lines.length - 1, endLineOffset);
  }

  private static class SimpleConverter implements ASTConverter {

    private Tree parsedTreeToReturn;

    private boolean isTerminated = false;

    public SimpleConverter(Tree parsedTreeToReturn) {
      this.parsedTreeToReturn = parsedTreeToReturn;
    }

    @Override
    public Tree parse(String content) {
      return parsedTreeToReturn;
    }

    @Override
    public void terminate() {
      isTerminated = true;
    }
  }

  private class SimpleConfig implements Configuration {

    private final String key;
    private final String value;

    private SimpleConfig(String key, String value) {
      this.key = key;
      this.value = value;
    }

    @Override
    public Optional<String> get(String key) {
      return Optional.ofNullable(this.key.equals(key) ? value : null);
    }

    @Override
    public boolean hasKey(String key) {
      return this.key.equals(key);
    }

    @Override
    public String[] getStringArray(String key) {
      throw new UnsupportedOperationException();
    }

  }
}
