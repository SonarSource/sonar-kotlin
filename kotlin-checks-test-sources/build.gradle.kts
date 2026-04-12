import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

apply(plugin = "org.jetbrains.kotlin.jvm")

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.4.0-mars-471")
    implementation("org.jetbrains.kotlin:kotlin-test:2.4.0-mars-471")
    implementation("org.jetbrains.kotlin:kotlin-test-junit5:2.4.0-mars-471")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("commons-net:commons-net:3.13.0")
    implementation("org.springframework.security:spring-security-crypto:5.8.16")
    implementation("org.springframework.security:spring-security-core:4.2.20.RELEASE")
    implementation("commons-codec:commons-codec:1.21.0")
    implementation("com.google.guava:guava:30.1.1-jre")
    implementation("org.apache.commons:commons-lang3:3.20.0")
    implementation("commons-lang:commons-lang:2.6")
}

sonarqube.isSkipProject = true

val compileKotlin: KotlinCompile by tasks
compileKotlin.compilerOptions {
    suppressWarnings = true
}

task<Copy>("copyAllDependencies") {
    from(configurations.runtimeClasspath)
    into("build/test-jars")
}

tasks.build {
    finalizedBy(tasks.named("copyAllDependencies"))
}
