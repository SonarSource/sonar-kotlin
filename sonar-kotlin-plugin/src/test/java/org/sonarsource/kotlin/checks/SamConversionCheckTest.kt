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

import io.mockk.every
import io.mockk.mockk
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtSuperTypeListEntry
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.FlexibleType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.sonarsource.kotlin.api.getType
import org.sonarsource.kotlin.plugin.KotlinFileContext

internal class SamConversionCheckTest : CheckTest(SamConversionCheck()) {

    @Test
    fun testNullReferenceType() {
        val mockTypeEntry = mockk<KtSuperTypeListEntry>()
        every { mockTypeEntry.typeReference } returns null

        val mockTypeEntries = listOf(mockTypeEntry)
        val mockDeclaration = mockk<KtObjectDeclaration>()
        every { mockDeclaration.superTypeListEntries } returns mockTypeEntries

        val mockContext = mockk<KotlinFileContext>()

        assertDoesNotThrow {
            val check = SamConversionCheck()
            check.visitObjectDeclaration(mockDeclaration, mockContext)
        }
    }

    @Test
    fun testFlexibleType() {

        val mockBindingContext = mockk<BindingContext>()

        val mockContext = mockk<KotlinFileContext>()
        every { mockContext.bindingContext } returns mockBindingContext

        val mockFlexibleType = mockk<FlexibleType>()

        val mockTypeReference = mockk<KtTypeReference>()
        every { mockTypeReference.getType(mockBindingContext) } returns mockFlexibleType

        val mockTypeEntry = mockk<KtSuperTypeListEntry>()
        every { mockTypeEntry.typeReference } returns mockTypeReference

        val mockTypeEntries = listOf(mockTypeEntry)
        val mockDeclaration = mockk<KtObjectDeclaration>()
        every { mockDeclaration.superTypeListEntries } returns mockTypeEntries
        
        assertDoesNotThrow {
            val check = SamConversionCheck()
            check.visitObjectDeclaration(mockDeclaration, mockContext)
        }
    }
}
