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
package org.sonarsource.kotlin.plugin

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.event.Level
import org.sonar.api.batch.sensor.cache.ReadCache
import org.sonar.api.batch.sensor.cache.WriteCache
import org.sonarsource.kotlin.testapi.DummyInputFile
import org.sonarsource.kotlin.plugin.caching.ContentHashCache
import org.sonarsource.kotlin.testapi.AbstractSensorTest
import java.nio.file.Path
import java.security.MessageDigest

class ContentHashCacheTest : AbstractSensorTest() {

    private val pathToFile = Path.of("src", "test", "resources", "caching", "DummyFile.kt")
    private val pathToFileChanged = Path.of("src", "test", "resources", "caching", "DummyFileChanged.kt")
    private val dummyFile = DummyInputFile(pathToFile)
    private val dummyFileChanged = DummyInputFile(pathToFileChanged)
    private val hashAlgorithm = "MD5"
    private val messageDigest = MessageDigest.getInstance(hashAlgorithm)
    private val fileHash = messageDigest.digest(dummyFile.contents().byteInputStream().readAllBytes())

    @Test
    fun `test stored contents are present`() {
        val contentHashCache = contentHashCacheOf("kotlin:contentHash:MD5:DummyFile.kt", fileHash)
        assertThat(contentHashCache?.hasDifferentContentCached(dummyFile)).isFalse
    }

    @Test
    fun `test non-stored contents are not present`() {
        val contentHashCache = emptyContentHashCache()
        assertThat(contentHashCache?.hasDifferentContentCached(dummyFile)).isTrue
    }

    @Test
    fun `test changed file detection`() {
        val key = "kotlin:contentHash:MD5:DummyFileChanged.kt"
        val contentHashCache = contentHashCacheOf(key, fileHash)
        assertThat(contentHashCache?.hasDifferentContentCached(dummyFileChanged)).isTrue
    }

    @Test
    fun `test same key cannot be stored twice`() {
        val readCache = DummyReadCache(mapOf())
        val writeCache = DummyWriteCache()
        writeCache.write("kotlin:contentHash:MD5:DummyFile.kt", fileHash)
        val contentHashCache = cacheFromContextData(readCache, writeCache, true)
        contentHashCache?.hasDifferentContentCached(dummyFile)
        assertThat(logTester.logs(Level.WARN)).contains("Cache already contains key kotlin:contentHash:MD5:DummyFile.kt")
    }

    @Test
    fun `contentHashCache state is consistent with sensor context`() {
        val contentHashCacheEnabled = emptyContentHashCache()
        assertThat(contentHashCacheEnabled != null).isTrue
        context.isCacheEnabled = false
        val contentHashCacheDisabled = ContentHashCache.of(context)
        assertThat(contentHashCacheDisabled == null).isTrue
    }

    private fun emptyContentHashCache(): ContentHashCache? {
        val readCache = DummyReadCache(mapOf())
        val writeCache = DummyWriteCache(readCache = readCache)
        return cacheFromContextData(readCache, writeCache, true)
    }

    private fun contentHashCacheOf(key: String, hash: ByteArray): ContentHashCache? {
        val readCache = DummyReadCache(mapOf(key to hash))
        val writeCache = DummyWriteCache(readCache = readCache)
        return cacheFromContextData(readCache, writeCache, true)
    }

    private fun cacheFromContextData(readCache: ReadCache, writeCache: WriteCache, isEnabled: Boolean): ContentHashCache? {
        context.setPreviousCache(readCache)
        context.setNextCache(writeCache)
        context.isCacheEnabled = isEnabled
        return ContentHashCache.of(context)
    }

}