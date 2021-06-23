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

import java.nio.file.Path;
import java.nio.file.Paths;
import org.sonarsource.slang.api.ASTConverter;
import org.sonarsource.slang.checks.api.SlangCheck;
import org.sonarsource.slang.parser.SLangConverter;

public class Verifier {

  private static final Path BASE_DIR = Paths.get("src", "test", "resources", "org", "sonarsource", "slang", "checks");
  private static final ASTConverter CONVERTER = new SLangConverter();

  public static void verify(String fileName, SlangCheck check) {
    org.sonarsource.slang.testing.Verifier.verify(CONVERTER, BASE_DIR.resolve(fileName), check);
  }

  public static void verifyNoIssue(String fileName, SlangCheck check) {
    org.sonarsource.slang.testing.Verifier.verifyNoIssue(CONVERTER, BASE_DIR.resolve(fileName), check);
  }

}
