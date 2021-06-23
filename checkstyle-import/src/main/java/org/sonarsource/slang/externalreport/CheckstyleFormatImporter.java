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
package org.sonarsource.slang.externalreport;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonarsource.analyzer.commons.xml.SafeStaxParserFactory;

/**
 * Import external linter reports having a "Checkstyle" xml format into SonarQube
 */
public class CheckstyleFormatImporter {

  private static final Logger LOG = Loggers.get(CheckstyleFormatImporter.class);

  private static final Long DEFAULT_CONSTANT_DEBT_MINUTES = 5L;

  private static final QName CHECKSTYLE = new QName("checkstyle");
  private static final QName FILE = new QName("file");
  private static final QName ERROR = new QName("error");
  private static final QName NAME = new QName("name");
  private static final QName SEVERITY = new QName("severity");
  private static final QName SOURCE = new QName("source");
  private static final QName LINE = new QName("line");
  private static final QName MESSAGE = new QName("message");

  private final SensorContext context;

  private final String linterKey;

  private int level = 0;

  @Nullable
  private InputFile inputFile = null;

  /**
   * @param context, the context where issues will be sent
   * @param linterKey, used to specify the rule repository
   */
  public CheckstyleFormatImporter(SensorContext context, String linterKey) {
    this.context = context;
    this.linterKey = linterKey;
  }

  /**
   * "importFile" parses the given report file and imports the content into SonarQube
   * @param reportPath, path of the xml file
   */
  public void importFile(File reportPath) {
    try (InputStream in = new FileInputStream(reportPath)) {
      XMLEventReader reader = SafeStaxParserFactory.createXMLInputFactory().createXMLEventReader(in);
      level = 0;
      while (reader.hasNext()) {
        XMLEvent event = reader.nextEvent();
        if (event.isStartElement()) {
          level++;
          onElement(event.asStartElement());
        } else if (event.isEndElement()) {
          level--;
        }
      }
    } catch (IOException | XMLStreamException | RuntimeException e) {
      LOG.error("No issue information will be saved as the report file '{}' can't be read.", reportPath, e);
    }
  }

  private void onElement(StartElement element) throws IOException {
    if (level == 1 && !CHECKSTYLE.equals(element.getName())) {
      throw new IOException("Unexpected document root '" + element.getName().getLocalPart() + "' instead of 'checkstyle'.");
    } else if (level == 2 && FILE.equals(element.getName())) {
      onFileElement(element);
    } else if (level == 3 && ERROR.equals(element.getName()) && inputFile != null) {
      onErrorElement(element);
    }
  }

  private void onFileElement(StartElement element) {
    String filePath = getAttributeValue(element, NAME);
    if (filePath.isEmpty()) {
      inputFile = null;
      return;
    }
    FilePredicates predicates = context.fileSystem().predicates();
    inputFile = context.fileSystem().inputFile(predicates.or(
      predicates.hasAbsolutePath(filePath),
      predicates.hasRelativePath(filePath)));
    if (inputFile == null) {
      LOG.warn("No input file found for {}. No " + linterKey + " issues will be imported on this file.", filePath);
    }
  }

  private void onErrorElement(StartElement element) {
    String source = getAttributeValue(element, SOURCE);
    String line = getAttributeValue(element, LINE);
    // severity could be: error, warning, info
    String severity = getAttributeValue(element, SEVERITY);
    String message = getAttributeValue(element, MESSAGE);
    if (message.isEmpty()) {
      LOG.debug("Unexpected error without any message for rule: '{}'", source);
      return;
    }
    RuleKey ruleKey = createRuleKey(source);
    if (ruleKey != null) {
      saveIssue(ruleKey, line, severity, source, message);
    }
  }

  private void saveIssue(RuleKey ruleRepoAndKey, String line, String severity, String source, String message) {
    String ruleKey = ruleRepoAndKey.rule();
    NewExternalIssue newExternalIssue = context.newExternalIssue()
      .type(ruleType(ruleKey, severity, source))
      .severity(severity(ruleKey, severity))
      .remediationEffortMinutes(effort(ruleKey));

    NewIssueLocation primaryLocation = newExternalIssue.newLocation()
      .message(message)
      .on(inputFile);

    if (!line.isEmpty()) {
      primaryLocation.at(inputFile.selectLine(Integer.parseInt(line)));
    }

    newExternalIssue
      .at(primaryLocation)
      .engineId(ruleRepoAndKey.repository())
      .ruleId(ruleRepoAndKey.rule())
      .save();
  }

  @Nullable
  protected RuleKey createRuleKey(String source) {
    return RuleKey.of(linterKey, source);
  }

  /**
   * Return a RuleType equivalent based on the different parameters.
   *
   * @param ruleKey rule key of the current issue.
   * @param severity "severity" attribute's value of the report. Ex: "info", "error".
   * @param source "source" attribute's value of the report. Ex: "gosec", "detekt.MagicNumber".
   * @return the RuleType defined by the given parameters.
   */
  protected RuleType ruleType(String ruleKey, @Nullable String severity, String source) {
    return "error".equals(severity) ? RuleType.BUG : RuleType.CODE_SMELL;
  }

  /**
   * Return a Severity equivalent based on the different parameters.
   *
   * @param ruleKey rule key of the current issue.
   * @param severity "severity" attribute's value of the report. Ex: "info", "error".
   * @return the Severity defined by the given parameters.
   */
  protected Severity severity(String ruleKey, @Nullable String severity) {
    return "info".equals(severity) ? Severity.MINOR : Severity.MAJOR;
  }

  /**
   * Return an Effort value based on the ruleKey.
   *
   * @param ruleKey rule key of the current issue.
   * @return the Effort defined by the given ruleKey.
   */
  protected Long effort(String ruleKey) {
    return DEFAULT_CONSTANT_DEBT_MINUTES;
  }

  private static String getAttributeValue(StartElement element, QName attributeName) {
    Attribute attribute = element.getAttributeByName(attributeName);
    return attribute != null ? attribute.getValue() : "";
  }

}
