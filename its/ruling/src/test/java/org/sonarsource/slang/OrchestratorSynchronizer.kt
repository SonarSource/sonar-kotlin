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
package org.sonarsource.slang

import com.sonar.orchestrator.Orchestrator
import com.sonar.orchestrator.build.SonarScanner
import com.sonar.orchestrator.build.SonarScannerInstaller
import com.sonar.orchestrator.config.Configuration
import com.sonar.orchestrator.version.Version
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock

class OrchestratorSynchronizer {
    companion object {
        @JvmStatic
        private val ACTION_LOCK: ReentrantLock = ReentrantLock()

        @JvmStatic
        fun runSynchronized(action: Runnable) {
            ACTION_LOCK.lock()
            try {
                action.run()
            } finally {
                ACTION_LOCK.unlock()
            }
        }
    }
}

val CONFIGURATION: Configuration = Configuration.createEnv()
val REQUESTED_ORCHESTRATORS_KEY: AtomicInteger = AtomicInteger()
val IS_ORCHESTRATOR_READY: CountDownLatch = CountDownLatch(1)

fun startOrchestrator(orchestrator: Orchestrator) {
    if (REQUESTED_ORCHESTRATORS_KEY.getAndIncrement() == 0) {
        orchestrator.start()
        // installed scanner will be shared by all tests
        SonarScannerInstaller(CONFIGURATION.locators()).install(
            Version.create(SonarScanner.DEFAULT_SCANNER_VERSION),
            CONFIGURATION.fileSystem().workspace()
        )
        IS_ORCHESTRATOR_READY.countDown()
    } else {
        try {
            IS_ORCHESTRATOR_READY.await()
        } catch (e: InterruptedException) {
            throw IllegalStateException(e)
        }
    }
}
