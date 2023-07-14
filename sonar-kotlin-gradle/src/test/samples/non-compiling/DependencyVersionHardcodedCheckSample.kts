val springVersion = "5.0.2.RELEASE"
val springWeb = "spring-web:$springVersion"
val mockitoVersion = "4.5.1"
val slf4jVersion = "1.7.15"

dependencies {

    implementation("org.springframework:spring-web:5.0.2.RELEASE") // Noncompliant {{Do not hardcode version numbers.}}
//                                                 ^^^^^^^^^^^^^
    implementation("org.springframework:spring-web:$springVersion") // Compliant
    implementation("org.springframework:spring-web:" + springVersion) // Compliant

    implementation("org.springframework:spring-web:") // Compliant
    implementation("org.springframework:spring-web") // Compliant
    implementation("org.springframework") // Compliant
    implementation("org.springframework:$springWeb") // Compliant

    foo("org.springframework:spring-web:5.0.2.RELEASE") // Compliant
    foo("org.springframework", "spring-web", "5.0.2.RELEASE") // Compliant

    implementation("org.springframework", "spring-web", "5.0.2.RELEASE") // Noncompliant {{Do not hardcode version numbers.}}
//                                                       ^^^^^^^^^^^^^
    implementation("org.springframework", "spring-web", version = "5.0.2.RELEASE") // Noncompliant
//                                                                 ^^^^^^^^^^^^^
    runtimeOnly("org.springframework", version = "5.0.2.RELEASE", name = "spring-web") // Noncompliant
//                                                ^^^^^^^^^^^^^
    implementation("org.springframework", "spring-web", notAVersion = "5.0.2.RELEASE") // Compliant

    implementation("org.springframework", "spring-web", springVersion) // Compliant
    implementation("org.springframework", "spring-web", version = springVersion) // Compliant
    implementation("org.springframework", version = springVersion, name = "spring-web") // Compliant

    implementation("org.springframework", "spring-web", "$springVersion") // Compliant
    implementation("org.springframework", "spring-web", version = "$springVersion") // Compliant
    implementation("org.springframework", version = "$springVersion", name = "spring-web") // Compliant

    testImplementation("org.mockito:mockito-core:$mockitoVersion") // Compliant
    testImplementation("org.mockito:mockito-inline:$mockitoVersion") // Compliant

    testImplementation("org.mockito:mockito-core:4.5.1") // Noncompliant
    testImplementation("org.mockito:mockito-inline:4.5.1") // Noncompliant
    testImplementation("org.mockito:mockito-inline:4.5-ALPHA") // Noncompliant
    testImplementation("org.mockito:mockito-inline:1-a+1") // Noncompliant
    implementation("org.slf4j:slf4j-api:1.7.15") // Noncompliant

    implementation(org.foo.bar) // Compliant
    implementation(project(":my-module")) // Compliant
    implementation(files("./lib/alib.jar")) // Compliant

    compile("org.springframework:spring-web:5.0.2.RELEASE") // Noncompliant
    compileClasspath("org.springframework:spring-web:5.0.2.RELEASE") // Noncompliant
    compileOnly("org.springframework:spring-web:5.0.2.RELEASE") // Noncompliant
    implementation("org.springframework:spring-web:5.0.2.RELEASE") // Noncompliant
    runtime("org.springframework:spring-web:5.0.2.RELEASE") // Noncompliant
    runtimeClasspath("org.springframework:spring-web:5.0.2.RELEASE") // Noncompliant
    runtimeOnly("org.springframework:spring-web:5.0.2.RELEASE") // Noncompliant
    testCompile("org.springframework:spring-web:5.0.2.RELEASE") // Noncompliant
    testCompileClasspath("org.springframework:spring-web:5.0.2.RELEASE") // Noncompliant
    testCompileOnly("org.springframework:spring-web:5.0.2.RELEASE") // Noncompliant
    testImplementation("org.springframework:spring-web:5.0.2.RELEASE") // Noncompliant
    testRuntime("org.springframework:spring-web:5.0.2.RELEASE") // Noncompliant
    testRuntimeClasspath("org.springframework:spring-web:5.0.2.RELEASE") // Noncompliant
    testRuntimeOnly("org.springframework:spring-web:5.0.2.RELEASE") // Noncompliant

    constraints {
        implementation("org.springframework:spring-web:5.0.2.RELEASE") // Compliant, we ignore dependencies in "constraints"
        implementation("org.springframework:spring-web:$springVersion") // Compliant
        implementation("org.springframework:spring-web:" + springVersion) // Compliant
        implementation("org.springframework:spring-web:") // Compliant
    }

    implementation("org.slf4j:slf4j-api:1.7.15!!") // Noncompliant
//                                      ^^^^^^^^

    implementation("org.slf4j:slf4j-api") { // Compliant
        version {
            strictly("1.7.15") // Compliant, ignore hard-coded versions in "version"
            require("[1.7, 1.8[") // Compliant
            prefer("1.7.25") // Compliant
            reject("1.7.25") // Compliant
        }
    }
}
