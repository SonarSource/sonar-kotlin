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

import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import org.sonarsource.slang.api.UnaryExpressionTree;
import java.util.Collections;
import java.util.List;

public class UnaryExpressionTreeImpl extends BaseTreeImpl implements UnaryExpressionTree {

  private final Operator operator;
  private final Tree operand;

  public UnaryExpressionTreeImpl(TreeMetaData metaData, Operator operator, Tree operand) {
    super(metaData);
    this.operator = operator;
    this.operand = operand;
  }

  @Override
  public Operator operator() {
    return operator;
  }

  @Override
  public Tree operand() {
    return operand;
  }

  @Override
  public List<Tree> children() {
    return Collections.singletonList(operand);
  }

}
