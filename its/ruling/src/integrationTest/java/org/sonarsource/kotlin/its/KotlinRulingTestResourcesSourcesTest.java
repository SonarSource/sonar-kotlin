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

import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class KotlinRulingTestResourcesSourcesTest extends AbstractKotlinRulingTest {

  @Test
  void test_resources_sources() throws IOException {
    analyzeAndAssertDifferences("test-resources-sources", Map.of(
      "sonar.inclusions", "ruling/src/integrationTest/resources/sources/kotlin/**/*.kt",
      "sonar.java.libraries", System.getProperty("gradle.main.compile.classpath").replace(File.pathSeparatorChar, ',')
    ));
  }
}
