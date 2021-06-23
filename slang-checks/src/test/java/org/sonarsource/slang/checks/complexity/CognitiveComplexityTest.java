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
package org.sonarsource.slang.checks.complexity;

import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.parser.SLangConverter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CognitiveComplexityTest {

  private SLangConverter parser = new SLangConverter();

  @Test
  void unrelated_statement() {
    assertThat(complexity("42;").value()).isZero();
  }

  @Test
  void if_statements() {
    assertThat(complexity("if (x) { 42 };").value()).isEqualTo(1);
    assertThat(complexity("if (x) { 42 } else { 43 };").value()).isEqualTo(2);
    assertThat(complexity("if (x) { 42 } else if (y) { 43 };").value()).isEqualTo(2);
    assertThat(complexity("if (x) { 42 } else if (y) { 43 } else { 44 };").value()).isEqualTo(3);
  }

  @Test
  void ternary_operator() {
    assertThat(complexity("v = if (x) 42 else 43;").value()).isEqualTo(1);
    assertThat(complexity("v = if (x) 42 else if (y) 43 else 44;").value()).isEqualTo(3);
    assertThat(complexity("v = if (x) (if (y) 42 else 43) else 44;").value()).isEqualTo(3);
    assertThat(complexity("v = if (x) 42 else (if (y) 43 else 44);").value()).isEqualTo(3);
    assertThat(complexity("v = if (x) (if (y) 42 else 43) else (if (y) 44 else 45);").value()).isEqualTo(5);
    assertThat(complexity("v = if (x) if (y) if (z) 42 else 43 else 44 else 45;").value()).isEqualTo(6);
    assertThat(complexity("v = if (x) (if (y) (if (z) 42 else 43) else 44) else 45;").value()).isEqualTo(6);
  }

  @Test
  void nested_if_statements() {
    assertThat(complexity("if (x) { 42 };").value()).isEqualTo(1);
    assertThat(complexity("if (x) { 42 } else { 43 };").value()).isEqualTo(2);
    assertThat(complexity("if (x) { 42 } else if (y) { 43 };").value()).isEqualTo(2);
    assertThat(complexity("if (x) { 42 } else if (y) { if (y) { 43 } else { 44 } };").value()).isEqualTo(5);
    assertThat(complexity("if (x) { 42 } else if (y) { 43 } else { 44 };").value()).isEqualTo(3);
    assertThat(complexity("if (x) { 42 } else if (y) { 43 } else { if (y) { 44 } else { 45 } };").value()).isEqualTo(6);

  }

  @Test
  void loop_statements() {
    assertThat(complexity("while (x) { 42 };").value()).isEqualTo(1);
  }

  @Test
  void match_statements() {
    assertThat(complexity("match (x) { else -> 42; };").value()).isEqualTo(1);
    assertThat(complexity("match (x) { 'a' -> 0; else -> 42; };").value()).isEqualTo(1);
  }

  @Test
  void try_catch_statements() {
    assertThat(complexity("try { foo; };").value()).isZero();
    assertThat(complexity("try { foo; } catch (e1) { bar; };").value()).isEqualTo(1);
    assertThat(complexity("try { foo; } catch (e1) { bar; } catch (e2) { baz; };").value()).isEqualTo(2);
    assertThat(complexity("try { foo; } finally { bar; };").value()).isZero();
  }

  @Test
  void functions() {
    assertThat(complexity("fun foo() { 42 }").value()).isZero();
    assertThat(complexity("fun foo() { f = fun() { 42 }; }").value()).isZero();
  }

  @Test
  void binary_operators() {
    assertThat(complexity("a == b;").value()).isZero();
    assertThat(complexity("a && b;").value()).isEqualTo(1);
    assertThat(complexity("a || b;").value()).isEqualTo(1);
    assertThat(complexity("a && b && c;").value()).isEqualTo(1);
    assertThat(complexity("a || b || c;").value()).isEqualTo(1);
    assertThat(complexity("a || b && c;").value()).isEqualTo(2);
    assertThat(complexity("a || b && c || d;").value()).isEqualTo(3);
  }

  @Test
  void jumps() {
    assertThat(complexity("break;").value()).isZero();
    assertThat(complexity("break foo;").value()).isEqualTo(1);
    assertThat(complexity("while (x) break;").value()).isEqualTo(1);
    assertThat(complexity("while (x) break foo;").value()).isEqualTo(2);

    assertThat(complexity("continue;").value()).isZero();
    assertThat(complexity("continue foo;").value()).isEqualTo(1);
    assertThat(complexity("while (x) continue;").value()).isEqualTo(1);
    assertThat(complexity("while (x) continue foo;").value()).isEqualTo(2);
  }

  @Test
  void nesting() {
    assertThat(complexity("if (x) a && b;").value()).isEqualTo(2);
    assertThat(complexity("if (x) if (y) 42;").value()).isEqualTo(3);
    assertThat(complexity("while (x) if (y) 42;").value()).isEqualTo(3);
    assertThat(complexity("match (x) { else -> if (y) 42; };").value()).isEqualTo(3);
    assertThat(complexity("try { x } catch (e) { if (y) 42; };").value()).isEqualTo(3);
    assertThat(complexity("try { if (y) 42; } catch (e) { x };").value()).isEqualTo(2);
    assertThat(complexity("fun foo() { if (x) 42; }").value()).isEqualTo(1);
    assertThat(complexity("fun foo() { f = fun() { if (x) 42; }; }").value()).isEqualTo(2);
    assertThat(complexity("if (x) { f = fun() { if (x) 42; }; };").value()).isEqualTo(4);
  }

  @Test
  void nesting_with_classes() {
    assertThat(complexity("class A { if (x) a && b; }").value()).isEqualTo(2);
    assertThat(complexity("class A { fun foo() { if (x) a && b; } }").value()).isEqualTo(2);
    assertThat(complexity("class A { fun foo() { fun bar() { if (x) a && b; } } }").value()).isEqualTo(3);
    assertThat(complexity("class A { fun foo() { class B { fun bar() { if (x) a && b; } } } }").value()).isEqualTo(2);
    assertThat(complexity("class A { fun foo() { if (x) a && b; class B { fun bar() { if (x) a && b; } } } }").value()).isEqualTo(4);
  }


  private CognitiveComplexity complexity(String code) {
    Tree tree = parser.parse(code);
    return new CognitiveComplexity(tree);
  }
}
