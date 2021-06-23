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
import java.util.List;
import org.sonarsource.slang.api.FunctionInvocationTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;

public class FunctionInvocationTreeImpl extends BaseTreeImpl implements FunctionInvocationTree {

  private final Tree memberSelect;
  private final List<Tree> arguments;

  public FunctionInvocationTreeImpl(TreeMetaData metaData, Tree memberSelect, List<Tree> arguments) {
    super(metaData);
    this.memberSelect = memberSelect;
    this.arguments = arguments;
  }


  @Override
  public List<Tree> arguments() {
    return arguments;
  }

  @Override
  public Tree memberSelect() {
    return memberSelect;
  }

  @Override
  public List<Tree> children() {
    List<Tree> children = new ArrayList<>();
    children.add(memberSelect);
    children.addAll(arguments);
    return children;
  }
}
