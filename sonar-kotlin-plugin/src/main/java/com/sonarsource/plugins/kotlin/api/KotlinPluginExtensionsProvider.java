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
package com.sonarsource.plugins.kotlin.api;

import org.sonar.api.server.ServerSide;
import org.sonarsource.api.sonarlint.SonarLintSide;

/**
 * Note that this is EXPERIMENTAL API with NO COMPATIBILITY guarantees.
 * Classes implementing this interface must be public and have public no-arg constructor.
 */
@SonarLintSide
@ServerSide
public interface KotlinPluginExtensionsProvider {

  void registerKotlinPluginExtensions(Extensions extensions);

  interface Extensions {
    void registerRepository(String repositoryKey, String name);
    void registerRule(String repositoryKey, Class<?> ruleClass, boolean enabledInSonarWay);
  }

}
