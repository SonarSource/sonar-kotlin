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
package org.sonarsource.slang.checks.api;

import java.util.List;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.sonarsource.slang.api.HasTextRange;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.visitors.TreeContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CheckContextTest {

  @Test
  void parent_default_method() {
    Tree a = mock(Tree.class);
    Tree b = mock(Tree.class);
    Tree c = mock(Tree.class);

    CheckContextToTestDefaultMethod context = new CheckContextToTestDefaultMethod();
    assertThat(context.parent()).isNull();

    context.enter(a);
    assertThat(context.parent()).isNull();

    context.enter(b);
    assertThat(context.parent()).isSameAs(a);

    context.enter(c);
    assertThat(context.parent()).isSameAs(b);

    context.leave(c);
    assertThat(context.parent()).isSameAs(a);

    context.leave(b);
    assertThat(context.parent()).isNull();
  }

  private static class CheckContextToTestDefaultMethod extends TreeContext implements CheckContext {

    public String filename() {
      return null;
    }

    public String fileContent() {
      return null;
    }

    public void reportIssue(TextRange textRange, String message) {
    }

    public void reportIssue(HasTextRange toHighlight, String message) {
    }

    public void reportIssue(HasTextRange toHighlight, String message, SecondaryLocation secondaryLocation) {
    }

    public void reportIssue(HasTextRange toHighlight, String message, List<SecondaryLocation> secondaryLocations) {
    }

    public void reportIssue(HasTextRange toHighlight, String message, List<SecondaryLocation> secondaryLocations, @Nullable Double gap) {
    }

    public void reportFileIssue(String message) {
    }

    public void reportFileIssue(String message, @Nullable Double gap) {
    }

  }

}
