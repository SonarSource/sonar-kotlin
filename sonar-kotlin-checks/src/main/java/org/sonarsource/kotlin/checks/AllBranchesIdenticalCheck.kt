/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
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

import org.jetbrains.kotlin.psi.KtElement
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

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
