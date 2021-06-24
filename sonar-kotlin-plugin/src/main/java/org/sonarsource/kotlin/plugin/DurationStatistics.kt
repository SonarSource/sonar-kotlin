package org.sonarsource.kotlin.plugin

import org.sonar.api.config.Configuration
import org.sonar.api.utils.log.Loggers
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

private val LOG = Loggers.get(DurationStatistics::class.java)
private const val PROPERTY_KEY = "sonar.slang.duration.statistics"

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
