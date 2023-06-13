/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2023 SonarSource SA
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
package org.sonarsource.kotlin.plugin.caching

import org.slf4j.LoggerFactory
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.batch.sensor.cache.ReadCache
import org.sonar.api.batch.sensor.cache.WriteCache
import org.sonarsource.kotlin.api.checks.hasCacheEnabled
import org.sonarsource.kotlin.api.logging.trace
import java.security.MessageDigest

private val LOG = LoggerFactory.getLogger(ContentHashCache::class.java)
private const val HASH_ALGORITHM = "MD5"
private val messageDigest = MessageDigest.getInstance(HASH_ALGORITHM)
private const val CONTENT_HASHES_KEY = "kotlin:contentHash:$HASH_ALGORITHM:"


internal fun contentHashKey(inputFile: InputFile) = CONTENT_HASHES_KEY + inputFile.key().replace('\\', '/')

class ContentHashCache private constructor(private val readCache: ReadCache, private val writeCache: WriteCache) {

    companion object {
        fun of(ctx: SensorContext): ContentHashCache? = if (ctx.hasCacheEnabled()) {
            LOG.debug("Content hash cache was initialized")
            ContentHashCache(ctx.previousCache(), ctx.nextCache())
        } else {
            LOG.debug("Content hash cache is disabled")
            null
        }
    }

    /**
     * Checks if the inputFile has a different content hash cached.
     * If that is the case, or the file has never been cached before, takes care of writing a new entry to the cache.
     * Otherwise, it will copy the content hash from the previous cache.
     */
    fun hasDifferentContentCached(inputFile: InputFile): Boolean {
        val key = contentHashKey(inputFile)
        read(key)?.let { cachedFileHash ->
            val inputFileHash = getHash(inputFile)
            val cacheContentIsEqual = MessageDigest.isEqual(inputFileHash, cachedFileHash)
            if (cacheContentIsEqual) {
                try {
                    writeCache.copyFromPrevious(key)
                    LOG.trace { "Cache contained same hash for file ${inputFile.filename()}" }
                } catch (_: IllegalArgumentException) {
                    LOG.warn("Cannot copy key $key from cache as it has already been written")
                }
                return false
            }
        }
        write(key, getHash(inputFile))
        LOG.trace { "Cache contained a different hash for file ${inputFile.filename()}" }
        return true
    }

    private fun write(key: String, hash: ByteArray) {
        try {
            writeCache.write(key, hash)
        } catch (_: IllegalArgumentException) {
            LOG.warn("Cache already contains key $key")
        }
    }

    private fun read(key: String) = if (readCache.contains(key)) {
        readCache.read(key).use { it.readAllBytes() }
    } else {
        null
    }

    private fun getHash(inputFile: InputFile) =
        inputFile.contents().byteInputStream().use { it.readAllBytes() }.let { messageDigest.digest(it) }
}