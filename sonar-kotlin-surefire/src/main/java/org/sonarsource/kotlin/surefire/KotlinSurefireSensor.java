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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.config.Configuration;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonarsource.kotlin.metrics.TelemetryData;
import org.sonarsource.kotlin.surefire.api.SurefireUtils;

import java.io.File;
import java.util.List;

public class KotlinSurefireSensor implements Sensor {
  private static final Logger LOGGER = LoggerFactory.getLogger(KotlinSurefireSensor.class);

  private final KotlinSurefireParser kotlinSurefireParser;
  private final Configuration settings;
  private final PathResolver pathResolver;
  private final TelemetryData telemetryData;

  public KotlinSurefireSensor(
    KotlinSurefireParser kotlinSurefireParser,
    Configuration settings,
    PathResolver pathResolver,
    TelemetryData telemetryData) {
    this.kotlinSurefireParser = kotlinSurefireParser;
    this.settings = settings;
    this.pathResolver = pathResolver;
    this.telemetryData = telemetryData;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.onlyOnLanguage("kotlin").name("KotlinSurefireSensor");
  }

  @Override
  public void execute(SensorContext context) {
    List<File> dirs = SurefireUtils.getReportsDirectories(settings, context.fileSystem(), pathResolver);
    collect(context, dirs);
  }

  protected void collect(SensorContext context, List<File> reportsDirs) {
    LOGGER.info("parsing {}", reportsDirs);
    kotlinSurefireParser.collect(context, reportsDirs, settings.hasKey(SurefireUtils.SUREFIRE_REPORT_PATHS_PROPERTY), telemetryData);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
