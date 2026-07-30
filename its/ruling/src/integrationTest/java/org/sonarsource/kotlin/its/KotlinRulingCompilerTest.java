/*
 * SonarSource Kotlin
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonarsource.kotlin.its;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class KotlinRulingCompilerTest extends AbstractKotlinRulingTest {

  @Test
  @EnabledIfEnvironmentVariable(named = "KOTLIN_COMPILER_IT_ENABLED", matches = "true")
  void test_kotlin_compiler() throws IOException {
    List<String> exclusions = List.of(
      "**/testData/**/*",
      "sources/kotlin/kotlin/compiler/daemon/src/org/jetbrains/kotlin/daemon/CompileServiceImpl.kt",
      "sources/kotlin/kotlin/compiler/psi/src/org/jetbrains/kotlin/psi/psiUtil/ktPsiUtil.kt",
      "sources/kotlin/kotlin/compiler/psi/src/org/jetbrains/kotlin/psi/psiUtil/psiUtils.kt",
      "sources/kotlin/kotlin/j2k/src/org/jetbrains/kotlin/j2k/ast/Statements.kt",
      "sources/kotlin/kotlin/libraries/stdlib/js/src/org.w3c/org.w3c.dom.kt",
      "sources/kotlin/kotlin/libraries/stdlib/js/src/org.w3c/org.khronos.webgl.kt"
    );
    analyzeAndAssertDifferences("kotlin", Map.of(
      "sonar.inclusions", "sources/kotlin/kotlin/**/*.kt",
      "sonar.exclusions", String.join(",", exclusions)));
  }
}
