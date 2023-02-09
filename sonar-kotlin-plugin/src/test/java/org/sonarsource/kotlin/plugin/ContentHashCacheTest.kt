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

import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat
import org.sonar.api.internal.SonarRuntimeImpl
import org.sonar.api.utils.log.LoggerLevel
import org.sonarsource.kotlin.DummyInputFile
import org.sonarsource.kotlin.DummyReadCache
import org.sonarsource.kotlin.DummyWriteCache
import org.sonarsource.kotlin.plugin.caching.ContentHashCache
import org.sonarsource.kotlin.testing.AbstractSensorTest
import java.nio.file.Path
import java.security.MessageDigest

class ContentHashCacheTest: AbstractSensorTest() {

    private val pathToFile = Path.of("src", "test", "resources", "caching", "DummyFile.kt")
    private val pathToFileChanged = Path.of("src", "test", "resources", "caching", "DummyFileChanged.kt")
    private val dummyFile = DummyInputFile(pathToFile)
    private val dummyFileChanged = DummyInputFile(pathToFileChanged)
    private val HASH_ALGORITHM = "MD5"
    private val messageDigest = MessageDigest.getInstance(HASH_ALGORITHM)
    private val fileHash = messageDigest.digest(dummyFile.contents().byteInputStream().readAllBytes())

    @Test
    fun `test stored contents are present` (){
        val contentHashCache = contentHashCacheOf( "kotlin:contentHash:MD5:DummyFile.kt" to fileHash )
        assertThat(contentHashCache.hasDifferentContentCached(dummyFile)).isFalse
    }

    @Test
    fun `test non-stored contents are not present` (){
        val contentHashCache = emptyContentHashCache()
        assertThat(contentHashCache.hasDifferentContentCached(dummyFile)).isTrue
    }

    @Test
    fun `test changed file detection` (){
        val contentHashCache = contentHashCacheOf( "kotlin:contentHash:MD5:DummyFileChanged.kt" to fileHash )
        assertThat(contentHashCache.hasDifferentContentCached(dummyFileChanged)).isTrue
    }

    @Test
    fun `test hashes are not computed twice`(){
        val contentHashCache = emptyContentHashCache()
        if(contentHashCache.hasDifferentContentCached(dummyFile)){
            contentHashCache.writeContentHash(dummyFile)
        }
        assertThat(logTester.logs(LoggerLevel.INFO)).contains("Using already processed hash for key: kotlin:contentHash:MD5:DummyFile.kt")
    }

    @Test
    fun `test hashes are computed when needed`(){
        val contentHashCache = emptyContentHashCache()
        if(contentHashCache.hasDifferentContentCached(dummyFile)){
            contentHashCache.writeContentHash(dummyFileChanged)
        }
        assertThat(logTester.logs(LoggerLevel.INFO)).doesNotContain("Using already processed hash for key: kotlin:contentHash:MD5:DummyFileChanged.kt")
    }

    @Test
    fun `test sensor context`(){
        val contentHashCacheEnabled = ContentHashCache()
        context.isCacheEnabled = true
        context.setPreviousCache(DummyReadCache(mapOf()))
        context.setNextCache(DummyWriteCache())
        contentHashCacheEnabled.init(context)
        assertThat(contentHashCacheEnabled.isEnabled())

        val contentHashCacheDisabled = ContentHashCache()
        context.isCacheEnabled=false
        contentHashCacheDisabled.init(context)
        assertThat(!contentHashCacheDisabled.isEnabled())

        val contentHashCacheSL = ContentHashCache()
        context.isCacheEnabled=true
        val runtime = SonarRuntimeImpl.forSonarLint(context.runtime().apiVersion)
        context.setRuntime(runtime)
        contentHashCacheSL.init(context)
        assertThat(!contentHashCacheSL.isEnabled())
    }

    @Test
    fun `test operations on disabled cache`(){
        val contentHashCacheDisabled = ContentHashCache()
        context.isCacheEnabled=false
        contentHashCacheDisabled.init(context)

        contentHashCacheDisabled.writeContentHash(dummyFile)
        assertThat(logTester.logs(LoggerLevel.ERROR)).containsOnly("Cannot write to cache when disabled.")

        contentHashCacheDisabled.hasDifferentContentCached(dummyFile)
        assertThat(logTester.logs(LoggerLevel.ERROR)).contains("Cannot read from cache when disabled.")
    }

    fun emptyContentHashCache(): ContentHashCache{
        val cache = ContentHashCache()
        val readCache = DummyReadCache(mapOf())
        val writeCache = DummyWriteCache()
        cache.init(readCache, writeCache)
        return cache
    }

    fun contentHashCacheOf( previousValue: Pair<String, ByteArray> ): ContentHashCache{
        val cache = ContentHashCache()
        val readCache = DummyReadCache(mapOf( previousValue ))
        val writeCache = DummyWriteCache()
        cache.init(readCache, writeCache)
        return cache
    }

}