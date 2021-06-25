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

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.rules.ExternalResource;
import org.sonar.api.internal.google.common.collect.ArrayListMultimap;
import org.sonar.api.internal.google.common.collect.ListMultimap;

public class ThreadLocalLogTester extends ExternalResource {

  private static final Interceptor INTERCEPTOR = new Interceptor();

  @Override
  protected void before() {
    LogInterceptors.set(INTERCEPTOR);
    setLevel(LoggerLevel.INFO);
    INTERCEPTOR.clear();
  }

  @Override
  protected void after() {
    INTERCEPTOR.clear();
    setLevel(LoggerLevel.INFO);
  }

  LoggerLevel getLevel() {
    return Loggers.getFactory().getLevel();
  }

  /**
   * Enable/disable debug logs. Info, warn and error logs are always enabled.
   * By default INFO logs are enabled when LogTester is started.
   */
  public ThreadLocalLogTester setLevel(LoggerLevel level) {
    Loggers.getFactory().setLevel(level);
    return this;
  }

  /**
   * Logs in chronological order (item at index 0 is the oldest one)
   */
  public List<String> logs() {
    return INTERCEPTOR.logs();
  }

  /**
   * Logs in chronological order (item at index 0 is the oldest one) for
   * a given level
   */
  public List<String> logs(LoggerLevel level) {
    return INTERCEPTOR.logs(level);
  }

  public ThreadLocalLogTester clear() {
    INTERCEPTOR.clear();
    return this;
  }

  private static final class Interceptor implements LogInterceptor {

    private static final ThreadLocal<List<String>> LOGS = ThreadLocal.withInitial(ArrayList::new);
    private static final ThreadLocal<ListMultimap<LoggerLevel, String>> LOGS_BY_LEVEL = ThreadLocal.withInitial(ArrayListMultimap::create);

    private Interceptor() {
      // only one instance
    }

    @Override
    public void log(LoggerLevel level, String msg) {
      LOGS.get().add(msg);
      LOGS_BY_LEVEL.get().put(level, msg);
    }

    @Override
    public void log(LoggerLevel level, String msg, @Nullable Object arg) {
      String s = ConsoleFormatter.format(msg, arg);
      LOGS.get().add(s);
      LOGS_BY_LEVEL.get().put(level, s);
    }

    @Override
    public void log(LoggerLevel level, String msg, @Nullable Object arg1, @Nullable Object arg2) {
      String s = ConsoleFormatter.format(msg, arg1, arg2);
      LOGS.get().add(s);
      LOGS_BY_LEVEL.get().put(level, s);
    }

    @Override
    public void log(LoggerLevel level, String msg, Object... args) {
      String s = ConsoleFormatter.format(msg, args);
      LOGS.get().add(s);
      LOGS_BY_LEVEL.get().put(level, s);
    }

    @Override
    public void log(LoggerLevel level, String msg, Throwable thrown) {
      LOGS.get().add(msg);
      LOGS_BY_LEVEL.get().put(level, msg);
    }

    public List<String> logs() {
      return LOGS.get();
    }

    public List<String> logs(LoggerLevel level) {
      return LOGS_BY_LEVEL.get().get(level);
    }

    public void clear() {
      LOGS.get().clear();
      LOGS_BY_LEVEL.get().clear();
    }
  }
}
