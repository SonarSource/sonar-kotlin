/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
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

import org.sonar.api.batch.sensor.cache.ReadCache
import org.sonar.api.batch.sensor.cache.WriteCache
import java.io.InputStream

class DummyWriteCache(
    val cache: MutableMap<String, ByteArray> = mutableMapOf(),
    val readCache: ReadCache = DummyReadCache(mapOf())
) : WriteCache {
    override fun write(key: String, data: InputStream) {
        val dataAsByteArray = data.readAllBytes()
        write(key, dataAsByteArray)
    }

    override fun write(key: String, data: ByteArray) {
        if (cache.contains(key)) {
            throw IllegalArgumentException()
        }
        cache[key] = data
    }

    override fun copyFromPrevious(key: String) {
        if (!readCache.contains(key) || cache.contains(key)) {
            throw IllegalArgumentException()
        }
        cache[key] = readCache.read(key).readAllBytes()
    }
}