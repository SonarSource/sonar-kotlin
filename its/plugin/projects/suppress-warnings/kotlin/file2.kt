@java.lang.SuppressWarnings("kotlin:S1145")
class Clazz {
    fun complexity() {
        (1..2).forEach {
            if (true) {
                if (true) {
                    if (true) {
                        if (true) {
                            if (true) {
                                if (true) {
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Suppress("kotlin:S3776")
fun complexity() {
    (1..2).forEach {
        if (true) {
            if (true) {
                if (true) {
                    if (true) {
                        if (true) {
                            if (true) {
                            }
                        }
                    }
                }
            }
        }
    }
}

fun suppressedAtExpressionLevel(values: List<Int>): List<String> {
    return values.map { value ->
        @Suppress("kotlin:S1479")
        when (value) { // Noncompliant
            0 -> "0"
            1 -> "0"
            2 -> "0"
            3 -> "0"
            4 -> "0"
            5 -> "0"
            6 -> "0"
            7 -> "0"
            8 -> "0"
            9 -> "0"
            10 -> "0"
            11 -> "0"
            12 -> "0"
            13 -> "0"
            14 -> "0"
            15 -> "0"
            16 -> "0"
            17 -> "0"
            18 -> "0"
            19 -> "0"
            20 -> "0"
            21 -> "0"
            22 -> "0"
            23 -> "0"
            24 -> "0"
            25 -> "0"
            26 -> "0"
            27 -> "0"
            28 -> "0"
            29 -> "0"
            30 -> "0"
            else -> "Unknown"
        }
    }
}

@Suppress("kotlin:S1479")
fun suppressedAtFunctionLevel(values: List<Int>): List<String> {
    return values.map { value ->
        @Suppress("kotlin:S1479")
        when (value) { // Noncompliant
            0 -> "0"
            1 -> "0"
            2 -> "0"
            3 -> "0"
            4 -> "0"
            5 -> "0"
            6 -> "0"
            7 -> "0"
            8 -> "0"
            9 -> "0"
            10 -> "0"
            11 -> "0"
            12 -> "0"
            13 -> "0"
            14 -> "0"
            15 -> "0"
            16 -> "0"
            17 -> "0"
            18 -> "0"
            19 -> "0"
            20 -> "0"
            21 -> "0"
            22 -> "0"
            23 -> "0"
            24 -> "0"
            25 -> "0"
            26 -> "0"
            27 -> "0"
            28 -> "0"
            29 -> "0"
            30 -> "0"
            else -> "Unknown"
        }
    }
}
