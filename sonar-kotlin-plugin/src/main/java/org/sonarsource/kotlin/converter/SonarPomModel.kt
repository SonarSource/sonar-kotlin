/*
 * SonarSource SLang
 * Copyright (C) 2018-2021 SonarSource SA
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
package org.sonarsource.kotlin.converter

import org.jetbrains.kotlin.com.intellij.openapi.extensions.ExtensionPoint
import org.jetbrains.kotlin.com.intellij.openapi.extensions.Extensions
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.com.intellij.openapi.util.UserDataHolderBase
import org.jetbrains.kotlin.com.intellij.pom.PomModel
import org.jetbrains.kotlin.com.intellij.pom.PomModelAspect
import org.jetbrains.kotlin.com.intellij.pom.PomTransaction
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.TreeCopyHandler

class SonarPomModel(project: Project) : UserDataHolderBase(), PomModel {
    init {
        val extension = "org.jetbrains.kotlin.com.intellij.treeCopyHandler"
        val extensionClass = TreeCopyHandler::class.java.name
        synchronized(Extensions.getRootArea()) {
            arrayOf(project.extensionArea, Extensions.getRootArea())
                .filter { !it.hasExtensionPoint(extension) }
                .forEach { it.registerExtensionPoint(extension, extensionClass, ExtensionPoint.Kind.INTERFACE) }
        }
    }

    override fun <T : PomModelAspect?> getModelAspect(p0: Class<T>): T? { return null }

    override fun runTransaction(p0: PomTransaction) { /* TODO Add body */ }
}
