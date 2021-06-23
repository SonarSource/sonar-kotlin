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
package org.sonarsource.kotlin.plugin;

import java.util.List;
import java.util.stream.Collectors;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.sonarsource.slang.testing.PackageScanner;

import static org.assertj.core.api.Java6Assertions.assertThat;

class KotlinCheckListTest {

  private static final String KOTLIN_CHECKS_PACKAGE = "org.sonarsource.kotlin.checks";

  @Test
  void kotlin_checks_size() {
    Assertions.assertThat(KotlinCheckList.checks().size()).isGreaterThanOrEqualTo(40);
  }

  @Test
  void kotlin_specific_checks_are_added_to_check_list() {
    List<String> languageImplementation = PackageScanner.findSlangChecksInPackage(KOTLIN_CHECKS_PACKAGE);

    List<String> checkListNames = KotlinCheckList.checks().stream().map(Class::getName).collect(Collectors.toList());
    List<String> kotlinSpecificCheckList = KotlinCheckList.KOTLIN_LANGUAGE_SPECIFIC_CHECKS.stream().map(Class::getName).collect(Collectors.toList());

    for (String languageCheck : languageImplementation) {
      assertThat(checkListNames).contains(languageCheck);
      assertThat(kotlinSpecificCheckList).contains(languageCheck);
      assertThat(languageCheck).endsWith("KotlinCheck");
    }
  }

  @Test
  void kotlin_excluded_not_present() {
    List<Class<?>> checks = KotlinCheckList.checks();
    for (Class excluded : KotlinCheckList.KOTLIN_CHECK_BLACK_LIST) {
      assertThat(checks).doesNotContain(excluded);
    }
  }

  @Test
  void kotlin_included_are_present() {
    List<Class<?>> checks = KotlinCheckList.checks();
    for (Class specificCheck : KotlinCheckList.KOTLIN_LANGUAGE_SPECIFIC_CHECKS) {
      assertThat(checks).contains(specificCheck);
    }
  }
}
