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
import org.sonarsource.slang.api.NativeKind;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import org.junit.jupiter.api.Test;

import static org.sonarsource.slang.utils.SyntacticEquivalence.areEquivalent;
import static org.assertj.core.api.Assertions.assertThat;

class VariableDeclarationTreeImplTest {

  private class TypeNativeKind implements NativeKind {}

  @Test
  void test() {
    TreeMetaData meta = null;
    Tree variableType = new NativeTreeImpl(meta, new TypeNativeKind(), null);
    IdentifierTree identifierTreeX = new IdentifierTreeImpl(meta, "x");
    IdentifierTree identifierTreeY = new IdentifierTreeImpl(meta, "y");
    VariableDeclarationTreeImpl variableTreeX = new VariableDeclarationTreeImpl(meta, identifierTreeX, null,null, false);
    VariableDeclarationTreeImpl variableTreeXCopy = new VariableDeclarationTreeImpl(meta, new IdentifierTreeImpl(meta, "x"), null, null,false);
    VariableDeclarationTreeImpl valueTreeX = new VariableDeclarationTreeImpl(meta, new IdentifierTreeImpl(meta, "x"), null, null, true);
    VariableDeclarationTreeImpl variableTreeXTyped = new VariableDeclarationTreeImpl(meta, identifierTreeX, variableType, null, false);
    VariableDeclarationTreeImpl variableTreeY = new VariableDeclarationTreeImpl(meta, identifierTreeY, variableType, null, false);

    assertThat(variableTreeXTyped.children()).hasSize(2);
    assertThat(variableTreeX.children()).hasSize(1);
    assertThat(variableTreeX.type()).isNull();
    assertThat(variableTreeX.identifier()).isEqualTo(identifierTreeX);
    assertThat(areEquivalent(variableTreeX, variableTreeXCopy)).isTrue();
    assertThat(areEquivalent(variableTreeX, valueTreeX)).isFalse();
    assertThat(areEquivalent(variableTreeX, variableTreeXTyped)).isFalse();
    assertThat(areEquivalent(variableTreeX, variableTreeY)).isFalse();
    assertThat(areEquivalent(variableTreeXTyped, variableTreeY)).isFalse();
  }
}
