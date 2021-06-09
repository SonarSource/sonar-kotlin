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
package org.sonarsource.slang.checks.utils;

/**
 * This enum is used only to distinguish default values for rule parameters. This should be the sole exception in otherwise
 * language agnostic module
 */
public enum Language {
  KOTLIN, RUBY, SCALA, GO;

  public static final String RUBY_NAMING_DEFAULT = "^(@{0,2}[\\da-z_]+[!?=]?)|([*+-/%=!><~]+)|(\\[]=?)$";

  // scala constant starts with upper-case
  public static final String SCALA_NAMING_DEFAULT = "^[_a-zA-Z][a-zA-Z0-9]*$";

  // support function name suffix '_=', '_+', '_!', ... and operators '+', '-', ...
  public static final String SCALA_FUNCTION_OR_OPERATOR_NAMING_DEFAULT = "^([a-z][a-zA-Z0-9]*+(_[^a-zA-Z0-9]++)?+|[^a-zA-Z0-9]++)$";

  public static final String GO_NAMING_DEFAULT = "^(_|[a-zA-Z0-9]+)$";

  /**
   * @deprecated scheduled for removal
   */
  @Deprecated
  public static final String KOTLIN_FUNCTION_NAMING = "^[a-zA-Z][a-zA-Z0-9]*$";

  // Default regex + backticked identifier (used to escape kotlin keywords)
  /**
   * @deprecated scheduled for removal
   */
  @Deprecated
  public static final String KOTLIN_PARAMETERS_AND_VARIABLE_NAMING = "^`?[_a-z][a-zA-Z0-9]*`?$";

  public static final int GO_NESTED_STATEMENT_MAX_DEPTH = 4;
  public static final int GO_MATCH_CASES_DEFAULT_MAX = 6;
  public static final int GO_DEFAULT_MAXIMUM_LINE_LENGTH = 120;
  public static final int GO_DEFAULT_FILE_LINE_MAX = 750;
  public static final int GO_DEFAULT_MAXIMUM_FUNCTION_LENGTH = 120;
}
