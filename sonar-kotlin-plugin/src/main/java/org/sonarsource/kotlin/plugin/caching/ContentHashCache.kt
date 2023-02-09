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

import org.sonar.api.SonarProduct
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.batch.sensor.cache.ReadCache
import org.sonar.api.batch.sensor.cache.WriteCache
import org.sonar.api.utils.log.Loggers
import java.security.MessageDigest

private val LOG = Loggers.get(ContentHashCache::class.java)
private const val HASH_ALGORITHM = "MD5"
private val messageDigest = MessageDigest.getInstance(HASH_ALGORITHM)
const val CONTENT_HASHES_KEY = "kotlin:contentHash:$HASH_ALGORITHM:"

class ContentHashCache {

    private var enabled = false
    private var readCache: ReadCache? = null
    private var writeCache: WriteCache? = null
    private var lastKeyChecked = ""
    private var lastHashChecked = byteArrayOf()

    fun init(readCache: ReadCache, writeCache: WriteCache){
        LOG.info("Content Hash Cache was initialized")
        this.readCache = readCache
        this.writeCache = writeCache
        this.enabled = true
    }

    fun init(sensorContext: SensorContext){
        if(!sensorContext.runtime().product.equals(SonarProduct.SONARLINT) && sensorContext.isCacheEnabled){
            init(sensorContext.previousCache(), sensorContext.nextCache())
        }else{
            this.enabled = false
        }
    }

    fun hasDifferentContentCached(inputFile: InputFile): Boolean{
        val key = contentHashKey(inputFile)
        val inputFileHash = getHash(inputFile)
        storeLastKeyAndHashChecked(key, inputFileHash)
        val cachedFileHash = read(key)
        return !MessageDigest.isEqual(inputFileHash, cachedFileHash)
    }

    fun writeContentHash(inputFile: InputFile){
        val key = contentHashKey(inputFile)
        if(lastKeyChecked.equals(key)){
            LOG.info("Using already processed hash for key: $key")
            write(key, lastHashChecked)
        }else{
            write(key, getHash(inputFile))
        }
    }

    private fun write(key: String, hash: ByteArray){
        LOG.debug("Storing file content hash for key: $key")
        writeCache?.write(key, hash) ?: LOG.error("Cannot write to cache when disabled.")
    }

    private fun read(key: String): ByteArray? {
        if(enabled){
            if( readCache!!.contains(key)){
                val stream = readCache!!.read(key)
                return stream.readAllBytes().also { stream.close() }
            }else{
                LOG.info("Key $key was not found in ContentHashCache")
            }
        }else{
            LOG.error("Cannot read from cache when disabled.")
        }
        return null
    }

    private fun getHash(inputFile: InputFile): ByteArray{
        val inputFileBytes = inputFile.contents().byteInputStream().readAllBytes()
        return messageDigest.digest(inputFileBytes)
    }

    private fun contentHashKey(inputFile: InputFile) = CONTENT_HASHES_KEY + inputFile.key().replace('\\', '/')

    private fun storeLastKeyAndHashChecked(key: String, hash: ByteArray){
        lastKeyChecked = key
        lastHashChecked = hash
    }

    fun isEnabled() = this.enabled

}