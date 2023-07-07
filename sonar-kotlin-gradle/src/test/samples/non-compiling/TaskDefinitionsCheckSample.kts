tasks.register<DocsGenerate>("generateHtmlDocs") { // Noncompliant {{Define `"group"` and `"description"` for this task}}
//^[sc=1;ec=48]
    title.set("Project docs")
    outputDir.set(layout.buildDirectory.dir("docs"))
}

tasks.create<DocsGenerate>("generateHtmlDocs") { // Noncompliant {{Define `"group"` and `"description"` for this task}}
//^[sc=1;ec=46]
    title.set("Project docs")
    outputDir.set(layout.buildDirectory.dir("docs"))
}

tasks.register<DocsGenerate>("generateHtmlDocs") { // Noncompliant {{Define `"group"` for this task}}
    description = "Generates the HTML documentation for this project."
    title.set("Project docs")
    outputDir.set(layout.buildDirectory.dir("docs"))
}

tasks.register<DocsGenerate>("generateHtmlDocs") { // Noncompliant {{Define `"description"` for this task}}
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    title.set("Project docs")
    outputDir.set(layout.buildDirectory.dir("docs"))
}

tasks.register<DocsGenerate> { // Noncompliant {{Define `"description"` for this task}}
//^[sc=1;ec=14]
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    title.set("Project docs")
    outputDir.set(layout.buildDirectory.dir("docs"))
}

tasks.register<DocsGenerate>("generateHtmlDocs") { // Compliant
    description = "Generates the HTML documentation for this project."
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    title.set("Project docs")
    outputDir.set(layout.buildDirectory.dir("docs"))
}

tasks.register<DocsGenerate>("generateHtmlDocs") { // Noncompliant {{Define `"group"` and `"description"` for this task}}
}

tasks.register<DocsGenerate>("generateHtmlDocs") // Compliant, no lambda

tasks.create<DocsGenerate>("generateHtmlDocs") { // Noncompliant {{Define `"group"` and `"description"` for this task}}
}

tasks.create<DocsGenerate>("generateHtmlDocs") // Compliant, no lambda

register("generateHtmlDocs") { // Compliant
    other = "foo"
}

tasks("generateHtmlDocs") { // Compliant
    other = "foo"
}

other("generateHtmlDocs") { // Compliant
    other = "foo"
}

other.register("generateHtmlDocs") { // Compliant
    other = "foo"
}

tasks.other("generateHtmlDocs") { // Compliant
    other = "foo"
}

tasks.register<DocsGenerate>("generateHtmlDocs") { // Compliant
    description = "Generates the HTML documentation for this project."
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    other = "foo"
}

tasks.register<DocsGenerate>("generateHtmlDocs") { // Noncompliant {{Define `"group"` for this task}}
    description = "Generates the HTML documentation for this project."
    group[42] = JavaBasePlugin.DOCUMENTATION_GROUP
    other = "foo"
}
