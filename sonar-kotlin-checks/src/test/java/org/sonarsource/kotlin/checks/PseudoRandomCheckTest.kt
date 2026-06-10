/*
 * SonarSource Kotlin
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonarsource.kotlin.checks

import org.junit.jupiter.api.Test
import org.sonarsource.kotlin.testapi.KotlinVerifier

class PseudoRandomCheckTest : CheckTest(PseudoRandomCheck()) {

    @Test
    fun `no security context`() {
        KotlinVerifier(check) {
            this.fileName = "${checkName}SampleNoContext.kt"
        }.verifyNoIssue()
    }

    @Test
    fun `security keywords in scope`() {
        KotlinVerifier(check) {
            this.fileName = "${checkName}SampleSecurityKeywords.kt"
        }.verify()
    }

    @Test
    fun `crypto import gate`() {
        KotlinVerifier(check) {
            this.fileName = "${checkName}SampleCryptoImport.kt"
        }.verify()
    }

    @Test
    fun `top level scope fallback`() {
        KotlinVerifier(check) {
            this.fileName = "${checkName}SampleTopLevel.kt"
        }.verify()
    }
}
