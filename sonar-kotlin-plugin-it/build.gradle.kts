plugins {
    kotlin("jvm")
}

val shadowJar = project(":sonar-kotlin-plugin").tasks.named("shadowJar")

dependencies {
    implementation(files(shadowJar))
    testImplementation(testLibs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
