package org.sonarsource.kotlin.buildsrc

import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.AttributeCompatibilityRule
import org.gradle.api.attributes.AttributeDisambiguationRule
import org.gradle.api.attributes.CompatibilityCheckDetails
import org.gradle.api.attributes.MultipleCandidatesDetails

const val KOTLIN_COMPILER_FILTERED_JAR = "kotlin-compiler-filtered-jar"

/**
 * Compatibility rule for the artifact-type attribute.
 *
 * When a consumer requests kotlin-compiler-filtered-jar, only accept candidates that
 * explicitly have this value. Candidates with no artifact-type value (such as Spotless
 * or Jacoco plugin variants) would otherwise be considered compatible (no attribute =
 * no constraint = universally compatible), polluting the candidate set and causing
 * ambiguity errors with filteredRuntimeElements.
 */
abstract class ArtifactTypeCompatibilityRule : AttributeCompatibilityRule<String> {
    override fun execute(details: CompatibilityCheckDetails<String>) {
        if (details.consumerValue == KOTLIN_COMPILER_FILTERED_JAR && details.producerValue == null) {
            details.incompatible()
        }
    }
}

/**
 * Disambiguation rule for the artifact-type attribute.
 *
 * When a consumer has no artifact-type preference, prefer the standard JAR type over
 * kotlin-compiler-filtered-jar. Without this, both runtimeElements (artifact-type=jar)
 * and filteredRuntimeElements (artifact-type=kotlin-compiler-filtered-jar) are compatible
 * for consumers requesting only Usage=java-runtime, causing ambiguity errors.
 */
abstract class ArtifactTypeDisambiguationRule : AttributeDisambiguationRule<String> {
    override fun execute(details: MultipleCandidatesDetails<String>) {
        val preferred = details.consumerValue ?: ArtifactTypeDefinition.JAR_TYPE
        if (details.candidateValues.contains(preferred)) {
            details.closestMatch(preferred)
        }
    }
}
