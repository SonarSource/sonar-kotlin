package org.sonarsource.kotlin.api

import org.sonar.api.batch.fs.TextPointer

class ParseException(
    message: String?,
    val position: TextPointer? = null,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
