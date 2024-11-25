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
package org.sonarsource.kotlin.api.visiting

import org.sonarsource.kotlin.api.checks.InputFileContext
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.frontend.KotlinTree

abstract class KotlinFileVisitor {
    fun scan(fileContext: InputFileContext, root: KotlinTree) {
        visit(KotlinFileContext(fileContext, root.psiFile, root.bindingContext, root.diagnostics, root.regexCache))
    }

    abstract fun visit(kotlinFileContext: KotlinFileContext)
}
