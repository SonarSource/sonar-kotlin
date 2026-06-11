package checks

import java.util.Random
import java.util.concurrent.ThreadLocalRandom
import org.apache.commons.lang.math.JVMRandom
import org.apache.commons.lang3.RandomUtils
import org.apache.commons.lang.RandomStringUtils

// SONARKT-770: no crypto import, no security-keyword identifiers in scope ->
// the security-context heuristic suppresses the issue.
class PseudoRandomCheckSampleNoContext {

    fun neutralMethod() {
        val r = Random() // Compliant
        val v = r.nextInt()

        val j = JVMRandom() // Compliant
        val d1 = j.nextDouble()

        val d2 = Math.random() // Compliant
        val v2 = ThreadLocalRandom.current().nextInt() // Compliant

        val ru = RandomUtils()
        val f1 = RandomUtils.nextFloat() // Compliant

        val rsu = RandomStringUtils()
        val s1 = RandomStringUtils.random(1) // Compliant

        val kr = kotlin.random.Random(0) // Compliant
    }

    fun doWork(input: Int): Int = input + 1
}
