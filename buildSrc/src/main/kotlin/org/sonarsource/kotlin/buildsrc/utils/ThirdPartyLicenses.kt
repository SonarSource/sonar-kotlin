package org.sonarsource.kotlin.buildsrc.utils

data class DependencyInfo(
    val fqName: String,
    val packages: List<String> = emptyList(),
    val licenseName: String
)

// Set of all dependencies embedded in kotlin-compiler.jar
val kotlinCompilerDependencies = setOf(
    DependencyInfo(
        fqName = "com.fasterxml:aalto-xml",
        packages = listOf("com/fasterxml/aalto"),
        licenseName = "Apache 2.0"
    ),
    DependencyInfo(
        fqName = "com.fasterxml.woodstox:woodstox-core",
        packages = listOf("com/ctc"),
        licenseName = "Apache 2.0"
    ),
    DependencyInfo(
        fqName = "org.codehaus.woodstox:stax2-api",
        packages = listOf("org/codehaus/stax2"),
        licenseName = "BSD" // Note: in AnalyzerLicensingPackagingRenderer.kt, we map "BSD" as BSD 2-clause
    ),
    DependencyInfo(
        fqName = "com.github.ben-manes.caffeine:caffeine",
        packages = listOf("com/github/benmanes/caffeine"),
        licenseName = "Apache 2.0"
    ),
    DependencyInfo(
        fqName = "com.google.guava:guava",
        packages = listOf("com/google/common"),
        licenseName = "Apache 2.0"
    ),
    DependencyInfo(
        fqName = "com.google.gwt:gwt-user",
        packages = listOf("com/google/gwt"),
        licenseName = "Apache 2.0" // See https://www.gwtproject.org/terms.html
    ),
    DependencyInfo(
        fqName = "com.sun.jna:jna",
        packages = listOf("com/sun/jna"),
        // Kotlin is using JNA 5.x (https://github.com/JetBrains/kotlin/blob/master/gradle/versions.properties)
        // which is Apache 2.0; older versions (<4) of JNA were LGPL
        licenseName = "Apache 2.0"
    ),
    DependencyInfo(
        fqName = "io.opentelemetry:opentelemetry-api",
        packages = listOf("io/opentelemetry"),
        licenseName = "Apache 2.0"
    ),

    DependencyInfo(
        fqName = "io.vavr:vavr",
        packages = listOf("io/vavr"),
        licenseName = "Apache 2.0"
    ),
    DependencyInfo(
        fqName = "it.unimi.dsi:fastutil",
        packages = listOf("it/unimi/dsi"),
        licenseName = "Apache 2.0"
    ),
    DependencyInfo(
        fqName = "one.util.streamex:streamex",
        packages = listOf("one/util/streamex"),
        licenseName = "Apache 2.0"
    ),
    DependencyInfo(
        fqName = "org.apache.logging.log4j:log4j-api",
        packages = listOf("org/apache/log4j"),
        licenseName = "Apache 2.0"
    ),
    DependencyInfo(
        fqName = "org.jdom:jdom2",
        packages = listOf("org/jdom"),
        licenseName = "Apache 2.0" // Actually a bit special version: https://www.jdom.org/docs/faq.html#a0030
    ),
    DependencyInfo(
        fqName = "org.picocontainer:picocontainer",
        packages = listOf("org/picocontainer"),
        licenseName = "BSD"
    ),

    DependencyInfo(
        fqName = "javax.inject:javax.inject",
        packages = listOf("javax/inject"),
        licenseName = "Apache 2.0"
    ),

    DependencyInfo(
        fqName = "org.jetbrains.kotlin:kotlin-compiler",
        packages = listOf("com/intellij", "org/jetbrains", "kotlin/"),
        licenseName = "Apache 2.0"
    ),
    DependencyInfo(
        fqName = "org.jetbrains.kotlinx:kotlinx-collections-immutable",
        packages = listOf("kotlinx/collections/immutable"),
        licenseName = "Apache 2.0"
    ),

    // Resources and metadata
    DependencyInfo(
        fqName = "resources",
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
