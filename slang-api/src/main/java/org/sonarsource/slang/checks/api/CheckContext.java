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

import org.sonarsource.slang.api.HasTextRange;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.api.Tree;
import java.util.Deque;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public interface CheckContext {

  Deque<Tree> ancestors();

  @CheckForNull
  default Tree parent() {
    if (this.ancestors().isEmpty()) {
      return null;
    } else {
      return this.ancestors().peek();
    }
  }

  String filename();

  String fileContent();

  void reportIssue(TextRange textRange, String message);

  void reportIssue(HasTextRange toHighlight, String message);

  void reportIssue(HasTextRange toHighlight, String message, SecondaryLocation secondaryLocation);

  void reportIssue(HasTextRange toHighlight, String message, List<SecondaryLocation> secondaryLocations);

  void reportIssue(HasTextRange toHighlight, String message, List<SecondaryLocation> secondaryLocations, @Nullable Double gap);

  void reportFileIssue(String message);

  void reportFileIssue(String message, @Nullable Double gap);
}
