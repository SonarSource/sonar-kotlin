// Noncompliant {{Assign `rootProject.name` in `settings.gradle.kts`}}
//^[sc=1;ec=1]

dependencyResolutionManagement {
    // ...
}

project = "myProject"
name = "myProject"

project == "myProject"
project.name == "myProject"
