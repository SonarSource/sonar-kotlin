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

import org.sonarsource.slang.api.ParenthesizedExpressionTree;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import java.util.Collections;
import java.util.List;

public class ParenthesizedExpressionTreeImpl extends BaseTreeImpl implements ParenthesizedExpressionTree {

  private final Tree expression;
  private final Token leftParenthesis;
  private final Token rightParenthesis;

  public ParenthesizedExpressionTreeImpl(TreeMetaData metaData, Tree expression, Token leftParenthesis, Token rightParenthesis) {
    super(metaData);
    this.expression = expression;
    this.leftParenthesis = leftParenthesis;
    this.rightParenthesis = rightParenthesis;
  }

  @Override
  public Tree expression() {
    return expression;
  }

  @Override
  public Token leftParenthesis() {
    return leftParenthesis;
  }

  @Override
  public Token rightParenthesis() {
    return rightParenthesis;
  }

  @Override
  public List<Tree> children() {
    return Collections.singletonList(expression);
  }

}
