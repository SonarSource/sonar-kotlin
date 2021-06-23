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
package org.sonarsource.slang.api;

import javax.annotation.CheckForNull;

/**
 * The interface used to define conditional expressions.
 *
 * In order to be compatible with most of the languages, the 'IfTree' is not defined 
 * as a statement and should be considered as an expression.
 *
 * A 'IfTree' always has:
 * - a keyword ('if', '?', 'unless', ...)
 * - a condition
 * - a 'then' branch 
 * 
 * Additionally, it's possible to also have:
 * - an 'else' keyword
 * - an 'else' branch (which does not necessarily requires a 'else' keyword)
 * 
 * Known mapping from languages conditional expressions to IfTree:
 * - Apex:   if, ternary (a?b:c)
 * - Kotlin: if
 * - Ruby:   if, ternary (a?b:c), unless (equivalent to 'if not')
 * - Scala:  if
 */
public interface IfTree extends Tree {

  Tree condition();

  Tree thenBranch();

  @CheckForNull
  Tree elseBranch();

  Token ifKeyword();

  @CheckForNull
  Token elseKeyword();
}
