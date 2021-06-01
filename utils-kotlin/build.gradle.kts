plugins {
    java
    application
    kotlin("jvm")
}

val detektVersion = "1.17.1"
val ktlintVersion = "0.41.0"

dependencies {
    implementation("io.gitlab.arturbosch.detekt", "detekt-cli", detektVersion)
    implementation("io.gitlab.arturbosch.detekt", "detekt-core", detektVersion)
    implementation("io.gitlab.arturbosch.detekt", "detekt-api", detektVersion)
    implementation("com.pinterest", "ktlint", ktlintVersion)
    implementation("com.pinterest.ktlint", "ktlint-core", ktlintVersion)
    implementation("com.pinterest.ktlint", "ktlint-ruleset-standard", ktlintVersion)
    implementation("com.pinterest.ktlint", "ktlint-ruleset-experimental", ktlintVersion)

    implementation(kotlin("stdlib-jdk8"))

    implementation("com.google.code.gson:gson:2.8.7")
    implementation("com.beust:jcommander:1.81")
    implementation("org.apache.commons:commons-text:1.9")

    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.assertj:assertj-core")

    // JUnit4 is only included because forbiddenApisTest forbids using it and hence (?!) depends on it. So the check will fail, if
    // we don't depend on it. We probably want to clean this up eventually.
    testImplementation("junit:junit")
}

tasks {
    task<JavaExec>("updateDetektRules") {
        group = "Application"
        classpath = sourceSets.main.get().runtimeClasspath
        main = "org.sonarsource.kotlin.externalreport.detekt.DetektRuleDefinitionGeneratorKt"

        doFirst {
            println("Updating rules for Detekt version $detektVersion...")
        }
    }

    task<JavaExec>("updateKtlintRules") {
        group = "Application"
        classpath = sourceSets.main.get().runtimeClasspath
        main = "org.sonarsource.kotlin.externalreport.ktlint.KtlintRuleDefinitionGeneratorKt"

        doFirst {
            println("Updating rules for ktlint version $detektVersion...")
        }
    }
}
