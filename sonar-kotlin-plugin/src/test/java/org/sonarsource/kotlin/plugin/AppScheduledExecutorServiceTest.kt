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
package org.sonarsource.kotlin.plugin

import com.intellij.util.concurrency.AppScheduledExecutorService
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.ScheduledExecutorService

private class AppScheduledExecutorServiceTest {
    /**
     * Non-daemon thread created by
     * [KaFirSessionProvider](https://github.com/JetBrains/kotlin/blob/v2.1.10/analysis/analysis-api-fir/src/org/jetbrains/kotlin/analysis/api/fir/KaFirSessionProvider.kt#L67)
     * might prevent JVM termination - for example in case of [AssertionError]
     * [`ScannerMain` does not execute `System.exit`](https://github.com/SonarSource/sonar-enterprise/blob/sqs-10.8.1.101195/sonar-scanner-engine/src/main/java/org/sonar/scanner/bootstrap/ScannerMain.java#L51-L74).
     */
    @Test
    fun `should use daemon threads`() {
        val getInstanceMethod = AppScheduledExecutorService::class.java.getDeclaredMethod("getInstance")
        getInstanceMethod.isAccessible = true
        val instance: ScheduledExecutorService = getInstanceMethod.invoke(null) as ScheduledExecutorService
        instance.submit {
            assertTrue(Thread.currentThread().isDaemon)
        }.get()
    }
}
