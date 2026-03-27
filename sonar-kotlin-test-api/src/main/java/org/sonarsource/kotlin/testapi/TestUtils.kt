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
package org.sonarsource.kotlin.testapi

import org.sonar.api.batch.fs.InputFile
// TODO: testapi should not depend on frontend module.
import org.sonarsource.kotlin.api.frontend.Environment
import org.sonarsource.kotlin.api.frontend.KotlinFileSystem
import org.sonarsource.kotlin.api.frontend.KotlinSyntaxStructure
import org.sonarsource.kotlin.api.frontend.KotlinTree
import org.sonarsource.kotlin.api.frontend.KotlinVirtualFile
import org.sonarsource.kotlin.api.frontend.createK2AnalysisSession
import java.io.File

fun kotlinTreeOf(content: String, environment: Environment, inputFile: InputFile): KotlinTree {
    val virtualFile = KotlinVirtualFile(
        KotlinFileSystem(),
        File(inputFile.uri().rawPath),
        contentProvider = { content },
    )
    environment.k2session = createK2AnalysisSession(
        environment.disposable,
        environment.configuration,
        listOf(virtualFile),
    )
    val (ktFile, document) = KotlinSyntaxStructure.of(environment, inputFile, virtualFile)
    return KotlinTree(ktFile, document)
}
