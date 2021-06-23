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
package org.sonarsource.kotlin.plugin.surefire;

import java.io.File;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFilePredicates;
import org.sonar.api.batch.fs.internal.DefaultIndexedFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KotlinResourcesLocatorTest {

  private final FileSystem fileSystem = mock(FileSystem.class);
  private final KotlinResourcesLocator kotlinResourcesLocator = new KotlinResourcesLocator(fileSystem);
  private final InputFile expected = new DefaultInputFile(new DefaultIndexedFile("", new File("/").toPath(), "",""), (x) -> {});
  
  @BeforeEach
  void setUp() {
    when(fileSystem.predicates()).thenReturn(new DefaultFilePredicates(new File("/").toPath()));
    when(fileSystem.inputFiles(any())).thenReturn(Collections.singletonList(expected));
  }
  
  @Test
  void findResourceByClassName() {
    when(fileSystem.hasFiles(any())).thenReturn(true);
    
    Optional<InputFile> inputFile = kotlinResourcesLocator.findResourceByClassName("MyClass");
    
    assertEquals(Optional.of(expected), inputFile);
  }

  @Test
  void findNoResourceByClassName() {
    when(fileSystem.hasFiles(any())).thenReturn(false);

    Optional<InputFile> inputFile = kotlinResourcesLocator.findResourceByClassName("MyClass");

    assertEquals(Optional.empty(), inputFile);
  }
}