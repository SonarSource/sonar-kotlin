/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
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
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.xml.stream.XMLStreamException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonarsource.kotlin.surefire.data.UnitTestClassReport;
import org.sonarsource.kotlin.surefire.data.UnitTestIndex;

@ScannerSide
public class KotlinSurefireParser {
  private static final Logger LOGGER = LoggerFactory.getLogger(KotlinSurefireParser.class);
  private final KotlinResourcesLocator kotlinResourcesLocator;

  public KotlinSurefireParser(KotlinResourcesLocator kotlinResourcesLocator) {
    this.kotlinResourcesLocator = kotlinResourcesLocator;
  }

  public void collect(SensorContext context, List<File> reportsDirs, boolean reportDirSetByUser) {
    List<File> xmlFiles = getReports(reportsDirs, reportDirSetByUser);
    if (!xmlFiles.isEmpty()) {
      parseFiles(context, xmlFiles);
    }
  }

  private static List<File> getReports(List<File> dirs, boolean reportDirSetByUser) {
    return dirs.stream()
      .map(dir -> getReports(dir, reportDirSetByUser))
      .flatMap(Arrays::stream)
      .collect(Collectors.toList());
  }

  private static File[] getReports(File dir, boolean reportDirSetByUser) {
    if (!dir.isDirectory()) {
      if (reportDirSetByUser) {
        LOGGER.error("Reports path not found or is not a directory: {}", dir.getAbsolutePath());
      }
      return new File[0];
    }
    File[] unitTestResultFiles = findXMLFilesStartingWith(dir, "TEST-");
    if (unitTestResultFiles.length == 0) {
      // maybe there's only a test suite result file
      unitTestResultFiles = findXMLFilesStartingWith(dir, "TESTS-");
    }
    if (unitTestResultFiles.length == 0) {
      LOGGER.warn("Reports path contains no files matching TEST-.*.xml : {}", dir.getAbsolutePath());
    }
    return unitTestResultFiles;
  }

  private static File[] findXMLFilesStartingWith(File dir, final String fileNameStart) {
    return dir.listFiles((parentDir, name) -> name.startsWith(fileNameStart) && name.endsWith(".xml"));
  }

  private void parseFiles(SensorContext context, List<File> reports) {
    var index = new UnitTestIndex();
    parseFiles(reports, index);
    sanitize(index);
    save(index, context);
  }

  private static void parseFiles(List<File> reports, UnitTestIndex index) {
    var parser = new StaxParser(index);
    for (File report : reports) {
      try {
        parser.parse(report);
      } catch (XMLStreamException e) {
        throw new IllegalStateException("Fail to parse the Surefire report: " + report, e);
      }
    }
  }

  private static void sanitize(UnitTestIndex index) {
    for (String classname : index.getClassnames()) {
      if (classname.contains("$")) {
        // Surefire reports classes whereas sonar supports files
        var parentClassName = classname.substring(0, classname.indexOf('$'));
        index.merge(classname, parentClassName);
      }
    }
  }

  private void save(UnitTestIndex index, SensorContext context) {
    long negativeTimeTestNumber = 0;
    for (Map.Entry<String, UnitTestClassReport> entry : index.getIndexByClassname().entrySet()) {
      UnitTestClassReport report = entry.getValue();
      if (report.getTests() > 0) {
        negativeTimeTestNumber += report.getNegativeTimeTestNumber();
        Optional<InputFile> inputFile = kotlinResourcesLocator.findResourceByClassName(entry.getKey());
        if (inputFile.isPresent()) {
          save(report, inputFile.get(), context);
        } else {
          LOGGER.warn("Resource not found: {} under the directory {} while reading test reports. Please, make sure your \"sonar.junit.reportPaths\" property is configured properly",
            entry.getKey(), context.fileSystem().baseDir().getPath());
        }
      }
    }
    if (negativeTimeTestNumber > 0) {
      LOGGER.warn(
        "There is {} test(s) reported with negative time by surefire, total duration may not be accurate.",
        negativeTimeTestNumber);
    }
  }

  private static void save(UnitTestClassReport report, InputFile inputFile, SensorContext context) {
    int testsCount = report.getTests() - report.getSkipped();
    saveMeasure(context, inputFile, CoreMetrics.SKIPPED_TESTS, report.getSkipped());
    saveMeasure(context, inputFile, CoreMetrics.TESTS, testsCount);
    saveMeasure(context, inputFile, CoreMetrics.TEST_ERRORS, report.getErrors());
    saveMeasure(context, inputFile, CoreMetrics.TEST_FAILURES, report.getFailures());
    saveMeasure(
      context, inputFile, CoreMetrics.TEST_EXECUTION_TIME, report.getDurationMilliseconds());
  }

  private static <T extends Serializable> void saveMeasure(
    SensorContext context, InputFile inputFile, Metric<T> metric, T value) {
    context.<T>newMeasure().forMetric(metric).on(inputFile).withValue(value).save();
  }
}
