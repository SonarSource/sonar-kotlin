plugins {
    java
    application
    kotlin("jvm")
}

repositories {
    maven { url = uri("https://repo.gradle.org/gradle/libs-releases") }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(utilLibs.bundles.detekt)
    // detekt needs "kotlin-compiler-embeddable:{strictly 1.7.21}", to fix this we can specify a version explicitly:
    implementation(libs.kotlin.compiler.embeddable)
    implementation(utilLibs.bundles.ktlint)

    // The 2 following modules migrated to kotlin-compiler, but in the current module we still
    // need kotlin-compiler-embeddable, due to detekt and ktlint. So we need to disable transitivity
    // in these 2 dependencies, to avoid bringing th kotlin-compiler dependency
    implementation(project(":sonar-kotlin-plugin")) {
        isTransitive = false
    }
    implementation(project(":sonar-kotlin-api")) {
        isTransitive = false
    }

    implementation(libs.gson)
    implementation(utilLibs.jcommander)

    testRuntimeOnly(testLibs.junit.engine)
    testImplementation(testLibs.junit.api)
    testImplementation(testLibs.assertj.core)
    testImplementation(libs.sonar.plugin.api)

    implementation(project(":sonar-kotlin-external-linters")) {
        isTransitive = false
    }
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
