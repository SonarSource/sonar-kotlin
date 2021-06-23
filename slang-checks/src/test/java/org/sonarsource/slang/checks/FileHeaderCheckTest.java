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

import org.junit.jupiter.api.Test;

class FileHeaderCheckTest {
  private FileHeaderCheck check = new FileHeaderCheck();
  @Test
  void test() {
    check.headerFormat = "// copyright 2018";
    Verifier.verify("fileheader/Noncompliant.slang", check);
    Verifier.verifyNoIssue("fileheader/Compliant.slang", check);
  }

  @Test
  void test_regex() {
    check.headerFormat = "// copyright 20\\d\\d";
    check.isRegularExpression = true;
    Verifier.verify("fileheader/Noncompliant.slang", check);
    Verifier.verifyNoIssue("fileheader/Compliant.slang", check);
  }
  @Test
  void test_multiline() {
    check.headerFormat = "/*\n" +
      " * SonarSource SLang\n" +
      " * Copyright (C) 1999-2001 SonarSource SA\n" +
      " * mailto:info AT sonarsource DOT com\n" +
      " */";
    Verifier.verifyNoIssue("fileheader/Multiline.slang", check);
  }

  @Test
  void test_no_first_line() {
    check.headerFormat = "// copyright 20\\d\\d";
    check.isRegularExpression = true;
    Verifier.verify("fileheader/NoFirstLine.slang", check);
  }
}
