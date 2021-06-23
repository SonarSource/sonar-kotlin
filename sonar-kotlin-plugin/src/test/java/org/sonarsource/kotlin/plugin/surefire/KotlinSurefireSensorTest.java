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
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonarsource.kotlin.plugin.surefire.api.SurefireUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KotlinSurefireSensorTest {

  private KotlinResourcesLocator kotlinResourcesLocator;
  private KotlinSurefireSensor surefireSensor;
  private SensorContextTester context;
  private final PathResolver pathResolver = new PathResolver();

  @BeforeEach
  void before() {
    DefaultFileSystem fs = new DefaultFileSystem(new File("src/test/resources"));
    DefaultInputFile kotlinFile = new TestInputFileBuilder("", "src/org/foo/kotlin").setLanguage("kotlin").build();
    fs.add(kotlinFile);
    context = SensorContextTester.create(new File(""));
    context.setFileSystem(fs);
    
    kotlinResourcesLocator = mock(KotlinResourcesLocator.class);
    when(kotlinResourcesLocator.findResourceByClassName(anyString())).thenAnswer(invocation -> Optional.of(resource((String) invocation.getArguments()[0])));

    surefireSensor = new KotlinSurefireSensor(new KotlinSurefireParser(kotlinResourcesLocator), new MapSettings().asConfig(), pathResolver);
  }

  private DefaultInputFile resource(String key) {
    return new TestInputFileBuilder("", key).setType(InputFile.Type.TEST).build();
  }

  @Test
  void should_execute_if_filesystem_contains_kotlin_files() {
    surefireSensor = new KotlinSurefireSensor(new KotlinSurefireParser(kotlinResourcesLocator), new MapSettings().asConfig(), pathResolver);
    DefaultSensorDescriptor defaultSensorDescriptor = new DefaultSensorDescriptor();
    surefireSensor.describe(defaultSensorDescriptor);
    assertThat(defaultSensorDescriptor.languages()).containsOnly("kotlin");
  }

  @Test
  void shouldNotFailIfReportsNotFound() {
    MapSettings settings = new MapSettings();
    settings.setProperty(SurefireUtils.SUREFIRE_REPORT_PATHS_PROPERTY, "unknown");

    KotlinSurefireSensor surefireSensor = new KotlinSurefireSensor(mock(KotlinSurefireParser.class), settings.asConfig(), pathResolver);
    surefireSensor.execute(context);
  }

  @Test
  void shouldHandleTestSuiteDetails() throws URISyntaxException {
    SensorContextTester context = SensorContextTester.create(new File(""));
    context.fileSystem()
      .add(resource("org.sonar.core.ExtensionsFinderTest"))
      .add(resource("org.sonar.core.ExtensionsFinderTest2"))
      .add(resource("org.sonar.core.ExtensionsFinderTest3"));

    collect(context, "/org/sonarsource/kotlin/plugin/surefire/KotlinSurefireSensorTest/shouldHandleTestSuiteDetails/");

    assertThat(context.measures(":org.sonar.core.ExtensionsFinderTest")).hasSize(5);
    assertThat(context.measures(":org.sonar.core.ExtensionsFinderTest2")).hasSize(5);
    assertThat(context.measures(":org.sonar.core.ExtensionsFinderTest3")).hasSize(5);

    assertThat(context.measure(":org.sonar.core.ExtensionsFinderTest", CoreMetrics.TESTS).value()).isEqualTo(4);
    assertThat(context.measure(":org.sonar.core.ExtensionsFinderTest", CoreMetrics.TEST_EXECUTION_TIME).value()).isEqualTo(111L);

    assertThat(context.measure(":org.sonar.core.ExtensionsFinderTest", CoreMetrics.TEST_FAILURES).value()).isEqualTo(1);
    assertThat(context.measure(":org.sonar.core.ExtensionsFinderTest", CoreMetrics.TEST_ERRORS).value()).isEqualTo(1);
    assertThat(context.measure(":org.sonar.core.ExtensionsFinderTest", CoreMetrics.SKIPPED_TESTS).value()).isZero();

    assertThat(context.measure(":org.sonar.core.ExtensionsFinderTest2", CoreMetrics.TESTS).value()).isEqualTo(2);
    assertThat(context.measure(":org.sonar.core.ExtensionsFinderTest2", CoreMetrics.TEST_EXECUTION_TIME).value()).isEqualTo(2);
    assertThat(context.measure(":org.sonar.core.ExtensionsFinderTest2", CoreMetrics.TEST_FAILURES).value()).isZero();
    assertThat(context.measure(":org.sonar.core.ExtensionsFinderTest2", CoreMetrics.TEST_ERRORS).value()).isZero();
    assertThat(context.measure(":org.sonar.core.ExtensionsFinderTest2", CoreMetrics.SKIPPED_TESTS).value()).isZero();

    assertThat(context.measure(":org.sonar.core.ExtensionsFinderTest3", CoreMetrics.TESTS).value()).isEqualTo(1);
    assertThat(context.measure(":org.sonar.core.ExtensionsFinderTest3", CoreMetrics.TEST_EXECUTION_TIME).value()).isEqualTo(16);
    assertThat(context.measure(":org.sonar.core.ExtensionsFinderTest3", CoreMetrics.TEST_FAILURES).value()).isZero();
    assertThat(context.measure(":org.sonar.core.ExtensionsFinderTest3", CoreMetrics.TEST_ERRORS).value()).isZero();
    assertThat(context.measure(":org.sonar.core.ExtensionsFinderTest3", CoreMetrics.SKIPPED_TESTS).value()).isEqualTo(1);
  }

  @Test
  void shouldSaveErrorsAndFailuresInXML() throws URISyntaxException {
    SensorContextTester context = SensorContextTester.create(new File(""));
    context.fileSystem()
      .add(resource("org.sonar.core.ExtensionsFinderTest"))
      .add(resource("org.sonar.core.ExtensionsFinderTest2"))
      .add(resource("org.sonar.core.ExtensionsFinderTest3"));

    collect(context, "/org/sonarsource/kotlin/plugin/surefire/KotlinSurefireSensorTest/shouldSaveErrorsAndFailuresInXML/");

    // 1 classes, 5 measures by class
    assertThat(context.measures(":org.sonar.core.ExtensionsFinderTest")).hasSize(5);
    assertThat(context.measure(":org.sonar.core.ExtensionsFinderTest", CoreMetrics.SKIPPED_TESTS).value()).isEqualTo(1);
    assertThat(context.measure(":org.sonar.core.ExtensionsFinderTest", CoreMetrics.TESTS).value()).isEqualTo(7);
  }

  @Test
  void shouldSupportLongAttributeValues() throws URISyntaxException {
    SensorContextTester context = SensorContextTester.create(new File(""));
    collect(context, "/org/sonarsource/kotlin/plugin/surefire/KotlinSurefireSensorTest/should_support_long_attribute_values/");
    assertThat(context.allAnalysisErrors()).isEmpty();
  }

  @Test
  void shouldManageClassesWithDefaultPackage() throws URISyntaxException {
    SensorContextTester context = SensorContextTester.create(new File(""));
    context.fileSystem()
      .add(resource("NoPackagesTest"));

    collect(context, "/org/sonarsource/kotlin/plugin/surefire/KotlinSurefireSensorTest/shouldManageClassesWithDefaultPackage/");

    assertThat(context.measure(":NoPackagesTest", CoreMetrics.TESTS).value()).isEqualTo(2);
  }

  @Test
  void successRatioIsZeroWhenAllTestsFail() throws URISyntaxException {
    SensorContextTester context = SensorContextTester.create(new File(""));
    context.fileSystem()
      .add(resource("org.sonar.Foo"));

    collect(context, "/org/sonarsource/kotlin/plugin/surefire/KotlinSurefireSensorTest/successRatioIsZeroWhenAllTestsFail/");

    assertThat(context.measure(":org.sonar.Foo", CoreMetrics.TESTS).value()).isEqualTo(2);
    assertThat(context.measure(":org.sonar.Foo", CoreMetrics.TEST_FAILURES).value()).isEqualTo(1);
    assertThat(context.measure(":org.sonar.Foo", CoreMetrics.TEST_ERRORS).value()).isEqualTo(1);
  }

  @Test
  void measuresShouldNotIncludeSkippedTests() throws URISyntaxException {
    SensorContextTester context = SensorContextTester.create(new File(""));
    context.fileSystem()
      .add(resource("org.sonar.Foo"));

    collect(context, "/org/sonarsource/kotlin/plugin/surefire/KotlinSurefireSensorTest/measuresShouldNotIncludeSkippedTests/");

    assertThat(context.measure(":org.sonar.Foo", CoreMetrics.TESTS).value()).isEqualTo(2);
    assertThat(context.measure(":org.sonar.Foo", CoreMetrics.TEST_FAILURES).value()).isEqualTo(1);
    assertThat(context.measure(":org.sonar.Foo", CoreMetrics.TEST_ERRORS).value()).isZero();
    assertThat(context.measure(":org.sonar.Foo", CoreMetrics.SKIPPED_TESTS).value()).isEqualTo(1);
  }

  @Test
  void noSuccessRatioIfNoTests() throws URISyntaxException {
    SensorContextTester context = SensorContextTester.create(new File(""));
    context.fileSystem()
      .add(resource("org.sonar.Foo"));

    collect(context, "/org/sonarsource/kotlin/plugin/surefire/KotlinSurefireSensorTest/noSuccessRatioIfNoTests/");

    assertThat(context.measure(":org.sonar.Foo", CoreMetrics.TESTS).value()).isZero();
    assertThat(context.measure(":org.sonar.Foo", CoreMetrics.TEST_FAILURES).value()).isZero();
    assertThat(context.measure(":org.sonar.Foo", CoreMetrics.TEST_ERRORS).value()).isZero();
    assertThat(context.measure(":org.sonar.Foo", CoreMetrics.SKIPPED_TESTS).value()).isEqualTo(2);
  }

  @Test
  void should_support_reportNameSuffix() throws URISyntaxException {
    SensorContextTester context = SensorContextTester.create(new File(""));
    context.fileSystem()
      .add(resource("org.sonar.Foo"));

    collect(context, "/org/sonarsource/kotlin/plugin/surefire/KotlinSurefireSensorTest/should_support_reportNameSuffix/");

    assertThat(context.measure(":org.sonar.Foo", CoreMetrics.TESTS).value()).isEqualTo(4);
    assertThat(context.measure(":org.sonar.Foo", CoreMetrics.TEST_FAILURES).value()).isEqualTo(2);
    assertThat(context.measure(":org.sonar.Foo", CoreMetrics.TEST_ERRORS).value()).isZero();
    assertThat(context.measure(":org.sonar.Foo", CoreMetrics.SKIPPED_TESTS).value()).isEqualTo(2);
  }
  
  @Test
  void testToStringMethod() {
    assertThat(surefireSensor).hasToString("KotlinSurefireSensor");
  }

  private void collect(SensorContextTester context, String path) throws URISyntaxException {
    File file = new File(getClass().getResource(path).toURI());
    surefireSensor.collect(context, Collections.singletonList(file));
  }
}
