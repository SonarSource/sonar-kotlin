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
package org.sonar.api.utils.log;

import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;

import static org.assertj.core.api.Assertions.assertThat;

@EnableRuleMigrationSupport
class ThreadLocalLogTesterTest {

  private static final Logger LOG = Loggers.get(ThreadLocalLogTesterTest.class);

  @Rule
  public ThreadLocalLogTester logTester = new ThreadLocalLogTester();

  @Test
  void log_error() {
    LOG.error("BOOM in a test.");
    LOG.error("BOOM in a {}.", "test");
    LOG.error("BOOM in {} {}.", "a", "test");
    LOG.error("BOOM {} {} {}.", "in", "a", "test");
    LOG.error("BOOM in a test.", new RuntimeException("BOOM"));
    assertThat(logTester.logs()).containsExactly("BOOM in a test.", "BOOM in a test.", "BOOM in a test.", "BOOM in a test.", "BOOM in a test.");
    assertThat(logTester.logs(LoggerLevel.ERROR)).hasSize(5);
    assertThat(logTester.logs(LoggerLevel.WARN)).isEmpty();
    assertThat(logTester.getLevel()).isEqualTo(LoggerLevel.INFO);
    logTester.clear();
    assertThat(logTester.logs()).isEmpty();
  }
}
