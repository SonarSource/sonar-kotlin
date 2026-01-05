/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2026 SonarSource SA
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
package org.sonarsource.kotlin.api.reporting

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
