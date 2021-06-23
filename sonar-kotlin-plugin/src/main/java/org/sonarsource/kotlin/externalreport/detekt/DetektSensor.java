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
package org.sonarsource.kotlin.externalreport.detekt;

import java.io.File;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonarsource.kotlin.plugin.KotlinPlugin;
import org.sonarsource.slang.externalreport.CheckstyleFormatImporterWithRuleLoader;
import org.sonarsource.slang.plugin.AbstractPropertyHandlerSensor;

public class DetektSensor extends AbstractPropertyHandlerSensor {

  private static final Logger LOG = Loggers.get(DetektSensor.class);

  static final String LINTER_KEY = "detekt";

  static final String LINTER_NAME = "detekt";

  private static final String DETEKT_PREFIX = "detekt.";

  public static final String REPORT_PROPERTY_KEY = "sonar.kotlin.detekt.reportPaths";

  public DetektSensor(AnalysisWarnings analysisWarnings) {
    super(analysisWarnings, LINTER_KEY, LINTER_NAME, REPORT_PROPERTY_KEY, KotlinPlugin.KOTLIN_LANGUAGE_KEY);
  }

  @Override
  public Consumer<File> reportConsumer(SensorContext context) {
    return new ReportImporter(context)::importFile;
  }

  private static class ReportImporter extends CheckstyleFormatImporterWithRuleLoader {

    public ReportImporter(SensorContext context) {
      super(context, LINTER_KEY, DetektRulesDefinition.RULE_LOADER);
    }

    @Override
    @Nullable
    protected RuleKey createRuleKey(String source) {
      if (!source.startsWith(DETEKT_PREFIX)) {
        LOG.debug("Unexpected rule key without '{}' suffix: '{}'", DETEKT_PREFIX, source);
        return null;
      }
      return super.createRuleKey(source.substring(DETEKT_PREFIX.length()));
    }

  }

}
