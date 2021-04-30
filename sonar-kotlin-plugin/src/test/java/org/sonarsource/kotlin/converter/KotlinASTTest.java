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
package org.sonarsource.kotlin.converter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.sonarsource.slang.api.ParseException;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.visitors.TreePrinter;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

class KotlinASTTest {
  private static KotlinConverter converter = new KotlinConverter(Collections.emptyList());

  @Test
  void all_kotlin_files() throws IOException {
    for (Path kotlinPath : getKotlinSources()) {
      Path astPath = Paths.get(kotlinPath.toString().replaceFirst("\\.kts?$", ".txt"));
      String actualAst = TreePrinter.table(parse(kotlinPath));
      String expectingAst = astPath.toFile().exists() ? new String(Files.readAllBytes(astPath), UTF_8) : "";
      assertThat(actualAst)
        .describedAs("In the file: " + astPath + " (run KotlinASTTest.main manually)")
        .isEqualTo(expectingAst);
    }
  }

  public static void main(String[] args) throws IOException {
    fix_all_cls_files_test_automatically();
  }

  private static void fix_all_cls_files_test_automatically() throws IOException {
    for (Path kotlinPath : getKotlinSources()) {
      Path astPath = Paths.get(kotlinPath.toString().replaceFirst("\\.kts?$", ".txt"));
      String actualAst = TreePrinter.table(parse(kotlinPath));
      Files.write(astPath, actualAst.getBytes(UTF_8));
    }
  }

  private static List<Path> getKotlinSources() throws IOException {
    try (Stream<Path> pathStream = Files.walk(Paths.get("src", "test", "resources", "ast"))) {
      return pathStream
        .filter(path -> !path.toFile().isDirectory() && path.getFileName().toString().endsWith(".kt") || path.getFileName().toString().endsWith(".kts"))
        .sorted()
        .collect(Collectors.toList());
    }
  }

  private static Tree parse(Path path) throws IOException {
    String code = new String(Files.readAllBytes(path), UTF_8);
    try {
      return converter.parse(code);
    } catch (ParseException e) {
      throw new ParseException(e.getMessage() + " in file " + path, e.getPosition(), e);
    } catch (RuntimeException e) {
      throw new RuntimeException(e.getClass().getSimpleName() + ": " + e.getMessage() + " in file " + path, e);
    }
  }
}
