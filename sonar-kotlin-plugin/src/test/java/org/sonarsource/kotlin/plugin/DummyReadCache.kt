/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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
import java.io.ByteArrayInputStream
import java.io.InputStream

class DummyReadCache(val cache: Map<String, ByteArray> = mutableMapOf()) : ReadCache {

    override fun read(key: String): InputStream {
        if (!cache.containsKey(key)) {
            throw IllegalArgumentException("cache")
        }
        val bytes = cache.get(key)
        return ByteArrayInputStream(bytes)
    }

    override fun contains(key: String) = cache.containsKey(key)
}