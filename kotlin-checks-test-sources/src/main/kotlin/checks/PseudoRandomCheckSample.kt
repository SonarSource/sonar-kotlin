package checks

import org.apache.commons.lang.RandomStringUtils
import org.apache.commons.lang.math.JVMRandom
import org.apache.commons.lang3.RandomUtils
import java.util.Random
import java.util.concurrent.ThreadLocalRandom

class PseudoRandomCheckSample {

    fun f() {
        val i = nextInt()

        // Non static class, report only constructor
        // java.util.Random

        // Non static class, report only constructor
        // java.util.Random
        val random = Random() // Noncompliant {{Make sure that using this pseudorandom number generator is safe here.}}
//                   ^^^^^^        

        val kotlinRamdom = kotlin.random.Random(0) // Noncompliant {{Make sure that using this pseudorandom number generator is safe here.}}
//                                       ^^^^^^    
        
        val kotlinRamdomL = kotlin.random.Random(0L) // Noncompliant {{Make sure that using this pseudorandom number generator is safe here.}}
        
        val bytes = ByteArray(20)
        random.nextBytes(bytes) // Compliant


        // org.apache.commons.lang.math.JVMRandom

        // org.apache.commons.lang.math.JVMRandom
        val jvmRandom = JVMRandom() // Noncompliant
//                      ^^^^^^^^^

        val rand3 = jvmRandom.nextDouble()

        // Static class, don't report constructor, only usage
        // java.lang.Math. Report only Math.random()

        // Static class, don't report constructor, only usage
        // java.lang.Math. Report only Math.random()
        val rand1 = Math.random() // Noncompliant

        val abs = Math.abs(12).toDouble() // Compliant


        // java.util.concurrent.ThreadLocalRandom

        // java.util.concurrent.ThreadLocalRandom
        val rand2 = ThreadLocalRandom.current().nextInt() // Noncompliant


        // org.apache.commons.lang.math.RandomUtils

        // org.apache.commons.lang.math.RandomUtils
        val randomUtils = org.apache.commons.lang.math.RandomUtils()
        val rand4 = org.apache.commons.lang.math.RandomUtils.nextFloat() // Noncompliant

        val rand5 = org.apache.commons.lang.math.RandomUtils.nextFloat() // Noncompliant


        // org.apache.commons.lang3.RandomUtils

        // org.apache.commons.lang3.RandomUtils
        val randomUtils2 = RandomUtils()
        val rand6 = RandomUtils.nextFloat() // Noncompliant

        val rand7 = RandomUtils.nextFloat() // Noncompliant


        // org.apache.commons.lang.RandomStringUtils

        // org.apache.commons.lang.RandomStringUtils
        val randomStringUtils = RandomStringUtils()
        val rand8 = RandomStringUtils.random(1) // Noncompliant

        val rand9 = RandomStringUtils.random(1) // Noncompliant


        // org.apache.commons.lang3.RandomStringUtils

        // org.apache.commons.lang3.RandomStringUtils
        val randomStringUtils2 = org.apache.commons.lang3.RandomStringUtils()
        val rand10 = RandomStringUtils.random(1) // Noncompliant

        val rand11 = org.apache.commons.lang3.RandomStringUtils.random(1) // Noncompliant
    }

    fun nextInt(): Int {
        return 42
    }
}
