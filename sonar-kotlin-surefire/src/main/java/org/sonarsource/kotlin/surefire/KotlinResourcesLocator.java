/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2024 SonarSource SA
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
package org.sonarsource.kotlin.surefire;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ScannerSide
public class KotlinResourcesLocator {
  private static final Logger LOGGER = LoggerFactory.getLogger(KotlinResourcesLocator.class);
  private final FileSystem fs;

  public KotlinResourcesLocator(FileSystem fs) {
    this.fs = fs;
  }
  
  public Optional<InputFile> findResourceByClassName(String className) {
    String fileName = className.replace(".", "/");
    LOGGER.info("Searching for {}", fileName);
    FilePredicates p = fs.predicates();
    FilePredicate fileNamePredicates =
      getFileNamePredicateFromSuffixes(p, fileName, new String[]{".kt"});
    if (fs.hasFiles(fileNamePredicates)) {
      return Optional.of(fs.inputFiles(fileNamePredicates).iterator().next());
    } else {
      return Optional.empty();
    }
  }

  private static FilePredicate getFileNamePredicateFromSuffixes(
    FilePredicates p, String fileName, String[] suffixes) {
    List<FilePredicate> fileNamePredicates = new ArrayList<>(suffixes.length);
    for (String suffix : suffixes) {
      fileNamePredicates.add(p.matchesPathPattern("**/" + fileName + suffix));
    }
    return p.or(fileNamePredicates);
  }
}
