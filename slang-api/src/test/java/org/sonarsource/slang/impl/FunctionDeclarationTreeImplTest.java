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

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonarsource.slang.api.BlockTree;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.NativeKind;
import org.sonarsource.slang.api.NativeTree;
import org.sonarsource.slang.api.ParameterTree;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarsource.slang.api.ModifierTree.Kind.PUBLIC;
import static org.sonarsource.slang.impl.TextRanges.range;
import static org.sonarsource.slang.utils.TreeCreationUtils.identifier;
import static org.sonarsource.slang.utils.TreeCreationUtils.simpleNative;

class FunctionDeclarationTreeImplTest {
  private static final NativeKind SIMPLE_KIND = new NativeKind() {
  };

  @Test
  void test() {
    TreeMetaData meta = null;
    List<Tree> modifiers = Arrays.asList(new ModifierTreeImpl(meta, PUBLIC));
    Tree returnType = new IdentifierTreeImpl(meta, "int");
    IdentifierTree name = new IdentifierTreeImpl(meta, "foo");
    IdentifierTree paramName = new IdentifierTreeImpl(meta, "p1");
    ParameterTree param = new ParameterTreeImpl(meta, paramName, null);
    List<Tree> params = singletonList(param);
    BlockTree body = new BlockTreeImpl(meta, emptyList());
    IdentifierTree child = identifier("GENERIC");
    NativeTree nativeChildren = simpleNative(SIMPLE_KIND, singletonList(child));

    FunctionDeclarationTreeImpl tree =
      new FunctionDeclarationTreeImpl(meta, modifiers, false, returnType, name, params, body, singletonList(nativeChildren));
    assertThat(tree.children()).hasSize(6);
    assertThat(tree.children()).containsExactly(modifiers.get(0), returnType, name, param, body, nativeChildren);
    assertThat(tree.modifiers()).isEqualTo(modifiers);
    assertThat(tree.returnType()).isEqualTo(returnType);
    assertThat(tree.name()).isEqualTo(name);
    assertThat(tree.formalParameters()).isEqualTo(params);
    assertThat(tree.body()).isEqualTo(body);
    assertThat(tree.nativeChildren()).isEqualTo(singletonList(nativeChildren));
    assertThat(tree.isConstructor()).isFalse();

    FunctionDeclarationTreeImpl lightweightConstructor = new FunctionDeclarationTreeImpl(meta, modifiers, true, null, null, emptyList(), null, emptyList());

    assertThat(lightweightConstructor.children()).containsExactly(modifiers.get(0));
    assertThat(lightweightConstructor.nativeChildren()).isEmpty();
    assertThat(lightweightConstructor.isConstructor()).isTrue();
  }

  @Test
  void rangeToHighlight_with_name() {
    TreeMetaDataProvider metaDataProvider = new TreeMetaDataProvider(emptyList(), emptyList());
    TreeMetaData nameMetaData = metaDataProvider.metaData(range(1, 2, 3, 4));
    TreeMetaData bodyMetaData = metaDataProvider.metaData(range(5, 1, 5, 7));
    IdentifierTree name = new IdentifierTreeImpl(nameMetaData, "foo");
    BlockTree body = new BlockTreeImpl(bodyMetaData, emptyList());
    assertThat(new FunctionDeclarationTreeImpl(body.metaData(), emptyList(), false, null, name, emptyList(), body, emptyList()).rangeToHighlight())
      .isEqualTo(nameMetaData.textRange());
  }

  @Test
  void rangeToHighlight_with_body_only() {
    TreeMetaDataProvider metaDataProvider = new TreeMetaDataProvider(emptyList(), Arrays.asList(
      new TokenImpl(range(5, 1, 17, 18), "{", Token.Type.OTHER),
      new TokenImpl(range(5, 1, 19, 20), "}", Token.Type.OTHER)));
    TreeMetaData bodyMetaData = metaDataProvider.metaData(range(5, 1, 17, 20));
    BlockTree body = new BlockTreeImpl(bodyMetaData, emptyList());
    assertThat(new FunctionDeclarationTreeImpl(body.metaData(), emptyList(), false, null, null, emptyList(), body, emptyList()).rangeToHighlight())
      .isEqualTo(body.metaData().textRange());
  }

  @Test
  void rangeToHighlight_with_no_name_but_some_signature() {
    TreeMetaDataProvider metaDataProvider = new TreeMetaDataProvider(emptyList(), Arrays.asList(
      new TokenImpl(range(5, 1, 5, 10), "fun", Token.Type.KEYWORD),
      new TokenImpl(range(5, 11, 5, 15), "foo", Token.Type.OTHER),
      new TokenImpl(range(5, 17, 5, 18), "{", Token.Type.OTHER),
      new TokenImpl(range(5, 19, 5, 20), "}", Token.Type.OTHER)));
    TreeMetaData functionMetaData = metaDataProvider.metaData(range(5, 1, 5, 20));
    TreeMetaData bodyMetaData = metaDataProvider.metaData(range(5, 17, 5, 20));
    BlockTree body = new BlockTreeImpl(bodyMetaData, emptyList());
    assertThat(new FunctionDeclarationTreeImpl(functionMetaData, emptyList(), false, null, null, emptyList(), body, emptyList()).rangeToHighlight())
      .isEqualTo(range(5, 1, 5, 15));
  }

  @Test
  void rangeToHighlight_with_no_name_no_body_but_some_signature() {
    TreeMetaDataProvider metaDataProvider = new TreeMetaDataProvider(emptyList(), Arrays.asList(
      new TokenImpl(range(5, 1, 5, 10), "fun", Token.Type.KEYWORD),
      new TokenImpl(range(5, 11, 5, 12), "(", Token.Type.OTHER),
      new TokenImpl(range(5, 13, 5, 14), ")", Token.Type.OTHER)));
    TreeMetaData functionMetaData = metaDataProvider.metaData(range(5, 1, 5, 14));
    assertThat(new FunctionDeclarationTreeImpl(functionMetaData, emptyList(), false, null, null, emptyList(), null, emptyList()).rangeToHighlight())
      .isEqualTo(range(5, 1, 5, 14));
  }

}
