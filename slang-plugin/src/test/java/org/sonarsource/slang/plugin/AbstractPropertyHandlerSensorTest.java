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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.api.utils.log.ThreadLocalLogTester;

import static org.assertj.core.api.Assertions.assertThat;

import static java.nio.charset.StandardCharsets.UTF_8;

@EnableRuleMigrationSupport
class AbstractPropertyHandlerSensorTest {

  private static final List<String> ANALYSIS_WARNINGS = new ArrayList<>();
  private static final Path PROJECT_DIR = Paths.get("src", "test", "resources", "propertyHandler");

  @Rule
  public ThreadLocalLogTester logTester = new ThreadLocalLogTester();

  @BeforeEach
  void setup() {
    ANALYSIS_WARNINGS.clear();
  }

  @Test
  void test_descriptor() throws Exception {
    DefaultSensorDescriptor sensorDescriptor = new DefaultSensorDescriptor();
    PropertyHandlerSensorTester sensor = new PropertyHandlerSensorTester();
    sensor.describe(sensorDescriptor);
    assertThat(sensorDescriptor.name()).isEqualTo("Import of propertyName issues");
    assertThat(sensorDescriptor.languages()).containsOnly("dummy");
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  void test_configuration() throws Exception {
    PropertyHandlerSensorTester sensor = new PropertyHandlerSensorTester();
    assertThat(sensor.propertyKey()).isEqualTo("propertyKey");
    assertThat(sensor.propertyName()).isEqualTo("propertyName");
    assertThat(sensor.configurationKey()).isEqualTo("sonar.configuration.key");
  }

  @Test
  void do_nothing_if_property_not_configured() throws Exception {
    SensorContextTester context = createContext(PROJECT_DIR);
    PropertyHandlerSensorTester sensor = new PropertyHandlerSensorTester();
    sensor.execute(context);

    assertThat(ANALYSIS_WARNINGS).isEmpty();
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  void report_issue_if_file_missing() throws Exception {
    SensorContextTester context = createContext(PROJECT_DIR);
    PropertyHandlerSensorTester sensor = new PropertyHandlerSensorTester();
    context.settings().setProperty("sonar.configuration.key", "missing-file1.txt,missing-file2.txt,dummyReport.txt,missing-file3.txt");

    sensor.execute(context);
    List<String> infos = logTester.logs(LoggerLevel.INFO);
    assertThat(infos).hasSize(1);
    assertThat(infos.get(0))
      .startsWith("Importing")
      .endsWith("dummyReport.txt");

    List<String> warnings = logTester.logs(LoggerLevel.WARN);
    assertThat(warnings)
      .hasSize(1)
      .hasSameSizeAs(ANALYSIS_WARNINGS);
    assertThat(warnings.get(0))
      .startsWith("Unable to import propertyName report file(s):")
      .contains("missing-file1.txt")
      .contains("missing-file2.txt")
      .contains("missing-file3.txt")
      .doesNotContain("dummyReport.txt")
      .endsWith("The report file(s) can not be found. Check that the property 'sonar.configuration.key' is correctly configured.");
    assertThat(ANALYSIS_WARNINGS.get(0))
      .startsWith("Unable to import 3 propertyName report file(s).")
      .endsWith("Please check that property 'sonar.configuration.key' is correctly configured and the analysis logs for more details.");
  }

  private static SensorContextTester createContext(Path projectDir) throws IOException {
    SensorContextTester context = SensorContextTester.create(projectDir);
    Files.list(projectDir)
      .filter(file -> !Files.isDirectory(file))
      .forEach(file -> addFileToContext(context, projectDir, file));
    return context;
  }

  private static void addFileToContext(SensorContextTester context, Path projectDir, Path file) {
    try {
      String projectId = projectDir.getFileName().toString() + "-project";
      context.fileSystem().add(TestInputFileBuilder.create(projectId, projectDir.toFile(), file.toFile())
        .setCharset(UTF_8)
        .setLanguage("dummy")
        .setContents(new String(Files.readAllBytes(file), UTF_8))
        .setType(InputFile.Type.MAIN)
        .build());
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private static class PropertyHandlerSensorTester extends AbstractPropertyHandlerSensor {

    private PropertyHandlerSensorTester() {
      super(ANALYSIS_WARNINGS::add, "propertyKey", "propertyName", "sonar.configuration.key", "dummy");
    }

    @Override
    public Consumer<File> reportConsumer(SensorContext context) {
      return file -> { /* do nothing */ };
    }
  }
}