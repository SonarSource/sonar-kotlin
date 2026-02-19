import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.sonarsource.kotlin.buildsrc.ArtifactTypeCompatibilityRule
import org.sonarsource.kotlin.buildsrc.ArtifactTypeDisambiguationRule
import org.sonarsource.kotlin.buildsrc.KOTLIN_COMPILER_FILTERED_JAR
import org.sonarsource.kotlin.buildsrc.KotlinCompilerTransform

// Register rules for the artifact-type attribute so that:
//   - Consumers requesting kotlin-compiler-filtered-jar only see relevant variants.
//   - Consumers with no preference prefer the standard jar variant.
//
// Without these rules, plugin-created variants (Spotless, Jacoco, etc.) that have no
// artifact-type value would be considered universally compatible and cause ambiguity
// errors against filteredRuntimeElements.
dependencies.attributesSchema {
    attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE) {
        // Compatibility: reject null-artifact-type variants when consumer wants the filtered jar.
        compatibilityRules.add(ArtifactTypeCompatibilityRule::class.java)
        // Disambiguation: when consumer has no artifact-type preference, prefer jar over filtered.
        disambiguationRules.add(ArtifactTypeDisambiguationRule::class.java)
    }
}

plugins.withType<JavaPlugin> {
    // Tag the standard runtimeElements with artifact-type=jar so the disambiguation rule
    // above can identify it as the preferred variant for consumers that don't request filtering.
    configurations.named("runtimeElements") {
        attributes {
            attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.JAR_TYPE)
        }
    }

    // Expose a secondary "filtered" variant for this project's JAR.
    // Project JARs don't embed kotlin-compiler, so they don't need transformation.
    // Advertising this variant with artifact-type=kotlin-compiler-filtered-jar prevents Gradle
    // from applying KotlinCompilerTransform to project JARs, which would require them to already
    // exist at transform scheduling time (causing failures on clean builds).
    val filteredRuntimeElements by configurations.creating {
        isCanBeConsumed = true
        isCanBeResolved = false
        extendsFrom(configurations.getByName("implementation"), configurations.getByName("runtimeOnly"))
        attributes {
            attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, KOTLIN_COMPILER_FILTERED_JAR)
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
            attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
        }
        outgoing.artifact(tasks.named("jar"))
    }
}

configurations.configureEach {
    if (name.endsWith("RuntimeClasspath", ignoreCase = true)) {
        attributes {
            attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, KOTLIN_COMPILER_FILTERED_JAR)
        }
    }
}

dependencies.registerTransform(KotlinCompilerTransform::class) {
    from.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.JAR_TYPE)
    to.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, KOTLIN_COMPILER_FILTERED_JAR)
}
