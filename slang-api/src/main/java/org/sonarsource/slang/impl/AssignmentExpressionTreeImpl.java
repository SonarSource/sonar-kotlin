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

import org.sonarsource.slang.api.AssignmentExpressionTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import java.util.Arrays;
import java.util.List;

public class AssignmentExpressionTreeImpl extends BaseTreeImpl implements AssignmentExpressionTree {

  private final Operator operator;
  private final Tree leftHandSide;
  private final Tree statementOrExpression;

  public AssignmentExpressionTreeImpl(TreeMetaData metaData, Operator operator, Tree leftHandSide, Tree statementOrExpression) {
    super(metaData);
    this.operator = operator;
    this.leftHandSide = leftHandSide;
    this.statementOrExpression = statementOrExpression;
  }

  @Override
  public Operator operator() {
    return operator;
  }

  @Override
  public Tree leftHandSide() {
    return leftHandSide;
  }

  @Override
  public Tree statementOrExpression() {
    return statementOrExpression;
  }

  @Override
  public List<Tree> children() {
    return Arrays.asList(leftHandSide, statementOrExpression);
  }
}
