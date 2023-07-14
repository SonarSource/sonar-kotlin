// Noncompliant@0 {{Assign `rootProject.name` in `settings.gradle.kts`.}}

dependencyResolutionManagement {
    // ...
}

rootProject.name == "myProject"
rootProject.title = "myProject"
project.name = "myRepo"
project.title = "myRepo"
name = "myRepo"

rootProject.setTitle("myProject")
project.setName("myProject")
project.setTitle("myRepo")
setName("myProject")
