package org.sonarsource.kotlin

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
        if (!readCache.contains(key)) {
            throw IllegalArgumentException()
        }
        cache[key] = readCache.read(key).readAllBytes()
    }
}