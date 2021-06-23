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

import org.sonarsource.slang.api.ClassDeclarationTree;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.NativeKind;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import java.util.Collections;
import org.junit.jupiter.api.Test;

import static org.sonarsource.slang.utils.SyntacticEquivalence.areEquivalent;
import static org.assertj.core.api.Assertions.assertThat;

class ClassDeclarationTreeImplTest {

  private class ClassNativeKind implements NativeKind {}

  @Test
  void test() {
    TreeMetaData meta = null;
    IdentifierTree className = new IdentifierTreeImpl(meta, "MyClass");
    Tree classDecl = new NativeTreeImpl(meta, new ClassNativeKind(), Collections.singletonList(className));
    ClassDeclarationTree tree = new ClassDeclarationTreeImpl(meta, className, classDecl);
    assertThat(tree.children()).hasSize(1);
    assertThat(areEquivalent(tree.children().get(0), classDecl)).isTrue();
    assertThat(areEquivalent(tree.identifier(), className)).isTrue();
  }

}
