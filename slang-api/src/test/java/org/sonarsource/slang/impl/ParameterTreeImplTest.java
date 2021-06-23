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

import java.util.Collections;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.ModifierTree;
import org.sonarsource.slang.api.NativeKind;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import org.junit.jupiter.api.Test;

import static org.sonarsource.slang.utils.SyntacticEquivalence.areEquivalent;
import static org.assertj.core.api.Assertions.assertThat;

class ParameterTreeImplTest {

  private class TypeNativeKind implements NativeKind {}

  @Test
  void test() {
    TreeMetaData meta = null;
    Tree parameterType = new NativeTreeImpl(meta, new TypeNativeKind(), null);
    IdentifierTree identifierTreeX = new IdentifierTreeImpl(meta, "x");
    IdentifierTree identifierTreeY = new IdentifierTreeImpl(meta, "y");
    ParameterTreeImpl parameterTreeX = new ParameterTreeImpl(meta, identifierTreeX, null);
    ParameterTreeImpl parameterTreeXCopy = new ParameterTreeImpl(meta, new IdentifierTreeImpl(meta, "x"), null);
    ParameterTreeImpl parameterTreeXTyped = new ParameterTreeImpl(meta, identifierTreeX, parameterType);
    ParameterTreeImpl parameterTreeY = new ParameterTreeImpl(meta, identifierTreeY, parameterType);

    assertThat(parameterTreeXTyped.children()).hasSize(2);
    assertThat(parameterTreeX.children()).hasSize(1);
    assertThat(parameterTreeX.type()).isNull();
    assertThat(parameterTreeX.identifier()).isEqualTo(identifierTreeX);
    assertThat(areEquivalent(parameterTreeX, parameterTreeXCopy)).isTrue();
    assertThat(areEquivalent(parameterTreeX, parameterTreeXTyped)).isFalse();
    assertThat(areEquivalent(parameterTreeX, parameterTreeY)).isFalse();
    assertThat(areEquivalent(parameterTreeXTyped, parameterTreeY)).isFalse();
  }

  @Test
  void test_default_value() {
    TreeMetaData meta = null;
    IdentifierTree identifierTreeX = new IdentifierTreeImpl(meta, "x");
    IdentifierTree identifierTreeY = new IdentifierTreeImpl(meta, "y");
    IdentifierTree defaultValue1 = new IdentifierTreeImpl(meta, "1");
    ParameterTreeImpl parameterTreeXDefault1 = new ParameterTreeImpl(meta, identifierTreeX, null, defaultValue1);
    ParameterTreeImpl parameterTreeXDefault1Copy = new ParameterTreeImpl(meta, new IdentifierTreeImpl(meta, "x"), null, new IdentifierTreeImpl(meta, "1"));
    ParameterTreeImpl parameterTreeXDefault2 = new ParameterTreeImpl(meta, identifierTreeX, null, new IdentifierTreeImpl(meta, "2"));
    ParameterTreeImpl parameterTreeXDefaultNative = new ParameterTreeImpl(meta, identifierTreeX, null, new NativeTreeImpl(meta, new TypeNativeKind(), null));
    ParameterTreeImpl parameterTreeY = new ParameterTreeImpl(meta, identifierTreeY, null);


    assertThat(parameterTreeXDefault1.children()).hasSize(2);
    assertThat(parameterTreeXDefault2.children()).hasSize(2);
    assertThat(parameterTreeXDefaultNative.children()).hasSize(2);
    assertThat(parameterTreeY.children()).hasSize(1);
    assertThat(parameterTreeXDefault1.defaultValue()).isEqualTo(defaultValue1);
    assertThat(parameterTreeY.defaultValue()).isNull();

    assertThat(areEquivalent(parameterTreeXDefault1, parameterTreeXDefault1Copy)).isTrue();
    assertThat(areEquivalent(parameterTreeXDefault1, parameterTreeXDefault2)).isFalse();
    assertThat(areEquivalent(parameterTreeXDefault1, parameterTreeXDefaultNative)).isFalse();
    assertThat(areEquivalent(parameterTreeXDefault1, parameterTreeY)).isFalse();
  }

  @Test
  void test_modifiers() {
    TreeMetaData meta = null;
    IdentifierTree identifierTreeX = new IdentifierTreeImpl(meta, "x");
    IdentifierTree identifierTreeY = new IdentifierTreeImpl(meta, "y");
    Tree publicModifier = new ModifierTreeImpl(meta, ModifierTree.Kind.PUBLIC);

    ParameterTreeImpl parameterTreeXPublic = new ParameterTreeImpl(meta, identifierTreeX, null, null,
        Collections.singletonList(publicModifier));
    ParameterTreeImpl parameterTreeXPublicCopy = new ParameterTreeImpl(meta, new IdentifierTreeImpl(meta, "x"),
        null, null, Collections.singletonList(new ModifierTreeImpl(meta, ModifierTree.Kind.PUBLIC)));
    ParameterTreeImpl parameterTreeXPrivate = new ParameterTreeImpl(meta, identifierTreeX,
        null, null, Collections.singletonList(new ModifierTreeImpl(meta, ModifierTree.Kind.PRIVATE)));
    ParameterTreeImpl parameterTreeXNative = new ParameterTreeImpl(meta, identifierTreeX, null, null,
        Collections.singletonList(new NativeTreeImpl(meta, new TypeNativeKind(), null)));
    ParameterTreeImpl parameterTreeNoMod = new ParameterTreeImpl(meta, identifierTreeY, null);


    assertThat(parameterTreeXPublic.children()).hasSize(2);
    assertThat(parameterTreeXPrivate.children()).hasSize(2);
    assertThat(parameterTreeXNative.children()).hasSize(2);
    assertThat(parameterTreeNoMod.children()).hasSize(1);
    assertThat(parameterTreeXPublic.modifiers()).hasSize(1);
    assertThat(parameterTreeXPublic.modifiers().get(0)).isEqualTo(publicModifier);
    assertThat(parameterTreeNoMod.modifiers()).isEmpty();

    assertThat(areEquivalent(parameterTreeXPublic, parameterTreeXPublicCopy)).isTrue();
    assertThat(areEquivalent(parameterTreeXPublic, parameterTreeXPrivate)).isFalse();
    assertThat(areEquivalent(parameterTreeXPublic, parameterTreeXNative)).isFalse();
    assertThat(areEquivalent(parameterTreeXPublic, parameterTreeNoMod)).isFalse();
  }

}
