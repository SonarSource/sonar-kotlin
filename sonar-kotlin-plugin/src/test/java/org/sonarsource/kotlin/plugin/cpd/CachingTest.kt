/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2024 SonarSource SA
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
package org.sonarsource.kotlin.plugin.cpd

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.slf4j.event.Level
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.sensor.cache.ReadCache
import org.sonar.api.testfixtures.log.LogTesterJUnit5
import org.sonarsource.kotlin.testapi.DummyInputFile
import org.sonarsource.kotlin.plugin.DummyReadCache
import org.sonarsource.kotlin.plugin.DummyWriteCache
import java.nio.file.Path

class CachingTest {
    @JvmField
    @RegisterExtension
    val logTester = LogTesterJUnit5()

    private val pathToFile = Path.of("src", "test", "resources", "cpd", "MyFile.kt")
    private val file: InputFile = DummyInputFile(pathToFile)
    private val token = CPDToken(file.newRange(1, 1, 0, 12), "package cpd")
    private val tokens = listOf(token, token)
    private val encodedToken =
        byteArrayOf(49, 44, 49, 44, 48, 44, 49, 50, 44, 112, 97, 99, 107, 97, 103, 101, 32, 99, 112, 100)

    private val readCache: ReadCache = DummyReadCache(
        mapOf(
            "kotlin:cpdTokens:MyFile.kt" to encodedToken
        )
    )

    @Test
    fun `loadCPDTokens returns null when the cache is empty`() {
        val emptyReadCache = DummyReadCache(mapOf())
        assertThat(emptyReadCache.loadCPDTokens(file)).isNull()
    }

    @Test
    fun `loadCPDTokens returns the expected list of CPD tokens`() {
        assertThat(readCache.loadCPDTokens(file))
            .hasSize(1)
            .containsExactly(token)
    }


    @Test
    fun `storeCPDTokens writes to the cache`() {
        val emptyReadCache = DummyReadCache(mapOf())
        val writeCache = DummyWriteCache(readCache = emptyReadCache)
        assertThat(writeCache.storeCPDTokens(file, tokens)).isEqualTo("kotlin:cpdTokens:MyFile.kt")
        assertThat(writeCache.cache)
            .hasSize(1)
            .containsKey("kotlin:cpdTokens:MyFile.kt")
    }

    @Test
    fun `storeCPDTokens logs at warning level when trying to overwrite an entry under an existing key`() {
        val emptyReadCache = DummyReadCache(mapOf())
        val writeCache = DummyWriteCache(readCache = emptyReadCache)
        assertThat(writeCache.cache).isEmpty()

        val key = "kotlin:cpdTokens:MyFile.kt"

        writeCache.storeCPDTokens(file, tokens)
        assertThat(writeCache.cache).hasSize(1)

        assertThat(writeCache.storeCPDTokens(file, tokens)).isEqualTo(key)
        assertThat(writeCache.cache).hasSize(1)

        val logs = logTester.getLogs(Level.WARN).map { it.rawMsg }
        assertThat(logs)
            .contains("Could not write CPD tokens under key $key in cache.")
    }

    @Test
    fun `copyCPDTokensFromPrevious copies the data from the previous analysis cache to the cache of the next analysis`() {
        val key = "kotlin:cpdTokens:MyFile.kt"
        val cachedData = mapOf(key to encodedToken)
        val readCache = DummyReadCache(cachedData)
        val writeCache = DummyWriteCache(readCache = readCache)
        assertThat(writeCache.cache).isEmpty()

        assertThat(writeCache.copyCPDTokensFromPrevious(file)).isEqualTo("kotlin:cpdTokens:MyFile.kt")
        assertThat(writeCache.cache)
            .hasSize(1)
            .containsKey(key)

        assertThat(writeCache.cache[key]).isEqualTo(encodedToken)
    }

    @Test
    fun `copyCPDTokensFromPrevious logs at warning level and throws an IllegalArgumentException when the read cache does not contain the existing key`() {
        val key = "kotlin:cpdTokens:MyFile.kt"
        val emptyReadCache = DummyReadCache(emptyMap())
        val writeCache = DummyWriteCache(readCache = emptyReadCache)

        assertThatThrownBy { (writeCache.copyCPDTokensFromPrevious(file)) }
            .isExactlyInstanceOf(IllegalArgumentException::class.java)

        val logs = logTester.getLogs(Level.WARN).map { it.rawMsg }
        assertThat(logs)
            .contains("Could not copy CPD tokens from previous analysis for key $key.")
    }

    @Test
    fun `serialize results can be reversed by deserialize`() {
        val serialized = serialize(tokens)

        val expectedSerialized = byteArrayOf(
            49, 44, 49, 44, 48, 44, 49, 50, 44, 112, 97, 99, 107, 97, 103, 101, 32, 99, 112, 100,
            31,
            49, 44, 49, 44, 48, 44, 49, 50, 44, 112, 97, 99, 107, 97, 103, 101, 32, 99, 112, 100
        )

        assertThat(serialized).isEqualTo(expectedSerialized)

        val deserialized = deserialize(file, serialized)
        assertThat(deserialized).containsExactlyInAnyOrderElementsOf(tokens)
    }

    @Test
    fun `deserialize returns an empty list when trying to deserialize an empty byte array`() {
        logTester.setLevel(Level.TRACE)

        assertThat(deserialize(file, ByteArray(0))).isEmpty()
        val logs = logTester.getLogs(Level.TRACE).map { it.formattedMsg }
        assertThat(logs).contains("0 out of 1 CPD token(s) were successfully deserialized for file MyFile.kt.")
    }

    @Test
    fun `deserialize returns an empty list when trying to deserialize a corrupted byte array`() {
        assertThat(deserialize(file, "1,1,1,1".encodeToByteArray())).isEmpty()

        assertThat(deserialize(file, ",1,1,10,fun foo(){".encodeToByteArray())).isEmpty()
        assertThat(deserialize(file, "1,,1,10,fun foo(){".encodeToByteArray())).isEmpty()
        assertThat(deserialize(file, "1,1,,10,fun foo(){".encodeToByteArray())).isEmpty()
        assertThat(deserialize(file, "1,1,1,,fun foo(){".encodeToByteArray())).isEmpty()

        assertThat(deserialize(file, "x,1,1,10,fun foo(){".encodeToByteArray())).isEmpty()
        assertThat(deserialize(file, "1,x,1,10,fun foo(){".encodeToByteArray())).isEmpty()
        assertThat(deserialize(file, "1,1,x,10,fun foo(){".encodeToByteArray())).isEmpty()
        assertThat(deserialize(file, "1,1,1,x,fun foo(){".encodeToByteArray())).isEmpty()

        assertThat(deserialize(file, "1,1,10,fun foo(){".encodeToByteArray())).isEmpty()
        assertThat(deserialize(file, "1,1,1,fun foo(){".encodeToByteArray())).isEmpty()

        assertThat(deserialize(file, "".encodeToByteArray())).isEmpty()
    }

    @Test
    fun `deserialize logs at warning level and returns an empty list of tokens when an unexpected exception is caught`() {
        val mockInputFile = mockk<InputFile>()
        every {
            mockInputFile.newRange(
                any(),
                any(),
                any(),
                any()
            )
        } throws RuntimeException("A fake exception for testing purposes")
        every { mockInputFile.key() } returns "MyMockedFile.kt"

        assertThat(deserialize(mockInputFile, "1,1,1,10,fun foo(){".encodeToByteArray())).isEmpty()

        val logs = logTester.getLogs(Level.WARN).map { it.formattedMsg }

        assertThat(logs).contains("An unexpected RuntimeException was caught when trying to deserialize a CPD token of MyMockedFile.kt: A fake exception for testing purposes")
    }
}
