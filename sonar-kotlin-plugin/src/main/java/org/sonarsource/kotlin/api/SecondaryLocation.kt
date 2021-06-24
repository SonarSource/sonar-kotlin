package org.sonarsource.kotlin.api

import org.sonar.api.batch.fs.TextRange

data class SecondaryLocation(val textRange: TextRange, val message: String?)
