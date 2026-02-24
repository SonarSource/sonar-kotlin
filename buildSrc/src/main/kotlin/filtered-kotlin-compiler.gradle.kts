import org.gradle.api.attributes.Attribute
import org.sonarsource.kotlin.buildsrc.KotlinCompilerTransform

// TODO: custom attribute doesn't seem to match anything
val filtered: Attribute<Boolean> = Attribute.of("kotlin-compiler-filtered", Boolean::class.javaObjectType)

configurations.configureEach {
    if (name.endsWith("RuntimeClasspath", ignoreCase = true)) {
        attributes {
            attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "kotlin-compiler-filtered-jar")
            attribute(filtered, true)
        }
    }
}

dependencies.registerTransform(KotlinCompilerTransform::class) {
    from
        .attribute(filtered, false)
        .attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "jar")
    to
        .attribute(filtered, true)
        .attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "kotlin-compiler-filtered-jar")
}
