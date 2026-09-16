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
package checks

// When the configured threshold (50) exceeds COMPOSABLE_THRESHOLD (45), maxOf picks the configured value.
// A @Composable function with complexity 46 is compliant because maxOf(50, 45) = 50 and 46 <= 50.
class FunctionCognitiveComplexityCheckHighThresholdSample {

    val x: Boolean = false

    @Composable
    fun composableWithComplexityJustAboveComposableThreshold() { // Compliant - complexity 46 does not exceed maxOf(threshold=50, COMPOSABLE_THRESHOLD=45) = 50
        if (x) { // +1
            println()
            if (x) { // +2
                println()
                if (x) { // +3
                    println()
                    if (x) { // +4
                        println()
                        if (x) { // +5
                            println()
                            if (x) { // +6
                                println()
                                if (x) { // +7
                                    println()
                                    if (x) { // +8
                                        println()
                                        if (x) { // +9
                                            println()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (x) println() // +1
    }

}
