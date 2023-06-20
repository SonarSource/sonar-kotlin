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

import io.mockk.InternalPlatformDsl.toStr
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.junit.jupiter.api.Test
import org.sonar.api.batch.fs.InputFile

import org.sonarsource.analyzer.commons.checks.verifier.SingleFileVerifier
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinTree
import org.sonarsource.kotlin.api.visiting.Comment
import org.sonarsource.kotlin.api.visiting.CommentAnnotationsAndTokenVisitor
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitor
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes


// class ProviderGetOutsideTaskCheckTest : GradleCheckTest(ProviderGetOutsideTaskCheck())

