package org.sonarsource.kotlin.checks

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.sonarsource.kotlin.verifier.KotlinVerifier

class ManualCheckTest {
    /**
     * Put the check class you want to test here. Then run the test below to test your check.
     */
    val check = ServerCertificateCheck::class.java

    @Disabled
    @Test
    fun `test a check manually`() {
        KotlinVerifier(check.getDeclaredConstructor().newInstance()) {
            fileName = "${check.simpleName}Sample.kt"
        }.verify()
    }
}
