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
package org.sonarsource.slang.checks;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarsource.slang.checks.CheckList.ALL_CHECKS_WITH_LANGUAGE_CONFIG;

class CheckListTest {

  @Test
  void all_checks_should_be_present() {
    File directory = new File("src/main/java/org/sonarsource/slang/checks");
    File[] checkFiles = directory.listFiles((dir, name) ->
        name.endsWith("Check.java") && !name.startsWith("Abstract"));
    assertThat(CheckList.allChecks().size() + ALL_CHECKS_WITH_LANGUAGE_CONFIG.length).isEqualTo(checkFiles.length);
  }

  @Test
  void exclude_checks() {
    List<Class<?>> allChecks = CheckList.allChecks();
    assertThat(allChecks.size()).isGreaterThanOrEqualTo(40);

    List<Class<?>> includedChecks = CheckList.excludeChecks(new Class[] {AllBranchesIdenticalCheck.class});
    assertThat(includedChecks.size()).isEqualTo(allChecks.size() - 1);
  }

  @Test
  void configured_checks() {
    List<Class<?>> checksWithConfig = Arrays.asList(ALL_CHECKS_WITH_LANGUAGE_CONFIG);
    assertThat(checksWithConfig).hasSize(1);

    Set<Class<?>> allChecks = new HashSet<>(CheckList.allChecks());
    allChecks.removeAll(checksWithConfig);
    assertThat(allChecks).hasSize(CheckList.allChecks().size());
  }

}
