package checks

import java.nio.charset.Charset

fun foo() {
    byteArrayOf().toString(Charset.defaultCharset())
}
