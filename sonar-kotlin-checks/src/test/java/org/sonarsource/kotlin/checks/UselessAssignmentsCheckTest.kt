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
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.testapi.KotlinVerifier

internal class UselessAssignmentsCheckTest : CheckTest(UselessAssignmentsCheck()) {

    // The sample covers declarations containing compilation errors. An unreliable
    // ASSIGNED_VALUE_IS_NEVER_READ diagnostic is suppressed when the assignment's nearest
    // enclosing declaration with a body is one of those erroneous declarations. Assignments inside
    // lambdas belong to their enclosing declaration. Other S6615 diagnostics remain enabled.
    @Test
    fun `with partial semantics`() {
        verifier(check, "${checkName}SamplePartialSemantics.kt").verify()
    }

    private fun verifier(check: AbstractCheck, fileName: String) = KotlinVerifier(check) {
        this.fileName = fileName
        baseDir = NON_COMPILING_BASE_DIR
    }
}
