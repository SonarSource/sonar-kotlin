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

import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BuiltinsDecompilationTest extends SitTestBase {

  private static final String BASE_DIRECTORY = "projects/builtins-decompilation/kotlin";

  /**
   * Errors raised inside the Kotlin compiler are logged only at DEBUG level with prefix "ERROR:",
   * for example the {@code NoSuchMethodError} of {@code FileType.getDefaultExtension} raised while decompiling builtins.
   */
  @Test
  public void should_decompile_builtins_without_errors() {
    var result = analyze("builtinsDecompilation", BASE_DIRECTORY, Map.of("sonar.verbose", "true"));

    assertThat(result.analyzedFileCount()).isEqualTo(1);
    assertThat(result.logMessages()).contains("to parse source code");
    assertThat(result.logMessages().lines()).noneMatch(line -> line.startsWith("ERROR:"));
  }
}
