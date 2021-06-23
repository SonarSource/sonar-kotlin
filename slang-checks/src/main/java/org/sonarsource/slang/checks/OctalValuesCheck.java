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

import java.math.BigInteger;
import org.sonar.check.Rule;
import org.sonarsource.slang.api.IntegerLiteralTree;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SlangCheck;

import static org.sonarsource.slang.api.IntegerLiteralTree.Base.OCTAL;

@Rule(key = "S1314")
public class OctalValuesCheck implements SlangCheck {

  private static final String MESSAGE = "Use decimal values instead of octal ones.";
  private static final BigInteger EIGHT = BigInteger.valueOf(OCTAL.getRadix());
  private static final int FILE_PERMISSION_MASK_LENGTH = 3;

  @Override
  public void initialize(InitContext init) {
    init.register(IntegerLiteralTree.class, (ctx, literal) -> {
      if (literal.getBase() == OCTAL && !isException(literal)) {
        ctx.reportIssue(literal, MESSAGE);
      }
    });
  }

  private static boolean isException(IntegerLiteralTree literalTree) {
    // octal literal < 8 are authorized, as well as octal literals with 3 digits, as they are often used for file permissions
    BigInteger value = literalTree.getIntegerValue();
    return value.compareTo(EIGHT) < 0 || literalTree.getNumericPart().length() == FILE_PERMISSION_MASK_LENGTH;
  }

}
