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

import org.sonarsource.slang.api.AssignmentExpressionTree;
import org.sonarsource.slang.api.CatchTree;
import org.sonarsource.slang.api.ParameterTree;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionHandlingTreeImplTest {

  @Test
  void test() {
    TreeMetaData meta = null;
    ParameterTree parameter = new ParameterTreeImpl(meta, new IdentifierTreeImpl(meta, "e"), null);
    Tree lhs = new IdentifierTreeImpl(meta, "x");
    Tree one = new LiteralTreeImpl(meta, "1");
    Tree assignmentExpressionTree =
        new AssignmentExpressionTreeImpl(meta, AssignmentExpressionTree.Operator.EQUAL, lhs, one);
    CatchTreeImpl catchWithIdentifier = new CatchTreeImpl(meta, parameter, assignmentExpressionTree, null);
    CatchTreeImpl catchWithoutIdentifier = new CatchTreeImpl(meta, null, assignmentExpressionTree, null);
    TokenImpl tryToken = new TokenImpl(new TextRangeImpl(1, 0, 1, 3), "try", Token.Type.KEYWORD);

    Tree emptyTry = new BlockTreeImpl(meta, Collections.emptyList());

    List<CatchTree> catchTreeList = Arrays.asList(catchWithIdentifier, catchWithoutIdentifier);

    Tree emptyFinally = new BlockTreeImpl(meta, Collections.emptyList());

    ExceptionHandlingTreeImpl exceptionHandlingTree =
        new ExceptionHandlingTreeImpl(null, emptyTry, tryToken, catchTreeList, emptyFinally);

    assertThat(exceptionHandlingTree.children()).containsExactly(emptyTry, catchWithIdentifier, catchWithoutIdentifier, emptyFinally);
    assertThat(exceptionHandlingTree.tryBlock()).isEqualTo(emptyTry);
    assertThat(exceptionHandlingTree.catchBlocks()).containsExactly(catchWithIdentifier, catchWithoutIdentifier);
    assertThat(exceptionHandlingTree.finallyBlock()).isEqualTo(emptyFinally);
  }

}
