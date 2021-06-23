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
import java.util.Collections;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.ParameterTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class ParameterTreeImpl extends BaseTreeImpl implements ParameterTree {

  private final IdentifierTree identifier;
  private final Tree type;
  private final Tree defaultValue;
  private final List<Tree> modifiers;

  public ParameterTreeImpl(TreeMetaData metaData, IdentifierTree identifier, @Nullable Tree type, @Nullable Tree defaultValue, List<Tree> modifiers) {
    super(metaData);
    this.identifier = identifier;
    this.type = type;
    this.defaultValue = defaultValue;
    this.modifiers = modifiers;
  }

  public ParameterTreeImpl(TreeMetaData metaData, IdentifierTree identifier, @Nullable Tree type, @Nullable Tree defaultValue) {
    this(metaData, identifier, type, defaultValue, Collections.emptyList());
  }

  public ParameterTreeImpl(TreeMetaData metaData, IdentifierTree identifier, @Nullable Tree type) {
    this(metaData, identifier, type, null);
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
  public Tree defaultValue() {
    return defaultValue;
  }

  @Override
  public List<Tree> modifiers() {
    return modifiers;
  }

  @Override
  public List<Tree> children() {
    List<Tree> children = new ArrayList<>();
    children.addAll(modifiers);
    children.add(identifier);
    if (type != null) {
      children.add(type);
    }
    if (defaultValue != null) {
      children.add(defaultValue);
    }
    return children;
  }

}
