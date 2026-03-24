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
package kotlinx.coroutines.internal.intellij

import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

/**
 * Substitute implementation for https://github.com/JetBrains/intellij-deps-kotlinx.coroutines/blob/bb5a8ba1ec84c0f6311c7b71b556408273458e64/kotlinx-coroutines-core/jvm/src/internal/intellij/intellij.kt
 * based on https://github.com/JetBrains/intellij-community/blob/253/platform/util/src/com/intellij/util/IntelliJCoroutinesFacade.kt.
 *
 * We only need the "old" behavior, where IDE coroutines are not used.
 *
 * Note: IntellijCoroutinesFacade is introduced in IJ SDK 253. Kotlin 2.3.20 is using IJ SDK 251.x; previously it was 241.x.
 */
@Suppress("unused")
object IntellijCoroutines {
    fun currentThreadCoroutineContext(): CoroutineContext? = null

    @Throws(InterruptedException::class)
    fun <T> runBlockingWithParallelismCompensation(
        context: CoroutineContext,
        block: suspend CoroutineScope.() -> T
    ) = runBlocking(context, block)

    fun <T> runAndCompensateParallelism(timeout: Duration, action: () -> T) = action()
}
