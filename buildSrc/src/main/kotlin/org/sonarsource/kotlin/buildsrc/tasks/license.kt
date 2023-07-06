package org.sonarsource.kotlin.buildsrc.tasks

import java.nio.file.Path
import java.util.Calendar
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.readText

@OptIn(ExperimentalPathApi::class)
internal val LICENSE_HEADER by lazy {
    Path.of("LICENSE_HEADER").readText()
        .replace("${"$"}YEAR", Calendar.getInstance().get(Calendar.YEAR).toString())
}
