package checks

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

suspend fun redundant() {
    println("Hello!")
}

suspend fun suspending() {
    println("Hello!")
    delay(500L)
}
