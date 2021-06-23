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
package org.sonarsource.slang.persistence;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.WriterConfig;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonarsource.slang.api.Comment;
import org.sonarsource.slang.api.HasTextRange;
import org.sonarsource.slang.api.TextPointer;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import org.sonarsource.slang.impl.CommentImpl;
import org.sonarsource.slang.impl.TextRangeImpl;
import org.sonarsource.slang.impl.TextRanges;
import org.sonarsource.slang.impl.TokenImpl;
import org.sonarsource.slang.impl.TreeMetaDataProvider;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonTestHelper {

  protected TreeMetaDataProvider metaDataProvider = new TreeMetaDataProvider(Collections.emptyList(), Collections.emptyList());

  protected Token token(int line, int lineOffset, String text, Token.Type type) {
    TextRange tokenRange = new TextRangeImpl(line, lineOffset, line, lineOffset + text.length());
    Token token = new TokenImpl(tokenRange, text, type);
    metaDataProvider.allTokens().add(token);
    metaDataProvider.allTokens().sort(TreeMetaDataProvider.COMPARATOR);
    return token;
  }

  protected Token stringToken(int line, int lineOffset, String text) {
    return token(line, lineOffset, text, Token.Type.STRING_LITERAL);
  }

  protected Comment comment(int line, int lineOffset, String commentText, int prefixLength, int suffixLength) {
    String commentContentText = commentText.substring(prefixLength, commentText.length() - suffixLength);
    TextRange commentRange = new TextRangeImpl(line, lineOffset, line, lineOffset + commentText.length());
    TextRange commentContentRange = new TextRangeImpl(line, lineOffset + prefixLength,
      line, lineOffset + commentText.length() - suffixLength);
    CommentImpl comment = new CommentImpl(commentText, commentContentText, commentRange, commentContentRange);
    metaDataProvider.allComments().add(comment);
    metaDataProvider.allComments().sort(TreeMetaDataProvider.COMPARATOR);
    return comment;
  }

  protected Token otherToken(int line, int lineOffset, String text) {
    return token(line, lineOffset, text, Token.Type.OTHER);
  }

  protected Token keywordToken(int line, int lineOffset, String text) {
    return token(line, lineOffset, text, Token.Type.KEYWORD);
  }

  protected TreeMetaData metaData(TextRange textRange) {
    return metaDataProvider.metaData(textRange);
  }

  protected TreeMetaData metaData(Token token) {
    return metaData(token.textRange());
  }

  protected TreeMetaData metaData(HasTextRange from, HasTextRange to) {
    return metaData(TextRanges.merge(Arrays.asList(from.textRange(), to.textRange())));
  }

  protected static String indentedJson(String json) throws IOException {
    return Json.parse(json).toString(WriterConfig.PRETTY_PRINT);
  }

  protected static String indentedJsonFromFile(String fileName) throws IOException {
    Path path = Paths.get("src", "test", "resources", "org", "sonarsource", "slang", "persistence", fileName);
    return indentedJson(new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
  }

  public static <T extends Tree> T checkJsonSerializationDeserialization(T initialTree, String fileName) throws IOException {
    String initialTreeAsJson = indentedJson(JsonTree.toJson(initialTree));
    String expectedJson = indentedJsonFromFile(fileName);
    assertThat(initialTreeAsJson)
      .describedAs("Comparing tree serialized into json with " + fileName)
      .isEqualTo(expectedJson);

    T loadedTree = (T) JsonTree.fromJson(initialTreeAsJson);
    String loadedTreeAsJson = indentedJson(JsonTree.toJson(loadedTree));
    assertThat(loadedTreeAsJson)
      .describedAs("Comparing tree de-serialized/serialized into json with " + fileName)
      .isEqualTo(expectedJson);

    return loadedTree;
  }

  public static String tokens(Tree tree) {
    return tokens(tree.metaData());
  }

  public static String tokens(TreeMetaData metaData) {
    return tokens(metaData.tokens());
  }

  public static String tokens(List<Token> tokens) {
    if (tokens.isEmpty()) {
      return "";
    }
    TextPointer start = tokens.get(0).textRange().start();
    TextPointer end = tokens.get(tokens.size() - 1).textRange().end();
    return start.line() + ":" + start.lineOffset() + ":" + end.line() + ":" + end.lineOffset() + " - " +
      tokens.stream().map(Token::text).collect(Collectors.joining(" "));
  }

  public static String token(Token token) {
    TextPointer start = token.textRange().start();
    TextPointer end = token.textRange().end();
    return start.line() + ":" + start.lineOffset() + ":" + end.line() + ":" + end.lineOffset() + " - " +
      token.text();
  }

  public static List<String> methodNames(Class<?> cls) {
    List<String> ignoredMethods = Arrays.asList(
      "children", "descendants", "metaData", "textRange", "wait", "equals", "toString",
      "hashCode", "getClass", "notify", "notifyAll");
    return new ArrayList<>(Stream.of(cls.getMethods())
      .map(Method::getName)
      .filter(name -> !ignoredMethods.contains(name))
      .collect(Collectors.toSet()));
  }

}
