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
package org.sonarsource.kotlin.plugin.cpd

import org.slf4j.LoggerFactory
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.sensor.cache.ReadCache
import org.sonar.api.batch.sensor.cache.WriteCache
import org.sonarsource.kotlin.plugin.KotlinSensor

private const val DELIMITER: Char = 31.toChar() // ASCII unit delimiter

private val LOG = LoggerFactory.getLogger(KotlinSensor::class.java)

/**
 * Load the CPD tokens stored in the cache during a previous analysis.
 *
 * Returns null if no entry can be found for the file.
 *
 * @receiver ReadCache
 * @param file The input file for which CPD tokens should be loaded
 * @return The list of tokens stored in a previous analysis for a given input file.
 */
fun ReadCache.loadCPDTokens(file: InputFile): List<CPDToken>? {
    val key = computeCPDTokensCacheKey(file)
    if (!contains(key)) {
        return null
    }
    val serialized = read(key).readAllBytes()
    return deserialize(file, serialized)
}

/**
 * Store a given file's CPD tokens in the cache for the next analysis.
 *
 * @receiver WriteCache
 * @param file The file for which the CPD tokens are saved
 * @param tokens The collection of CPD tokens for the next analysis
 * @return The key under which the cpd tokens were stored in the cache
 * @throws IllegalArgumentException If tokens have already been written in the cache for this file
 */
fun WriteCache.storeCPDTokens(file: InputFile, tokens: List<CPDToken>): String {
    val key = computeCPDTokensCacheKey(file)
    val data = serialize(tokens)
    try {
        write(key, data)
    } catch (_: IllegalArgumentException) {
        LOG.warn("Could not write CPD tokens under key $key in cache.")
    }
    return key
}

/**
 * Copy the CPD tokens from the previous analysis cache to the cache of the next analysis.
 *
 * @receiver WriteCache
 * @param file The file for which the CPD tokens are copied
 * @return The key under which the cpd tokens were stored in the cache
 * @throws IllegalArgumentException If the previous cache does not contain CPD tokens for this file or tokens have
 * already been written in the cache for this file
 */
fun WriteCache.copyCPDTokensFromPrevious(file: InputFile): String {
    val key = computeCPDTokensCacheKey(file)
    try {
        copyFromPrevious(key)
    } catch (toRethrow: IllegalArgumentException) {
        LOG.warn("Could not copy CPD tokens from previous analysis for key $key.")
        throw toRethrow
    }
    return key
}

internal fun computeCPDTokensCacheKey(file: InputFile) = "kotlin:cpdTokens:${file.key()}"

internal fun serialize(tokens: List<CPDToken>): ByteArray =
    tokens.joinToString(separator = DELIMITER.toString()) {
        "${it.range.start().line()},${it.range.start().lineOffset()},${it.range.end().line()},${
            it.range.end().lineOffset()
        },${it.text}"
    }.encodeToByteArray()

internal fun deserialize(inputFile: InputFile, serialized: ByteArray): List<CPDToken> {
    val stringTokens = serialized.decodeToString().split(DELIMITER)
    return stringTokens.mapNotNull { stringToCPDToken(inputFile, it) }.also {
        LOG.trace("${it.size} out of ${stringTokens.size} CPD token(s) were successfully deserialized for file ${inputFile.key()}.")
    }
}

/* Visible for testing */
private fun stringToCPDToken(inputFile: InputFile, serialized: String): CPDToken? {
    if (serialized.isBlank()) {
        return null
    }
    return try {
        val tokens = serialized.split(",", limit = 5)
        val range = inputFile.newRange(
            tokens[0].toInt(),
            tokens[1].toInt(),
            tokens[2].toInt(),
            tokens[3].toInt()
        )
        val text = tokens[4]
        CPDToken(range, text)
    } catch (exception: RuntimeException) {
        when (exception) {
            is IndexOutOfBoundsException, is NumberFormatException -> null
            else -> {
                LOG.warn("An unexpected RuntimeException was caught when trying to deserialize a CPD token of ${inputFile.key()}: ${exception.message}")
                null
            }
        }
    }
}
