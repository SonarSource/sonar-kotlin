package checks

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

suspend fun unused() {
    
    coroutineScope { // Noncompliant {{This function returns "Deferred", but its result is never used.}}
//  ^^^^^^^^^^^^^^
        async { 
            delay(500L)
            1
        }
    }
    
    MainScope().async { // Noncompliant
//              ^^^^^
            delay(500L)
            1
        }
    
    coroutineScope { 
        async { // Noncompliant
            delay(500L)
            1
        }
        123
    }
    coroutineScope { // Noncompliant
        coroutineScope {
            coroutineScope {
                coroutineScope {
                    async {
                        delay(500L)
                        1
                    }
                }
            }
        }
    }
    val a = coroutineScope {
        async { // Noncompliant
            delay(500L)
            1
        }
        123
    }

    coroutineScope { // Noncompliant
        async {
            delay(500L)
            1
        }.apply {  }
    }

    coroutineScope { // Noncompliant
        async {
            delay(500L)
            1
        }.also {  }
    }
    
    custom() // Noncompliant
}

suspend fun used() {
    var b = coroutineScope {
        async {
            delay(500L)
            1
        }
    }

    coroutineScope {
        b = async {
            delay(500L)
            1
        }
        123
    }
    b = coroutineScope {
        coroutineScope {
            coroutineScope {
                coroutineScope {
                    async {
                        delay(500L)
                        1
                    }
                }
            }
        }
    } 
    coroutineScope {
        deferredConsumer(
            async { 
                delay(500L) 
                1
            }
        )
    }
}

fun CoroutineScope.ext() {
    async { "abc" } // Noncompliant
}


suspend fun asyncReturnUnit() {
    val a = coroutineScope {
        async {
            delay(100L)
            println("Hello!")
        }
    }
    a.await()
}

fun deferredConsumer(d: Deferred<Int>): Nothing = TODO()

fun custom() : Deferred<String> = TODO()
