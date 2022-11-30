package checks

import java.io.File
import java.util.Date
import java.util.concurrent.TimeUnit

fun File.deleteRecursively(): Boolean =
    listFiles()?.all(File::deleteRecursively) ?: true &&
        delete() // Compliant

fun File.deleteRecursively2() {
    listFiles()?.forEach(File::deleteRecursively2)
    delete() // Noncompliant {{Do something with the "Boolean" value returned by "delete".}}
//  ^^^^^^
}

class IgnoredOperationStatusCheckSample {

    private fun err() { /* handle errors */
    }

    fun testAllKindOfExpression(file: File) {
        file.delete() // Noncompliant
//           ^^^^^^
        if (!file.delete()) err() // Compliant
        if (file.delete() == false) err() // Compliant

        while (file.delete()) { // Compliant
            println()
        }

        do {
            println()
        } while (file.delete()) // Compliant

        val result1 = file.delete() // Compliant
        if (!result1) err()

        var result2 = true;
        result2 = result2 && file.delete() // Compliant
        if (!result2) err()

        listOf(file).filter { !it.delete() } // Compliant, issue related to: S3958 Intermediate Stream methods should not be left unused
        listOf(file).all { it.delete() } // Compliant, issue related to a rule to be created: Stream reduced result should not be left unused
        if (!listOf(file).all { it.delete() }) { // Compliant
            err()
        }
        listOf(file).all(File::delete) // Compliant, see ".all { it.delete() }" above
        if (listOf(file).all(File::delete)) { // Compliant
            err()
        }
        listOf(file).forEach(File::delete) // false-negative, limitation of the rule implementation, it does not search for method references
        listOf(file).forEach { f -> f.delete() } // Noncompliant
        listOf(file).map { f -> f.delete() }.filter { !it }.forEach { _ -> err() } // Compliant

        for (result3 in listOf(file).map { !it.delete() }) {
            if (!result3) err()
        }

        when (file.delete()) {
            true -> println("ok")
            false -> err()
        }

        val result4 = file.delete() // Compliant, issue related unused variable is not part or this rule
    }

    fun testFile(
        file: java.io.File,
    ): Boolean {
        file.delete() // Noncompliant {{Do something with the "Boolean" value returned by "delete".}}
        file.mkdir() // Noncompliant {{Do something with the "Boolean" value returned by "mkdir".}}
        file.mkdirs() // Compliant
        file.renameTo(file) // Noncompliant {{Do something with the "Boolean" value returned by "renameTo".}}
        file.setReadOnly() // Noncompliant {{Do something with the "Boolean" value returned by "setReadOnly".}}
        file.setLastModified(0) // Noncompliant {{Do something with the "Boolean" value returned by "setLastModified".}}
        file.setWritable(true) // Noncompliant {{Do something with the "Boolean" value returned by "setWritable".}}
        file.setWritable(true, true) // Noncompliant
        file.setReadable(true) // Noncompliant {{Do something with the "Boolean" value returned by "setReadable".}}
        file.setReadable(true, true) // Noncompliant
        file.setExecutable(true) // Noncompliant {{Do something with the "Boolean" value returned by "setExecutable".}}
        file.setExecutable(true, true) // Noncompliant
        return file.setWritable(true) // Compliant
    }

    fun testIterator(
        iter1: java.util.Iterator<String>,
        iter2: kotlin.collections.Iterator<String>,
        iter3: kotlin.collections.MutableIterator<String>,
    ): Boolean {
        iter1.hasNext() // Noncompliant {{Do something with the "Boolean" value returned by "hasNext".}}
        iter2.hasNext() // Noncompliant
        iter3.hasNext() // Noncompliant
        return iter1.hasNext()// Compliant
            && iter2.hasNext()// Compliant
            && iter3.hasNext() // Compliant
    }

    fun testEnumeration(enum1: java.util.Enumeration<String>): Boolean {
        enum1.hasMoreElements() // Noncompliant {{Do something with the "Boolean" value returned by "hasMoreElements".}}
        return enum1.hasMoreElements() // Compliant
    }

    fun testLock(lock: java.util.concurrent.locks.Lock): Boolean {
        lock.tryLock() // Noncompliant {{Do something with the "Boolean" value returned by "tryLock".}}
        lock.tryLock(10, TimeUnit.MINUTES) // Noncompliant {{Do something with the "Boolean" value returned by "tryLock".}}
        return lock.tryLock() // Compliant
    }

    fun testCondition(condition: java.util.concurrent.locks.Condition) {
        condition.await() // Compliant, return void
        condition.awaitUninterruptibly() // Compliant, return void
        condition.await(12, TimeUnit.SECONDS) // Noncompliant {{Do something with the "Boolean" value returned by "await".}}
        condition.awaitNanos(Long.MAX_VALUE) // Noncompliant {{Do something with the "Long" value returned by "awaitNanos".}}
        condition.awaitUntil(Date()) // Noncompliant {{Do something with the "Boolean" value returned by "awaitUntil".}}
    }

    fun testCountDownLatch(countDownLatch: java.util.concurrent.CountDownLatch) {
        countDownLatch.await() // Compliant, return void
        countDownLatch.await(12, TimeUnit.SECONDS) // Noncompliant {{Do something with the "Boolean" value returned by "await".}}
    }

    fun testSemaphore(semaphore: java.util.concurrent.Semaphore) {
        semaphore.acquire() // Compliant, return void
        semaphore.tryAcquire() // Noncompliant {{Do something with the "Boolean" value returned by "tryAcquire".}}
        semaphore.tryAcquire(2) // Noncompliant {{Do something with the "Boolean" value returned by "tryAcquire".}}
        semaphore.tryAcquire(2, 12, TimeUnit.SECONDS) // Noncompliant {{Do something with the "Boolean" value returned by "tryAcquire".}}
        semaphore.tryAcquire(12, TimeUnit.SECONDS) // Noncompliant {{Do something with the "Boolean" value returned by "tryAcquire".}}
    }

    fun testBlockingQueue(queue: java.util.concurrent.BlockingQueue<String>) {
        queue.offer("Hello") // Noncompliant {{Do something with the "Boolean" value returned by "offer".}}
        queue.offer("World", 2, TimeUnit.SECONDS) // Noncompliant {{Do something with the "Boolean" value returned by "offer".}}
        queue.remove("Hello") // Noncompliant {{Do something with the "Boolean" value returned by "remove".}}
        queue.remove() // Noncompliant {{Do something with the "String" value returned by "remove".}}
    }

}
