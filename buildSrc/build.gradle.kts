plugins {
    // Support convention plugins written in Kotlin. Convention plugins are build scripts in 'src/main' that automatically become available as plugins in the main build.
    `kotlin-dsl`
}

repositories {
    // Use the plugin portal to apply community plugins in convention plugins.
    gradlePluginPortal()
}

configurations.matching { it.name == "kotlinBouncyCastleConfiguration" }.configureEach {
    // Workaround for https://github.com/gradle/gradle/issues/35309.
    // When any of cloud-native Gradle plugins is applied in a project
    // whose Gradle version embeds Kotlin <2.3.20, there will be an unnecessary dependency on build classpath.
    withDependencies { clear() }
}
