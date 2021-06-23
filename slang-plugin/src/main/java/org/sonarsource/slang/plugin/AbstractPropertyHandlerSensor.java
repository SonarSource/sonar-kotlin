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
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonarsource.analyzer.commons.ExternalReportProvider;

public abstract class AbstractPropertyHandlerSensor implements Sensor {

  private static final Logger LOG = Loggers.get(AbstractPropertyHandlerSensor.class);
  private final AnalysisWarnings analysisWarnings;
  private final String propertyKey;
  private final String propertyName;
  private final String configurationKey;
  private final String languageKey;

  protected AbstractPropertyHandlerSensor(AnalysisWarnings analysisWarnings, String propertyKey, String propertyName,
                                          String configurationKey, String languageKey) {
    this.analysisWarnings = analysisWarnings;
    this.propertyKey = propertyKey;
    this.propertyName = propertyName;
    this.configurationKey = configurationKey;
    this.languageKey = languageKey;
  }

  public final String propertyName() {
    return propertyName;
  }

  public final String propertyKey() {
    return propertyKey;
  }

  public final String configurationKey() {
    return configurationKey;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .onlyOnLanguage(languageKey)
      .onlyWhenConfiguration(conf -> conf.hasKey(configurationKey()))
      .name("Import of " + propertyName() + " issues");
  }

  @Override
  public void execute(SensorContext context) {
    executeOnFiles(reportFiles(context), reportConsumer(context));
  }

  public abstract Consumer<File> reportConsumer(SensorContext context);

  private void executeOnFiles(List<File> reportFiles, Consumer<File> action) {
    reportFiles.stream()
      .filter(File::exists)
      .forEach(file -> {
        LOG.info("Importing {}", file);
        action.accept(file);
      });
    reportMissingFiles(reportFiles);
  }

  private List<File> reportFiles(SensorContext context) {
    return ExternalReportProvider.getReportFiles(context, configurationKey());
  }

  private void reportMissingFiles(List<File> reportFiles) {
    List<String> missingFiles = reportFiles.stream()
      .filter(file -> !file.exists())
      .map(File::getPath)
      .collect(Collectors.toList());

    if (!missingFiles.isEmpty()) {
      String missingFilesAsString = missingFiles.stream().collect(Collectors.joining("\n- ", "\n- ", ""));
      String logWarning = String.format("Unable to import %s report file(s):%s%nThe report file(s) can not be found. Check that the property '%s' is correctly configured.",
        propertyName(), missingFilesAsString, configurationKey());
      LOG.warn(logWarning);

      String uiWarning = String.format("Unable to import %d %s report file(s).%nPlease check that property '%s' is correctly configured and the analysis logs for more details.",
        missingFiles.size(), propertyName(), configurationKey());
      analysisWarnings.addUnique(uiWarning);
    }
  }
}
