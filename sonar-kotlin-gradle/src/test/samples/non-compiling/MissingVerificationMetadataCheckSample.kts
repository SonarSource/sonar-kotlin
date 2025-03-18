plugins {
    id("com.android.application")
}

configurations {
    named("jetbrainsRuntimeLocalInstance") {
        resolutionStrategy.disableDependencyVerification() // Noncompliant {{This call disables dependencies verification. Make sure it is safe here.}}
        //                 ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    }
    named("jetbrainsRuntimeDependency") {
        resolutionStrategy {
            disableDependencyVerification() // Noncompliant
        //  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        }
    }
    named("jetbrainsRuntime") {
        resolutionStrategy {
            this.disableDependencyVerification() // Noncompliant
        }
    }
}

detachedConfig.resolutionStrategy.disableDependencyVerification() // Noncompliant

if (name.contains("detached")) {
    disableDependencyVerification() // Noncompliant
}

someGroup {
    tasks.register("checkDetachedDependencies") {
        val detachedConf: FileCollection = configurations.detachedConfiguration(dependencies.create("org.apache.commons:commons-lang3:3.3.1")).apply {
            resolutionStrategy.disableDependencyVerification() // Noncompliant
        }
        doLast {
            println(detachedConf.files)
        }
    }

    disableDependencyVerification { // Noncompliant
    }
}
