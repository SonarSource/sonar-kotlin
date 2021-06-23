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
package org.sonarsource.slang.persistence.conversion;

import java.util.NoSuchElementException;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.sonarsource.slang.api.TextPointer;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import org.sonarsource.slang.impl.TextRangeImpl;
import org.sonarsource.slang.impl.TreeMetaDataProvider;

public final class RangeConverter {

  private RangeConverter() {
  }

  @Nullable
  public static String format(@Nullable TextRange range) {
    if (range == null) {
      return null;
    }
    TextPointer start = range.start();
    TextPointer end = range.end();

    String endLine = start.line() == end.line() ? "" : Integer.toString(end.line());
    return start.line() + ":" + start.lineOffset() + ":" + endLine + ":" + end.lineOffset();
  }

  @Nullable
  public static TextRange parse(@Nullable String value) {
    if (value == null) {
      return null;
    }
    String[] values = value.split(":", 4);
    if (values.length != 4) {
      throw new IllegalArgumentException("Invalid TextRange '" + value + "'");
    }
    int startLine = Integer.parseInt(values[0]);
    int startLineOffset = Integer.parseInt(values[1]);
    int endLine = values[2].isEmpty() ? startLine : Integer.parseInt(values[2]);
    int endLineOffset = Integer.parseInt(values[3]);
    return new TextRangeImpl(startLine, startLineOffset, endLine, endLineOffset);
  }

  @Nullable
  public static String tokenReference(@Nullable Token token) {
    if (token == null) {
      return null;
    }
    return format(token.textRange());
  }

  @Nullable
  public static Token resolveToken(TreeMetaDataProvider metaDataProvider, @Nullable String tokenReference) {
    TextRange range = parse(tokenReference);
    if (range == null) {
      return null;
    }
    return metaDataProvider.firstToken(range)
      .orElseThrow(() -> new NoSuchElementException("Token not found: " + tokenReference));
  }

  public static String metaDataReference(Tree tree) {
    return format(tree.metaData().textRange());
  }

  public static TreeMetaData resolveMetaData(TreeMetaDataProvider metaDataProvider, String metaDataReference) {
    return metaDataProvider.metaData(parse(metaDataReference));
  }

  @Nullable
  public static String treeReference(@Nullable Tree tree) {
    if (tree == null) {
      return null;
    }
    return format(tree.metaData().textRange());
  }

  @Nullable
  public static <T extends Tree> T resolveNullableTree(Tree parent, @Nullable String treeReference, Class<T> childClass) {
    if (treeReference == null) {
      return null;
    }
    TextRange range = parse(treeReference);
    return Stream.concat(Stream.of(parent), parent.descendants())
      .filter(child -> child.textRange().equals(range))
      .filter(childClass::isInstance)
      .map(childClass::cast)
      .findFirst()
      .orElse(null);
  }

}
