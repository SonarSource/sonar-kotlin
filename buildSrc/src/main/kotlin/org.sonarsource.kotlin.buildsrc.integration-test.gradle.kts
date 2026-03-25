import org.gradle.api.attributes.Attribute
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

val sonarKotlinPluginDist by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
    attributes {
        attribute(Attribute.of("org.sonarsource.kotlin.dist", String::class.java), "sonar-kotlin-plugin")
    }
}

dependencies {
    sonarKotlinPluginDist(project(":sonar-kotlin-plugin"))
}

tasks.withType<Test> {
    useJUnitPlatform()
    inputs.files(sonarKotlinPluginDist)
    outputs.upToDateWhen { false }
}
