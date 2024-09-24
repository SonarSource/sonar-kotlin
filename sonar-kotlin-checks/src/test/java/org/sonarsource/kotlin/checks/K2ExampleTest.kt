package org.sonarsource.kotlin.checks

import org.junit.jupiter.api.Test
import org.sonarsource.kotlin.testapi.KotlinVerifier

class K2ExampleTest {

    @Test
    fun test() {
        val check = IndexedAccessCheck()
        KotlinVerifier(check) {
            fileName = "K2Example.kt"
        }.verify()
    }

}
