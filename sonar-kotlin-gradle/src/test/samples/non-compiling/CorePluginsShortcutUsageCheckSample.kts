plugins {
    id("org.gradle.java") // Noncompliant {{Replace this with the core plugin short name `java`}}
    id("org.gradle.java-library") // Noncompliant {{Replace this with the core plugin short name ``java-library``}}
    id("org.gradle.test-report-aggregation") // Noncompliant

    // Plugins that do not have a known name at the time of implementation should also work
    id("org.gradle.foo") // Noncompliant

    // Compliant:
    java
    `maven-publish`
    `test-report-aggregation`
    id("some.thing.else") // Compliant, not a core plugin

    val pluginId = "org.gradle.java"
    id(pluginId) // Compliant FN - for now, we don't have semantics and hence don't support resultion of the value.

    id("org.gradle.subpckg.foo") // Compliant, it is not in "org.gradle" but in a subpackage
    id()
    id("org.gradle.java", "") // Compliant, not the single-arg `id` fun we are looking for
    with (::id) {
        this("org.gradle.java") // Compliant, We don't support such constructs
    }
    notId("org.gradle.java") // Compliant, not `id`
}
