import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

apply(plugin = "org.jetbrains.kotlin.jvm")

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.1")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.5.21")
    implementation("org.jetbrains.kotlin:kotlin-test:1.5.21")
    implementation("com.squareup.okhttp3:okhttp:4.9.0")
    implementation("commons-net:commons-net:3.8.0")
    implementation("org.springframework.security:spring-security-crypto:5.7.2")
    implementation("org.springframework.security:spring-security-core:4.2.17.RELEASE")
    implementation("commons-codec:commons-codec:1.13")
    implementation("com.google.guava:guava:30.1.1-jre")
    implementation("org.apache.commons:commons-lang3:3.9")
    implementation("commons-lang:commons-lang:2.6")
}

sonarqube.isSkipProject = true

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    suppressWarnings = true
}

task<Copy>("copyAllDependencies") {
    from(configurations.runtimeClasspath)
    into("build/test-jars")
}

tasks.build {
    finalizedBy(tasks.named("copyAllDependencies"))
}
