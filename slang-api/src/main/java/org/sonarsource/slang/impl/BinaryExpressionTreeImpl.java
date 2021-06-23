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

import org.sonarsource.slang.api.BinaryExpressionTree;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import java.util.Arrays;
import java.util.List;

public class BinaryExpressionTreeImpl extends BaseTreeImpl implements BinaryExpressionTree {

  private final Operator operator;
  private final Token operatorToken;
  private final Tree leftOperand;
  private final Tree rightOperand;

  public BinaryExpressionTreeImpl(TreeMetaData metaData, Operator operator, Token operatorToken, Tree leftOperand, Tree rightOperand) {
    super(metaData);
    this.operator = operator;
    this.operatorToken = operatorToken;

    this.leftOperand = leftOperand;
    this.rightOperand = rightOperand;
  }

  @Override
  public Operator operator() {
    return operator;
  }

  @Override
  public Token operatorToken() {
    return operatorToken;
  }

  @Override
  public Tree leftOperand() {
    return leftOperand;
  }

  @Override
  public Tree rightOperand() {
    return rightOperand;
  }

  @Override
  public List<Tree> children() {
    return Arrays.asList(leftOperand, rightOperand);
  }
}
