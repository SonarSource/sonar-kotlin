plugins {
    java
    application
    kotlin("jvm")
}

val detektVersion: String by project
val ktlintVersion: String by project
val jcommanderVersion: String by project
val commonsTextVersion: String by project

dependencies {
    implementation("io.gitlab.arturbosch.detekt", "detekt-cli", detektVersion)
    implementation("io.gitlab.arturbosch.detekt", "detekt-core", detektVersion)
    implementation("io.gitlab.arturbosch.detekt", "detekt-api", detektVersion)
    constraints {
        runtimeOnly("org.yaml:snakeyaml:1.32") {
            because("detekt-core brings an outdated version of snakeyaml exposed to CVE-2022-25857")
        }
    }
    implementation("com.pinterest", "ktlint", ktlintVersion)
    implementation("com.pinterest.ktlint", "ktlint-core", ktlintVersion)
    implementation("com.pinterest.ktlint", "ktlint-ruleset-standard", ktlintVersion)
    implementation("com.pinterest.ktlint", "ktlint-ruleset-experimental", ktlintVersion)

    implementation(kotlin("stdlib-jdk8"))

    implementation("com.google.code.gson:gson")
    implementation("com.beust:jcommander:$jcommanderVersion")
    implementation("org.apache.commons:commons-text:$commonsTextVersion")
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
