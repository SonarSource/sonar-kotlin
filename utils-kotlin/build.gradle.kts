plugins {
    java
    application
    kotlin("jvm")
}

val detektVersion = "1.19.0"
val ktlintVersion = "0.43.0"

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
    implementation(project(":sonar-kotlin-plugin"))

    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.sonarsource.sonarqube:sonar-plugin-api")
}

tasks {
    task<JavaExec>("updateDetektRules") {
        group = "Application"
        classpath = sourceSets.main.get().runtimeClasspath
        mainClass.set("org.sonarsource.kotlin.externalreport.detekt.DetektRuleDefinitionGeneratorKt")

        doFirst {
            println("Updating rules for Detekt version $detektVersion...")
        }
    }

    task<JavaExec>("updateKtlintRules") {
        group = "Application"
        classpath = sourceSets.main.get().runtimeClasspath
        mainClass.set("org.sonarsource.kotlin.externalreport.ktlint.KtlintRuleDefinitionGeneratorKt")

        doFirst {
            println("Updating rules for ktlint version $detektVersion...")
        }
    }

    task<JavaExec>("updateAndroidLintRules") {
        group = "Application"
        classpath = sourceSets.main.get().runtimeClasspath
        mainClass.set("org.sonarsource.kotlin.externalreport.androidlint.AndroidLintRuleDefinitionKt")

        doFirst {
            println("Updating rules for Android Lint")
        }
    }

    task<JavaExec>("printAst") {
        dependsOn(":sonar-kotlin-plugin:compileJava")
        group = "Application"
        classpath = sourceSets.main.get().runtimeClasspath
        mainClass.set("org.sonarsource.kotlin.ast.AstPrinterKt")
    }
}
