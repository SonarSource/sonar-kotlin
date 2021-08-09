package checks

import io.MyFile
import java.io.File
import java.io.IOException
import kotlin.jvm.Throws
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class MainSafeCoroutinesCheckSample {

    suspend fun throwsIO3() {
        GlobalScope.launch {
            f() // Noncompliant {{Use "Dispatchers.IO" to run this potentially blocking operation.}}
        }
        delay(500L)
    }

    suspend fun throwsIO4() {
        withContext(Dispatchers.Default) {
            f() // Noncompliant {{Use "Dispatchers.IO" to run this potentially blocking operation.}}
        }
        delay(500L)
    }
    suspend fun throwsIO() {
        f() // Noncompliant {{Use "Dispatchers.IO" to run this potentially blocking operation.}}
        delay(500L)
    }

    suspend fun throwsIO5() {
        withContext(Dispatchers.Main) {
            f() // Noncompliant {{Use "Dispatchers.IO" to run this potentially blocking operation.}}
        }
        delay(500L)
    }

    suspend fun throwsIO6(context: CoroutineDispatcher) {
        withContext(context) {
            f() // Compliant
        }
        delay(500L)
    }

    suspend fun throwsIO7(obj: Any?) {
        withContext(Dispatchers.Main) {
            obj?.let {
                f() // Noncompliant
            }
        }
        delay(500L)
    }

    suspend fun throwsIO8(obj1: Any?, obj2: Any?, obj3: Any?) {
        obj1?.let {
            obj2?.let {
                obj3?.let {
                    f() // Noncompliant
                }
            }
        }
        delay(500L)
    }

    // False-negative, can't get throwing section from binaries
    suspend fun javaThrowsIO2() {
        MyFile().createFile()
        delay(500L)
    }

    suspend fun suspendThreadSleep() {
        Thread.sleep(500L) // Noncompliant {{Replace this "Thread.sleep()" call with "delay()".}}
    }

    fun threadSleep() {
        Thread.sleep(500L) // Compliant, not in a suspending function
    }

    fun inSuspendingLambda() {
        runBlocking {
            Thread.sleep(500L) // Noncompliant {{Replace this "Thread.sleep()" call with "delay()".}}
        }
    }

    fun withGlobalScope() {
        GlobalScope.launch {
            Thread.sleep(500L) // Noncompliant {{Replace this "Thread.sleep()" call with "delay()".}}
        }
    }

    fun inSuspendingLambdaDelay() {
        runBlocking {
            delay(500L)
        }
    }
    // False-negative, can't get throwing section from binaries
    suspend fun javaThrowsIO() {
        File("").getCanonicalPath()
        delay(500L)
    }

    suspend fun compliant() {
        withContext(Dispatchers.IO) {
            f()
        }
        delay(500L)
    }
}

@Throws(IOException::class)
fun f() {
}

