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
package org.sonarsource.slang.plugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.sonar.api.SonarProduct;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.resources.Language;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonarsource.analyzer.commons.ProgressReport;
import org.sonarsource.slang.api.ASTConverter;
import org.sonarsource.slang.api.ParseException;
import org.sonarsource.slang.api.TextPointer;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.checks.api.SlangCheck;
import org.sonarsource.slang.plugin.converter.ASTConverterValidation;
import org.sonarsource.slang.visitors.TreeVisitor;

public abstract class SlangSensor implements Sensor {
  private static final Logger LOG = Loggers.get(SlangSensor.class);
  private static final Pattern EMPTY_FILE_CONTENT_PATTERN = Pattern.compile("\\s*+");

  private final NoSonarFilter noSonarFilter;
  private final Language language;
  private FileLinesContextFactory fileLinesContextFactory;

  protected SlangSensor(NoSonarFilter noSonarFilter, FileLinesContextFactory fileLinesContextFactory, Language language) {
    this.noSonarFilter = noSonarFilter;
    this.fileLinesContextFactory = fileLinesContextFactory;
    this.language = language;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .onlyOnLanguage(language.getKey())
      .name(language.getName() + " Sensor");
  }

  protected abstract ASTConverter astConverter(SensorContext sensorContext);

  protected abstract Checks<SlangCheck> checks();

  protected abstract String repositoryKey();

  private boolean analyseFiles(ASTConverter converter,
    SensorContext sensorContext,
    Iterable<InputFile> inputFiles,
    ProgressReport progressReport,
    List<TreeVisitor<InputFileContext>> visitors,
    DurationStatistics statistics) {

    for (InputFile inputFile : inputFiles) {
      if (sensorContext.isCancelled()) {
        return false;
      }
      InputFileContext inputFileContext = new InputFileContext(sensorContext, inputFile);
      try {
        analyseFile(converter, inputFileContext, inputFile, visitors, statistics);
      } catch (ParseException e) {
        logParsingError(inputFile, e);
        inputFileContext.reportAnalysisParseError(repositoryKey(), inputFile, e.getPosition());
      }
      progressReport.nextFile();
    }
    return true;
  }

  private static void analyseFile(ASTConverter converter,
                                  InputFileContext inputFileContext,
                                  InputFile inputFile,
                                  List<TreeVisitor<InputFileContext>> visitors,
                                  DurationStatistics statistics) {
    String content;
    String fileName;
    try {
      content = inputFile.contents();
      fileName = inputFile.toString();
    } catch (IOException | RuntimeException e) {
      throw toParseException("read", inputFile, e);
    }

    if (EMPTY_FILE_CONTENT_PATTERN.matcher(content).matches()) {
      return;
    }

    Tree tree = statistics.time("Parse", () -> {
      try {
        return converter.parse(content, fileName);
      } catch (RuntimeException e) {
        throw toParseException("parse", inputFile, e);
      }
    });
    for (TreeVisitor<InputFileContext> visitor : visitors) {
      try {
        String visitorId = visitor.getClass().getSimpleName();
        statistics.time(visitorId, () -> visitor.scan(inputFileContext, tree));
      } catch (RuntimeException e) {
        inputFileContext.reportAnalysisError(e.getMessage(), null);
        LOG.error("Cannot analyse '" + inputFile +"': " + e.getMessage(), e);
      }
    }
  }

  private static ParseException toParseException(String action, InputFile inputFile, Exception cause) {
    TextPointer position = cause instanceof ParseException ? ((ParseException) cause).getPosition() : null;
    return new ParseException("Cannot " + action + " '" + inputFile + "': " + cause.getMessage(), position, cause);
  }

  private static void logParsingError(InputFile inputFile, ParseException e) {
    TextPointer position = e.getPosition();
    String positionMessage = "";
    if (position != null) {
      positionMessage = String.format("Parse error at position %s:%s", position.line(), position.lineOffset());
    }
    LOG.error(String.format("Unable to parse file: %s. %s", inputFile.uri(), positionMessage));
    LOG.error(e.getMessage());
  }

  @Override
  public void execute(SensorContext sensorContext) {
    DurationStatistics statistics = new DurationStatistics(sensorContext.config());
    FileSystem fileSystem = sensorContext.fileSystem();
    FilePredicate mainFilePredicate = fileSystem.predicates().and(
      fileSystem.predicates().hasLanguage(language.getKey()),
      fileSystem.predicates().hasType(InputFile.Type.MAIN));
    Iterable<InputFile> inputFiles = fileSystem.inputFiles(mainFilePredicate);
    List<String> filenames = StreamSupport.stream(inputFiles.spliterator(), false).map(InputFile::toString).collect(Collectors.toList());
    ProgressReport progressReport = new ProgressReport("Progress of the " + language.getName() + " analysis", TimeUnit.SECONDS.toMillis(10));
    progressReport.start(filenames);
    boolean success = false;
    ASTConverter converter = ASTConverterValidation.wrap(astConverter(sensorContext), sensorContext.config());
    try {
      success = analyseFiles(converter, sensorContext, inputFiles, progressReport, visitors(sensorContext, statistics), statistics);
    } finally {
      if (success) {
        progressReport.stop();
      } else {
        progressReport.cancel();
      }
      converter.terminate();
    }
    statistics.log();
  }

  private List<TreeVisitor<InputFileContext>> visitors(SensorContext sensorContext, DurationStatistics statistics) {
    List<TreeVisitor<InputFileContext>> treeVisitors;
    if (sensorContext.runtime().getProduct() == SonarProduct.SONARLINT) {
      treeVisitors = Arrays.asList(
        new IssueSuppressionVisitor(),
        new ChecksVisitor(checks(), statistics)
      );
    } else {
      treeVisitors = Arrays.asList(
        new IssueSuppressionVisitor(),
        new MetricVisitor(fileLinesContextFactory, noSonarFilter),
        new ChecksVisitor(checks(), statistics),
        new CpdVisitor(),
        new SyntaxHighlighter());
    }

    // TODO: remove this workaround once the Kotlin plugin is extracted
    List<TreeVisitor<InputFileContext>> languageSpecificTreeVisitors = new ArrayList<>(treeVisitors);
    languageSpecificTreeVisitors.addAll(languageSpecificVisitors(sensorContext));

    return languageSpecificTreeVisitors;
  }

  protected List<TreeVisitor<InputFileContext>> languageSpecificVisitors(SensorContext sensorContext) {
    return Collections.emptyList();
  }
}
