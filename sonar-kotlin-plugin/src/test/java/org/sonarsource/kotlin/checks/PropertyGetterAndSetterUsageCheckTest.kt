/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.kotlin.checks

import my.org.sonarsource.kotlin.checks.PropertyGetterAndSetterUsageCheck
import org.junit.jupiter.api.Test
import org.sonarsource.kotlin.verifier.KotlinVerifier

internal class PropertyGetterAndSetterUsageCheckTest : CheckTest(PropertyGetterAndSetterUsageCheck()) {

    @Test
    fun `also check public classes`() {
        val check = PropertyGetterAndSetterUsageCheck()
        check.checkPublicClasses = true
        KotlinVerifier(check) {
            fileName = "PropertyGetterAndSetterUsageCheckSamplePublic.kt"
        }.verify()
    }

}
