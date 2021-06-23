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

import java.util.ArrayList;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonarsource.slang.api.LoopTree;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;

import java.util.List;

public class LoopTreeImpl extends BaseTreeImpl implements LoopTree {

  private final Tree condition;
  private final Tree body;
  private final LoopKind kind;
  private final Token keyword;

  public LoopTreeImpl(TreeMetaData metaData, @Nullable Tree condition, Tree body, LoopKind kind, Token keyword) {
    super(metaData);
    this.condition = condition;
    this.body = body;
    this.kind = kind;
    this.keyword = keyword;

  }

  @CheckForNull
  @Override
  public Tree condition() {
    return condition;
  }

  @Override
  public Tree body() {
    return body;
  }

  @Override
  public LoopKind kind() {
    return kind;
  }

  @Override
  public Token keyword() {
    return keyword;
  }

  @Override
  public List<Tree> children() {
    List<Tree> children = new ArrayList<>();
    if (condition != null) {
      children.add(condition);
    }
    children.add(body);
    return children;
  }
}
