plugins {
    java
    application
    kotlin("jvm")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(project(":sonar-kotlin-plugin"))

    implementation(utilLibs.bundles.detekt)
    implementation(utilLibs.bundles.ktlint)

    implementation(libs.gson)
    implementation(utilLibs.jcommander)

    testImplementation(testLibs.junit.api)
    testRuntimeOnly(testLibs.junit.engine)
    testImplementation(testLibs.assertj.core)
    testImplementation(libs.sonar.plugin.api)
}

tasks {
    task<JavaExec>("updateDetektRules") {
        group = "Application"
        classpath = sourceSets.main.get().runtimeClasspath
        mainClass.set("org.sonarsource.kotlin.externalreport.detekt.DetektRuleDefinitionGeneratorKt")

        doFirst {
            println("Updating rules for Detekt version ${utilLibs.versions.detekt.get()}...")
        }
    }

    task<JavaExec>("updateKtlintRules") {
        group = "Application"
        classpath = sourceSets.main.get().runtimeClasspath
        mainClass.set("org.sonarsource.kotlin.externalreport.ktlint.KtlintRuleDefinitionGeneratorKt")

        doFirst {
            println("Updating rules for ktlint version ${utilLibs.versions.ktlint.get()}...")
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
