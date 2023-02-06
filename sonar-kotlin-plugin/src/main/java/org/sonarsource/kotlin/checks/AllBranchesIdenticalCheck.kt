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

import org.jetbrains.kotlin.psi.KtElement
import org.sonar.check.Rule
import org.sonarsource.kotlin.plugin.KotlinFileContext

@Rule(key = "S3923")
class AllBranchesIdenticalCheck : AbstractBranchDuplication() {
    override fun checkDuplicatedBranches(ctx: KotlinFileContext, tree: KtElement, branches: List<KtElement>) {
        // handled by S1871
    }

    override fun onAllIdenticalBranches(ctx: KotlinFileContext, tree: KtElement) {
        ctx.reportIssue(tree,
            "Remove this conditional structure or edit its code blocks so that they're not all the same.")
    }
}
