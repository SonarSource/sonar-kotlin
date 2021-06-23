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

import org.sonarsource.slang.api.MatchCaseTree;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class MatchCaseTreeImpl extends BaseTreeImpl implements MatchCaseTree {

  private final Tree expression;
  private final Tree body;

  public MatchCaseTreeImpl(TreeMetaData metaData, @Nullable Tree expression, @Nullable Tree body) {
    super(metaData);
    this.expression = expression;
    this.body = body;
  }

  @CheckForNull
  @Override
  public Tree expression() {
    return expression;
  }

  @CheckForNull
  @Override
  public Tree body() {
    return body;
  }

  @Override
  public TextRange rangeToHighlight() {
    if (body == null) {
      return textRange();
    }

    TextRange bodyRange = body.metaData().textRange();
    List<TextRange> tokenRangesBeforeBody = metaData().tokens().stream()
      .map(Token::textRange)
      .filter(t -> t.start().compareTo(bodyRange.start()) < 0)
      .collect(Collectors.toList());

    // for ruby when body is empty, "when expr" is body meta, so there is nothing before
    if (tokenRangesBeforeBody.isEmpty()) {
      return bodyRange;
    }
    return TextRanges.merge(tokenRangesBeforeBody);
  }

  @Override
  public List<Tree> children() {
    List<Tree> children = new ArrayList<>();
    if (expression != null) {
      children.add(expression);
    }
    if (body != null) {
      children.add(body);
    }
    return children;
  }
}
