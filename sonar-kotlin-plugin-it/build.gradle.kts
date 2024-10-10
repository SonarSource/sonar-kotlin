plugins {
    kotlin("jvm")
}

val shadowJar = project(":sonar-kotlin-plugin").tasks.named("shadowJar")

// TODO look at https://github.com/JetBrains/kotlin/commit/719addaa51bd52d2a8f52ba706f4caf2ed58fec0#diff-c0dfa6bc7a8685217f70a860145fbdf416d449eaff052fa28352c5cec1a98c06L535-L538
// as a way to hide fat jar from searches

dependencies {
    implementation(files(shadowJar))
    testImplementation(testLibs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
