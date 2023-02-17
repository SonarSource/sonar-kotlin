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
    private var computedHashes = mutableMapOf<String, ByteArray>()

    constructor(sensorContext: SensorContext){
        this.enabled = sensorContext.isCacheEnabled
        if(this.enabled){
            LOG.info("Content Hash Cache was initialized")
            this.readCache = sensorContext.previousCache()
            this.writeCache = sensorContext.nextCache()
        }
    }

    fun isEnabled() = this.enabled

    fun hasDifferentContentCached(inputFile: InputFile): Boolean{
        val key = contentHashKey(inputFile)
        val cachedFileHash = read(key) ?: return true
        val inputFileHash = getHash(inputFile)
        computedHashes[key] = inputFileHash
        return !MessageDigest.isEqual(inputFileHash, cachedFileHash)
    }

    fun writeContentHash(inputFile: InputFile){
        val key = contentHashKey(inputFile)
        if(computedHashes.contains(key)){
            LOG.trace("Using already processed hash for key: $key")
            write(key, computedHashes.remove(key)!!)
        }else{
            write(key, getHash(inputFile))
        }
    }

    fun copyFromPreviousAnalysis(inputFile: InputFile){
        if(isEnabled()){
            val key = contentHashKey(inputFile)
            if(readCache!!.contains(key)){
                writeCache!!.copyFromPrevious(key)
                LOG.trace("Copied file $key from previous cache.")
            }else{
                writeContentHash(inputFile)
            }
        }else{
            LOG.trace("Cannot write to cache when disabled.")
        }
    }

    private fun write(key: String, hash: ByteArray){
        if(isEnabled()){
            LOG.trace("Storing file content hash for key: $key")
            writeCache!!.write(key, hash)
        }else{
            LOG.trace("Cannot write to cache when disabled.")
        }
    }

    private fun read(key: String): ByteArray? {
        if(enabled){
            if(readCache!!.contains(key)){
                val stream = readCache!!.read(key)
                return stream.readAllBytes().also { stream.close() }
            }else{
                LOG.trace("Key $key was not found in ContentHashCache")
            }
        }else{
            LOG.trace("Cannot read from cache when disabled.")
        }
        return null
    }

    private fun getHash(inputFile: InputFile): ByteArray{
        val inputFileBytes = inputFile.contents().byteInputStream().readAllBytes()
        return messageDigest.digest(inputFileBytes)
    }

    private fun contentHashKey(inputFile: InputFile) = CONTENT_HASHES_KEY + inputFile.key().replace('\\', '/')

}