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
import java.util.List;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.config.Configuration;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonarsource.kotlin.plugin.surefire.api.SurefireUtils;

public class KotlinSurefireSensor implements Sensor {
  private static final Logger LOGGER = Loggers.get(KotlinSurefireSensor.class);

  private final KotlinSurefireParser kotlinSurefireParser;
  private final Configuration settings;
  private final PathResolver pathResolver;

  public KotlinSurefireSensor(KotlinSurefireParser kotlinSurefireParser, Configuration settings, PathResolver pathResolver) {
    this.kotlinSurefireParser = kotlinSurefireParser;
    this.settings = settings;
    this.pathResolver = pathResolver;
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
    kotlinSurefireParser.collect(context, reportsDirs, settings.hasKey(SurefireUtils.SUREFIRE_REPORT_PATHS_PROPERTY));
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
