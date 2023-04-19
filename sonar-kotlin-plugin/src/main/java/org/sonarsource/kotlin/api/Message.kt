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
package org.sonarsource.kotlin.api

class Message(text: String = "", ranges: List<Pair<Int, Int>> = mutableListOf()) {
    var text: String = text
        private set
    private val _ranges: MutableList<Pair<Int, Int>> = ranges.toMutableList()

    val ranges
        get() = _ranges.toList()

    operator fun String.unaryPlus() {
        text += this
    }

    fun code(code: String) {
        _ranges.add(text.length to text.length + code.length)
        text += code
    }
}

fun message(block: Message.() -> Unit) = Message().apply(block)
