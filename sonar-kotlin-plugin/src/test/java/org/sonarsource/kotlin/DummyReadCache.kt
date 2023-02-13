package org.sonarsource.kotlin

import org.sonar.api.batch.sensor.cache.ReadCache
import java.io.ByteArrayInputStream
import java.io.InputStream

class DummyReadCache(cache: Map<String, ByteArray>): ReadCache {
    private val cache = cache

    override fun read(key: String): InputStream {
        if (!cache.containsKey(key)) {
            throw IllegalArgumentException("cache")
        }
        val bytes = cache.get(key)
        return ByteArrayInputStream(bytes)
    }

    override fun contains(key: String) = cache.containsKey(key)
}