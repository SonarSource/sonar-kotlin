package checks

import io.github.oshai.kotlinlogging.KLogger
import java.util.logging.Level
import java.util.logging.Logger as JulLogger
import mu.KLogger as LegacyKLogger
import org.apache.logging.log4j.Logger as Log4jLogger
import org.slf4j.Logger as Slf4jLogger

class StringLiteralDuplicatedCheckLoggingSample {

    fun recognizedLoggingCalls(
        slf4j: Slf4jLogger,
        jul: JulLogger,
        log4j: Log4jLogger,
        kotlinLogger: KLogger,
        legacyKotlinLogger: LegacyKLogger,
        suffix: String,
    ) {
        slf4j.trace("repeated SLF4J message!")
        slf4j.debug("repeated SLF4J message!")
        slf4j.info("repeated SLF4J message!")

        jul.info("repeated JUL message!")
        jul.warning("repeated JUL message!")
        jul.log(Level.SEVERE, "repeated JUL message!")

        log4j.info("repeated Log4j message!")
        log4j.warn("repeated Log4j message!")
        log4j.fatal("repeated Log4j message!")

        kotlinLogger.info { "repeated Kotlin logging message!" }
        kotlinLogger.warn { "repeated Kotlin logging message!" }
        kotlinLogger.error { "repeated Kotlin logging message!" }

        legacyKotlinLogger.info { "repeated legacy Kotlin logging message!" }
        legacyKotlinLogger.warn { "repeated legacy Kotlin logging message!" }
        legacyKotlinLogger.error { "repeated legacy Kotlin logging message!" }

        slf4j.info("concatenated logging message: " + suffix)
        slf4j.warn("concatenated logging message: " + suffix)
        slf4j.error("concatenated logging message: " + suffix)
    }

    fun loggingSubtypeIsRecognized(logger: DerivedSlf4jLogger) {
        logger.info("logging subtype message!")
        logger.warn("logging subtype message!")
        logger.error("logging subtype message!")
    }

    fun loggingIsMatchedSemantically(logger: LoggerWithAnInfoMethod) {
        // Noncompliant@+1
        logger.info("not a recognized logging API!")
        logger.info("not a recognized logging API!")
        logger.info("not a recognized logging API!")
    }

    fun mixedLoggingAndOrdinaryOccurrences(logger: Slf4jLogger) {
        // Noncompliant@+1 {{Define a constant instead of duplicating this literal "mixed logging and ordinary!" 5 times.}}
        logger.info("mixed logging and ordinary!")
        println("mixed logging and ordinary!")
        println("mixed logging and ordinary!")
        println("mixed logging and ordinary!")
        logger.info("mixed logging and ordinary!")

        logger.info("logging does not reach threshold!")
        println("logging does not reach threshold!")
        println("logging does not reach threshold!")
        logger.info("logging does not reach threshold!")
        logger.info("logging does not reach threshold!") // Compliant - only two ordinary occurrences
    }
}

class LoggerWithAnInfoMethod {
    fun info(message: String) = Unit
}

interface DerivedSlf4jLogger : Slf4jLogger
