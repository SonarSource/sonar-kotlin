/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2026 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1.0.1, as published by SonarSource Sàrl.
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
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ctc.wstx.exc.WstxEOFException;
import kotlin.jvm.JvmField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonarsource.kotlin.metrics.TelemetryData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KotlinSurefireParserTest {

  private KotlinSurefireParser parser;

  @JvmField
  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5();
  private KotlinResourcesLocator kotlinResourcesLocator;
  private TelemetryData telemetryData;

  @BeforeEach
  void before() {
    FileSystem fs = new DefaultFileSystem(Paths.get("."));
    kotlinResourcesLocator = spy(new KotlinResourcesLocator(fs));
    parser = spy(new KotlinSurefireParser(kotlinResourcesLocator));
    telemetryData = new TelemetryData();

    doAnswer(
      invocation -> Optional.of(TestInputFileBuilder.create("", (String) invocation.getArguments()[0]).build())
    )
      .when(kotlinResourcesLocator)
      .findResourceByClassName(anyString());
  }

  @Test
  void should_store_zero_tests_when_directory_is_null_or_non_existing_or_a_file() {

    SensorContext context = mock(SensorContext.class);
    parser.collect(context, getDirs("nonExistingReportsDirectory"), false, telemetryData);
    verify(context, never()).newMeasure();

    context = mock(SensorContext.class);
    parser.collect(context, getDirs("file.txt"), true, telemetryData);
    verify(context, never()).newMeasure();
    verifyTelemetry(0, 0);
  }

  @Test
  void should_store_zero_tests_when_source_file_is_not_found() {

    when(kotlinResourcesLocator.findResourceByClassName(anyString())).thenReturn(Optional.empty());

    SensorContext context = mock(SensorContext.class);
    when(context.fileSystem()).thenReturn(new DefaultFileSystem(Paths.get("/test")));
    parser.collect(context, getDirs("multipleReports"), false, telemetryData);
    verify(context, never()).newMeasure();
    verifyTelemetry(0, 6);
  }

  @Test
  void shouldAggregateReports() {
    SensorContextTester context = mockContext();
    parser.collect(context, getDirs("multipleReports"), true, telemetryData);

    // Only 5 tests measures should be stored, no more: the TESTS-AllTests.xml must not be read as
    // there's 1 file result per unit test (SONAR-2841).
    assertThat(context.measures(":ch.hortis.sonar.mvn.mc.MetricsCollectorRegistryTest")).hasSize(5);
    assertThat(context.measures(":ch.hortis.sonar.mvn.mc.CloverCollectorTest")).hasSize(5);
    assertThat(context.measures(":ch.hortis.sonar.mvn.mc.CheckstyleCollectorTest")).hasSize(5);
    assertThat(context.measures(":ch.hortis.sonar.mvn.SonarMojoTest")).hasSize(5);
    assertThat(context.measures(":ch.hortis.sonar.mvn.mc.JDependsCollectorTest")).hasSize(5);
    assertThat(context.measures(":ch.hortis.sonar.mvn.mc.JavaNCSSCollectorTest")).hasSize(5);

    verifyTelemetry(6, 0);
  }

  @Test
  void shouldAggregateReportsFromMultipleDirectories() {
    SensorContextTester context = mockContext();
    parser.collect(context, getDirs("multipleDirectories/dir1", "multipleDirectories/dir2"), true, telemetryData);
    assertThat(context.measures(":ch.hortis.sonar.mvn.mc.MetricsCollectorRegistryTest")).hasSize(5);
    assertThat(context.measures(":ch.hortis.sonar.mvn.mc.CloverCollectorTest")).hasSize(5);
    assertThat(context.measures(":ch.hortis.sonar.mvn.mc.CheckstyleCollectorTest")).hasSize(5);
    assertThat(context.measures(":ch.hortis.sonar.mvn.SonarMojoTest")).hasSize(5);
    assertThat(context.measures(":ch.hortis.sonar.mvn.mc.JDependsCollectorTest")).hasSize(5);
    assertThat(context.measures(":ch.hortis.sonar.mvn.mc.JavaNCSSCollectorTest")).hasSize(5);

    verifyTelemetry(6, 0);
  }

  // SONAR-2841: if there's only a test suite report, then it should be read.
  @Test
  void shouldUseTestSuiteReportIfAlone() {
    SensorContextTester context = mockContext();

    parser.collect(context, getDirs("onlyTestSuiteReport"), true, telemetryData);
    assertThat(context.measures(":org.sonar.SecondTest")).hasSize(5);
    assertThat(context.measures(":org.sonar.JavaNCSSCollectorTest")).hasSize(5);

    verifyTelemetry(2, 0);
  }

  /**
   * See http://jira.codehaus.org/browse/SONAR-2371
   */
  @Test
  void shouldInsertZeroWhenNoReports() {
    SensorContext context = mock(SensorContext.class);
    parser.collect(context, getDirs("noReports"), true, telemetryData);
    verify(context, never()).newMeasure();
    verifyTelemetry(0, 0);
  }

  @Test
  void shouldNotInsertZeroOnFiles() {
    SensorContext context = mock(SensorContext.class);
    parser.collect(context, getDirs("noTests"), true, telemetryData);
    verify(context, never()).newMeasure();
    verifyTelemetry(0, 0);
  }

  @Test
  void shouldMergeInnerClasses() {
    SensorContextTester context = mockContext();
    parser.collect(context, getDirs("innerClasses"), true, telemetryData);
    assertThat(context.measure(":org.apache.commons.collections.bidimap.AbstractTestBidiMap", CoreMetrics.TESTS).value()).isEqualTo(7);
    assertThat(context.measure(":org.apache.commons.collections.bidimap.AbstractTestBidiMap", CoreMetrics.TEST_ERRORS).value()).isEqualTo(1);
    assertThat(context.measures(":org.apache.commons.collections.bidimap.AbstractTestBidiMap$TestBidiMapEntrySet")).isEmpty();
    verifyTelemetry(1, 0);
  }

  @Test
  void shouldMergeNestedInnerClasses() {
    SensorContextTester context = mockContext();
    parser.collect(context, getDirs("nestedInnerClasses"), true, telemetryData);
    assertThat(context.measure(":org.sonar.plugins.surefire.NestedInnerTest", CoreMetrics.TESTS).value()).isEqualTo(3);
    verifyTelemetry(1, 0);
  }

  @Test
  void shouldMergeInnerClassReportInExtraFile() {
    SensorContextTester context = mockContext();
    parser.collect(context, getDirs("innerClassExtraFile"), true, telemetryData);
    assertThat(context.measure(":com.example.project.CalculatorTests", CoreMetrics.TESTS).value()).isEqualTo(6);
    verifyTelemetry(1, 0);
  }

  @Test
  void shouldNotCountNegativeTests() {
    SensorContextTester context = mockContext();

    parser.collect(context, getDirs("negativeTestTime"), true, telemetryData);
    //Test times : -1.120, 0.644, 0.015 -> computed time : 0.659, ignore negative time.
    assertThat(context.measure(":java.Foo", CoreMetrics.SKIPPED_TESTS).value()).isZero();
    assertThat(context.measure(":java.Foo", CoreMetrics.TESTS).value()).isEqualTo(6);
    assertThat(context.measure(":java.Foo", CoreMetrics.TEST_ERRORS).value()).isZero();
    assertThat(context.measure(":java.Foo", CoreMetrics.TEST_FAILURES).value()).isZero();
    assertThat(context.measure(":java.Foo", CoreMetrics.TEST_EXECUTION_TIME).value()).isEqualTo(659);
    verifyTelemetry(1, 0);
  }

  @Test
  void shouldThrowWhenUnparsable() {
    SensorContextTester context = mockContext();

    var reportPath = "src/test/resources/org/sonarsource/kotlin/surefire/api/SurefireParserTest/unparsable/TEST-FooTest.xml"
      .replace('/', File.separatorChar);
    var dirs = getDirs("unparsable");
    assertThatThrownBy(() -> parser.collect(context, dirs, true, telemetryData))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Fail to parse the Surefire report: " + reportPath)
      .hasRootCauseInstanceOf(WstxEOFException.class)
      .hasRootCauseMessage("Unexpected EOF in prolog" + System.lineSeparator() +
        " at [row,col {unknown-source}]: [1,0]");
  }

  @Test
  void shouldAccumulateTelemetryAcrossMultipleCalls() {
    SensorContextTester context = mockContext();

    parser.collect(context, getDirs("onlyTestSuiteReport"), true, telemetryData);
    verifyTelemetry(2, 0);

    parser.collect(context, getDirs("negativeTestTime"), true, telemetryData);
    verifyTelemetry(3, 0);
  }

  private List<File> getDirs(String... directoryNames) {
    return Stream.of(directoryNames)
      .map(directoryName -> new File("src/test/resources/org/sonarsource/kotlin/surefire/api/SurefireParserTest/" + directoryName))
      .collect(Collectors.toList());
  }

  private SensorContextTester mockContext() {
    return SensorContextTester.create(new File(""));
  }

  private void verifyTelemetry(int expectedImported, int expectedFailed) {
    assertThat(telemetryData.getSurefireReportsImported()).hasValue(expectedImported);
    assertThat(telemetryData.getSurefireReportsFailed()).hasValue(expectedFailed);
  }
}
