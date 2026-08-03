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
package org.sonarsource.kotlin.surefire;

import java.io.File;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.scanner.plugin.api.impl.fs.DefaultIndexedFile;
import org.sonar.scanner.plugin.api.impl.fs.DefaultInputFile;
import org.sonar.scanner.plugin.api.impl.fs.predicates.DefaultFilePredicates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KotlinResourcesLocatorTest {

  private final FileSystem fileSystem = mock(FileSystem.class);
  private final KotlinResourcesLocator kotlinResourcesLocator = new KotlinResourcesLocator(fileSystem);
  private final InputFile expected = new DefaultInputFile(
    new DefaultIndexedFile("", new File("/").toPath(), "",""),
    x -> {},
    x -> {});
  private final InputFile other = new DefaultInputFile(
    new DefaultIndexedFile("", new File("/").toPath(), "",""),
    x -> {},
    x -> {});

  @BeforeEach
  void setUp() {
    when(fileSystem.predicates()).thenReturn(new DefaultFilePredicates(new File("/").toPath()));
  }

  @Test
  void findResourceByClassName() {
    when(fileSystem.inputFiles(any())).thenReturn(Collections.singletonList(expected));

    List<InputFile> inputFiles = kotlinResourcesLocator.findResourceByClassName("MyClass");

    assertThat(inputFiles).containsExactly(expected);
  }

  @Test
  void findNoResourceByClassName() {
    when(fileSystem.inputFiles(any())).thenReturn(Collections.emptyList());

    List<InputFile> inputFiles = kotlinResourcesLocator.findResourceByClassName("MyClass");

    assertThat(inputFiles).isEmpty();
  }

  @Test
  void findMultipleResourcesByClassName() {
    when(fileSystem.inputFiles(any())).thenReturn(List.of(expected, other));

    List<InputFile> inputFiles = kotlinResourcesLocator.findResourceByClassName("MyClass");

    assertThat(inputFiles).containsExactly(expected, other);
  }
}
