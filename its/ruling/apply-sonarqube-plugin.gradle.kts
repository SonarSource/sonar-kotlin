initscript {
    repositories {
        gradlePluginPortal()
    }
    dependencies {
        classpath("org.sonarsource.scanner.gradle:sonarqube-gradle-plugin:latest.release")
    }
}

rootProject {
    apply<org.sonarqube.gradle.SonarQubePlugin>()
}
