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

import org.sonarsource.slang.api.IfTree;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class IfTreeImpl extends BaseTreeImpl implements IfTree {

  private final Tree condition;
  private final Tree thenBranch;
  private final Tree elseBranch;
  private final Token ifKeyword;
  private final Token elseKeyword;

  public IfTreeImpl(
    TreeMetaData metaData,
    Tree condition,
    Tree thenBranch,
    @Nullable Tree elseBranch,
    Token ifKeyword,
    @Nullable Token elseKeyword) {
    super(metaData);
    this.condition = condition;
    this.thenBranch = thenBranch;
    this.elseBranch = elseBranch;
    this.ifKeyword = ifKeyword;
    this.elseKeyword = elseKeyword;
  }

  @Override
  public Tree condition() {
    return condition;
  }

  @Override
  public Tree thenBranch() {
    return thenBranch;
  }

  @CheckForNull
  @Override
  public Tree elseBranch() {
    return elseBranch;
  }

  @Override
  public Token ifKeyword() {
    return ifKeyword;
  }

  @CheckForNull
  @Override
  public Token elseKeyword() {
    return elseKeyword;
  }

  @Override
  public List<Tree> children() {
    List<Tree> children = new ArrayList<>();
    children.add(condition);
    children.add(thenBranch);
    if (elseBranch != null) {
      children.add(elseBranch);
    }
    return children;
  }
}
