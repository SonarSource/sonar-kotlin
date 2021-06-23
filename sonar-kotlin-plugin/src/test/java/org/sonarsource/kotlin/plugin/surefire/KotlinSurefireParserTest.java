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
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@EnableRuleMigrationSupport
class KotlinSurefireParserTest {

  private static final String PREFIX = "Resource not found:";
  private static final String WARNING = "while reading test reports. Please, make sure your \"sonar.junit.reportPaths\" property is configured properly";
  private KotlinSurefireParser parser;

  @Rule
  public LogTester logTester = new LogTester();
  private KotlinResourcesLocator kotlinResourcesLocator;

  @BeforeEach
  void before() {
    FileSystem fs = new DefaultFileSystem(Paths.get("."));
    kotlinResourcesLocator = spy(new KotlinResourcesLocator(fs));
    parser = spy(new KotlinSurefireParser(kotlinResourcesLocator));

    doAnswer(
      invocation -> Optional.of(TestInputFileBuilder.create("", (String) invocation.getArguments()[0]).build())
    )
      .when(kotlinResourcesLocator)
      .findResourceByClassName(anyString());
  }

  @Test
  void should_store_zero_tests_when_directory_is_null_or_non_existing_or_a_file() {

    SensorContext context = mock(SensorContext.class);
    parser.collect(context, getDirs("nonExistingReportsDirectory"), false);
    verify(context, never()).newMeasure();

    context = mock(SensorContext.class);
    parser.collect(context, getDirs("file.txt"), true);
    verify(context, never()).newMeasure();
  }

  @Test
  void should_store_zero_tests_when_source_file_is_not_found() {

    when(kotlinResourcesLocator.findResourceByClassName(anyString())).thenReturn(Optional.empty());

    SensorContext context = mock(SensorContext.class);
    when(context.fileSystem()).thenReturn(new DefaultFileSystem(Paths.get("/test")));
    parser.collect(context, getDirs("multipleReports"), false);
    verify(context, never()).newMeasure();
    assertThat(logTester.logs(LoggerLevel.WARN)).isNotEmpty();
    assertThat(logTester.logs(LoggerLevel.WARN)).allMatch(message -> message.startsWith(PREFIX));
    assertThat(logTester.logs(LoggerLevel.WARN)).allMatch(message -> message.endsWith(WARNING));
  }

  @Test
  void shouldAggregateReports() {
    SensorContextTester context = mockContext();
    parser.collect(context, getDirs("multipleReports"), true);

    // Only 5 tests measures should be stored, no more: the TESTS-AllTests.xml must not be read as
    // there's 1 file result per unit test (SONAR-2841).
    assertThat(context.measures(":ch.hortis.sonar.mvn.mc.MetricsCollectorRegistryTest")).hasSize(5);
    assertThat(context.measures(":ch.hortis.sonar.mvn.mc.CloverCollectorTest")).hasSize(5);
    assertThat(context.measures(":ch.hortis.sonar.mvn.mc.CheckstyleCollectorTest")).hasSize(5);
    assertThat(context.measures(":ch.hortis.sonar.mvn.SonarMojoTest")).hasSize(5);
    assertThat(context.measures(":ch.hortis.sonar.mvn.mc.JDependsCollectorTest")).hasSize(5);
    assertThat(context.measures(":ch.hortis.sonar.mvn.mc.JavaNCSSCollectorTest")).hasSize(5);
  }

  @Test
  void shouldAggregateReportsFromMultipleDirectories() {
    SensorContextTester context = mockContext();
    parser.collect(context, getDirs("multipleDirectories/dir1", "multipleDirectories/dir2"), true);
    assertThat(context.measures(":ch.hortis.sonar.mvn.mc.MetricsCollectorRegistryTest")).hasSize(5);
    assertThat(context.measures(":ch.hortis.sonar.mvn.mc.CloverCollectorTest")).hasSize(5);
    assertThat(context.measures(":ch.hortis.sonar.mvn.mc.CheckstyleCollectorTest")).hasSize(5);
    assertThat(context.measures(":ch.hortis.sonar.mvn.SonarMojoTest")).hasSize(5);
    assertThat(context.measures(":ch.hortis.sonar.mvn.mc.JDependsCollectorTest")).hasSize(5);
    assertThat(context.measures(":ch.hortis.sonar.mvn.mc.JavaNCSSCollectorTest")).hasSize(5);
  }

  // SONAR-2841: if there's only a test suite report, then it should be read.
  @Test
  void shouldUseTestSuiteReportIfAlone() {
    SensorContextTester context = mockContext();

    parser.collect(context, getDirs("onlyTestSuiteReport"), true);
    assertThat(context.measures(":org.sonar.SecondTest")).hasSize(5);
    assertThat(context.measures(":org.sonar.JavaNCSSCollectorTest")).hasSize(5);
  }

  /**
   * See http://jira.codehaus.org/browse/SONAR-2371
   */
  @Test
  void shouldInsertZeroWhenNoReports() {
    SensorContext context = mock(SensorContext.class);
    parser.collect(context, getDirs("noReports"), true);
    verify(context, never()).newMeasure();
  }

  @Test
  void shouldNotInsertZeroOnFiles() {
    SensorContext context = mock(SensorContext.class);
    parser.collect(context, getDirs("noTests"), true);
    verify(context, never()).newMeasure();
  }

  @Test
  void shouldMergeInnerClasses() {
    SensorContextTester context = mockContext();
    parser.collect(context, getDirs("innerClasses"), true);
    assertThat(context.measure(":org.apache.commons.collections.bidimap.AbstractTestBidiMap", CoreMetrics.TESTS).value()).isEqualTo(7);
    assertThat(context.measure(":org.apache.commons.collections.bidimap.AbstractTestBidiMap", CoreMetrics.TEST_ERRORS).value()).isEqualTo(1);
    assertThat(context.measures(":org.apache.commons.collections.bidimap.AbstractTestBidiMap$TestBidiMapEntrySet")).isEmpty();
  }

  @Test
  void shouldMergeNestedInnerClasses() {
    SensorContextTester context = mockContext();
    parser.collect(context, getDirs("nestedInnerClasses"), true);
    assertThat(context.measure(":org.sonar.plugins.surefire.NestedInnerTest", CoreMetrics.TESTS).value()).isEqualTo(3);
  }

  @Test
  void shouldMergeInnerClassReportInExtraFile() {
    SensorContextTester context = mockContext();
    parser.collect(context, getDirs("innerClassExtraFile"), true);
    assertThat(context.measure(":com.example.project.CalculatorTests", CoreMetrics.TESTS).value()).isEqualTo(6);
  }

  @Test
  void shouldNotCountNegativeTests() {
    SensorContextTester context = mockContext();

    parser.collect(context, getDirs("negativeTestTime"), true);
    //Test times : -1.120, 0.644, 0.015 -> computed time : 0.659, ignore negative time.
    assertThat(context.measure(":java.Foo", CoreMetrics.SKIPPED_TESTS).value()).isZero();
    assertThat(context.measure(":java.Foo", CoreMetrics.TESTS).value()).isEqualTo(6);
    assertThat(context.measure(":java.Foo", CoreMetrics.TEST_ERRORS).value()).isZero();
    assertThat(context.measure(":java.Foo", CoreMetrics.TEST_FAILURES).value()).isZero();
    assertThat(context.measure(":java.Foo", CoreMetrics.TEST_EXECUTION_TIME).value()).isEqualTo(659);
  }

  private List<File> getDirs(String... directoryNames) {
    return Stream.of(directoryNames)
      .map(directoryName -> new File("src/test/resources/org/sonarsource/kotlin/plugin/surefire/api/SurefireParserTest/" + directoryName))
      .collect(Collectors.toList());
  }

  private SensorContextTester mockContext() {
    return SensorContextTester.create(new File(""));
  }

}
