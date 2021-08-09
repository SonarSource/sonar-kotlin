package checks

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class RedundantSuspendModifierCheckSample {

    suspend fun redundant() { // Noncompliant {{Remove this unnecessary "suspend" modifier.}}
//  ^^^^^^^
        println("Hello!")
    }

    fun noSuspend() {
        println("Hello!")
    }

    suspend fun suspending() { // Compliant, has suspension point
        println("Hello!")
        delay(500L)
    }

    suspend fun methodReference() { // FN, this method reference is not actually a call, also not reported by compiler
        ::delayInt
    }

    suspend fun lambda() { // FN, this lambda is not actually in the outer method, also not reported by compiler
        (1..10).suspendForeach {
            println("Hello!")
            delayInt(it)
        }
    }
}

class SuspendableImpl: Suspendable {
    override suspend fun suspend() { // Compliant, overrides suspending function
        TODO("Not yet implemented")
    }
}

interface Suspendable {
    suspend fun suspend() // Compliant, no body
}

fun Iterable<Int>.suspendForeach(block: suspend (Int) -> Unit) = this.forEach { runBlocking { launch { block(it) } } }

suspend fun delayInt(i: Int) = delay(100L * i)
