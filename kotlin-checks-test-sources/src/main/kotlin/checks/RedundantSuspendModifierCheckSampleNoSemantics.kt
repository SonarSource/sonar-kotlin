package checks

import kotlinx.coroutines.delay

suspend fun redundant() {
    println("Hello!")
}

suspend fun suspending() {
    println("Hello!")
    delay(500L)
}
