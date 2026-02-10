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
package org.sonarsource.kotlin.metrics

import java.util.concurrent.atomic.AtomicInteger
import org.sonar.api.scanner.ScannerSide
import org.sonarsource.api.sonarlint.SonarLintSide

@ScannerSide
@SonarLintSide
class TelemetryData {
    var hasAndroidImports = false
    val surefireReportsImported = AtomicInteger(0)
    val surefireReportsFailed = AtomicInteger(0)
}
