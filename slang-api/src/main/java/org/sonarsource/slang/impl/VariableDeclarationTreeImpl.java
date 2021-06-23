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

import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import org.sonarsource.slang.api.VariableDeclarationTree;


import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class VariableDeclarationTreeImpl extends BaseTreeImpl implements VariableDeclarationTree {

  private final IdentifierTree identifier;
  private final Tree type;
  private final Tree initializer;
  private final boolean isVal;

  public VariableDeclarationTreeImpl(TreeMetaData metaData, IdentifierTree identifier, @Nullable Tree type, @Nullable Tree initializer, boolean isVal) {
    super(metaData);
    this.identifier = identifier;
    this.type = type;
    this.initializer = initializer;
    this.isVal = isVal;
  }

  @Override
  public IdentifierTree identifier() {
    return identifier;
  }

  @CheckForNull
  @Override
  public Tree type() {
    return type;
  }

  @CheckForNull
  @Override
  public Tree initializer() {
    return initializer;
  }

  @Override
  public boolean isVal() {
    return isVal;
  }

  @Override
  public List<Tree> children() {
    List<Tree> children = new ArrayList<>();
    children.add(identifier);

    if (type != null) {
      children.add(type);
    }

    if (initializer != null) {
      children.add(initializer);
    }

    return children;
  }

}
