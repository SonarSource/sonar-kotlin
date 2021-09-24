/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2021 SonarSource SA
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

import org.sonar.api.config.Configuration
import org.sonar.api.utils.log.Loggers
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

private val LOG = Loggers.get(DurationStatistics::class.java)
private const val PROPERTY_KEY = "sonar.kotlin.duration.statistics"

internal class DurationStatistics(config: Configuration) {

    private val stats: MutableMap<String, AtomicLong> = ConcurrentHashMap()
    private val recordStat = config.getBoolean(PROPERTY_KEY).orElse(false)

    fun <C, T> time(id: String, consumer: (C, T) -> Unit): (C, T) -> Unit {
        return if (recordStat) {
            { t: C, u: T -> time(id) { consumer(t, u) } }
        } else {
            consumer
        }
    }

    fun time(id: String, runnable: () -> Unit) {
        if (recordStat) time<Unit>(id) { runnable() }
        else runnable()
    }

    fun <T> time(id: String, supplier: () -> T): T {
        return if (recordStat) {
            val startTime = System.nanoTime()
            val result = supplier()
            record(id, System.nanoTime() - startTime)
            result
        } else {
            supplier()
        }
    }

    fun record(id: String, elapsedTime: Long) {
        stats.computeIfAbsent(id) { AtomicLong(0) }.addAndGet(elapsedTime)
    }

    fun log() {
        if (recordStat) {
            val symbols = DecimalFormatSymbols(Locale.ROOT).apply {
                groupingSeparator = '\''
            }

            val format = DecimalFormat("#,###", symbols)

            val result = "Duration Statistics" +
                stats.entries
                    .sortedBy { (_, value) -> value.get() }
                    .joinToString { (key, value) ->
                        ", $key ${format.format(value.get() / 1000000L)} ms"
                    }

            LOG.info(result)
        }
    }
}
