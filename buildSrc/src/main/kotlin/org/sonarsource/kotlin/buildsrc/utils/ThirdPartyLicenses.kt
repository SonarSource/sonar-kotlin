package org.sonarsource.kotlin.buildsrc.utils

data class DependencyInfo(
    val fqName: String,
    val version: String,
    val packages: List<String> = emptyList(),
    val licenseName: String,
)

/*
 * Set of all dependencies embedded in kotlin-compiler.jar.
 * The versions are taken either from the [versions.properties](https://github.com/JetBrains/kotlin/blob/master/gradle/versions.properties),
 * or [libs.versions.toml](https://github.com/JetBrains/kotlin/blob/master/gradle/libs.versions.toml#L39).
 */
val kotlinCompilerDependencies = setOf(
    DependencyInfo(
        fqName = "com.fasterxml:aalto-xml",
        version = "1.3.0",
        packages = listOf("com/fasterxml/aalto"),
        licenseName = "Apache 2.0"
    ),
    DependencyInfo(
        fqName = "com.fasterxml.woodstox:woodstox-core",
        version = "7.1.0",
        packages = listOf("com/ctc"),
        licenseName = "Apache 2.0"
    ),
    DependencyInfo(
        fqName = "org.codehaus.woodstox:stax2-api",
        version = "4.2.1",
        packages = listOf("org/codehaus/stax2"),
        licenseName = "BSD" // Note: in AnalyzerLicensingPackagingRenderer.kt, we map "BSD" as BSD 2-clause
    ),
    DependencyInfo(
        fqName = "com.github.ben-manes.caffeine:caffeine",
        version = "2.9.3",
        packages = listOf("com/github/benmanes/caffeine"),
        licenseName = "Apache 2.0"
    ),
    DependencyInfo(
        fqName = "com.google.guava:guava",
        version = "33.3.1-jre",
        packages = listOf("com/google/common"),
        licenseName = "Apache 2.0"
    ),
    DependencyInfo(
        fqName = "org.gwtproject:gwt-user",
        version = "2.11.0",
        packages = listOf("com/google/gwt"),
        licenseName = "Apache 2.0" // See https://www.gwtproject.org/terms.html
    ),
    DependencyInfo(
        fqName = "net.java.dev.jna:jna",
        version = "5.9.0.26",
        packages = listOf("com/sun/jna"),
        // Kotlin is using JNA 5.x (https://github.com/JetBrains/kotlin/blob/master/gradle/versions.properties)
        // which is Apache 2.0; older versions (<4) of JNA were LGPL and had different groupId
        licenseName = "Apache 2.0"
    ),
    DependencyInfo(
        fqName = "io.opentelemetry:opentelemetry-api",
        version = "1.41.0",
        packages = listOf("io/opentelemetry"),
        licenseName = "Apache 2.0"
    ),

    DependencyInfo(
        fqName = "io.vavr:vavr",
        version = "0.10.4",
        packages = listOf("io/vavr"),
        licenseName = "Apache 2.0"
    ),
    DependencyInfo(
        fqName = "it.unimi.dsi:fastutil",
        version = "8.5.16",
        packages = listOf("it/unimi/dsi"),
        licenseName = "Apache 2.0"
    ),
    DependencyInfo(
        fqName = "one.util:streamex",
        version = "0.7.2",
        packages = listOf("one/util/streamex"),
        licenseName = "Apache 2.0"
    ),
    DependencyInfo(
        fqName = "log4j:log4j",
        version = "1.2.17",
        packages = listOf("org/apache/log4j"),
        licenseName = "Apache 2.0"
    ),
    DependencyInfo(
        fqName = "org.jdom:jdom2",
        version = "2.0.6.1",
        packages = listOf("org/jdom"),
        licenseName = "Apache 2.0" // Actually a bit special version: https://www.jdom.org/docs/faq.html#a0030
    ),
    DependencyInfo(
        fqName = "org.picocontainer:picocontainer",
        version = "2.15",
        packages = listOf("org/picocontainer"),
        licenseName = "BSD"
    ),

    DependencyInfo(
        fqName = "javax.inject:javax.inject",
        version = "1",
        packages = listOf("javax/inject"),
        licenseName = "Apache 2.0"
    ),

    DependencyInfo(
        fqName = "org.jetbrains.kotlin:kotlin-compiler",
        version = "2.2.21",
        packages = listOf("com/intellij", "org/jetbrains", "kotlin/"),
        licenseName = "Apache 2.0"
    ),
    DependencyInfo(
        fqName = "org.jetbrains.kotlinx:kotlinx-collections-immutable",
        version = "0.3.7",
        packages = listOf("kotlinx/collections/immutable"),
        licenseName = "Apache 2.0"
    ),

    // Resources and metadata
    DependencyInfo(
        fqName = "resources",
        version = "N/A",
        packages = listOf("META-INF/", "messages/", "misc/", "custom-formatters.js", "kotlinManifest.properties"),
        licenseName = "N/A" // N/A for resources
    ),
)

val packagesToDependencies: Map<String, String> by lazy {
    kotlinCompilerDependencies
        .flatMap { info ->
            info.packages.map { pkg -> pkg to info.fqName }
        }
        .toMap()
}
