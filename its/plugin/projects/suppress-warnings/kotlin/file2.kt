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
