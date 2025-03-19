// Glossary:
// - App: Android application (identified by the presence of the applicationId property in the defaultConfig block)
// - Library: Android library (identified by the absence of the applicationId property in the defaultConfig block)

// region non-compliant scenarios

// App: basic case
android {
    var someVariable = "com.example"
    defaultConfig {
        applicationId = someVariable // Android app, not a library
    }

    buildTypes {
        release {
            isDebuggable = true // Noncompliant {{Make sure this debug feature is deactivated before delivering the code in production.}}
//          ^^^^^^^^^^^^^^^^^^^
        }
    }
}

// App: lambda in argument list within parentheses
android({
    var someVariable = "com.example"
    defaultConfig {
        applicationId = someVariable // Android app, not a library
    }

    buildTypes({
        release({
            isDebuggable = true // Noncompliant
//          ^^^^^^^^^^^^^^^^^^^
        })
    })
})

// App: lambda within standalone parentheses (not parentheses of argument list)
(
    android {
        (
            buildTypes {
                (
                    release {
                        isDebuggable = true // Noncompliant
//                      ^^^^^^^^^^^^^^^^^^^
                    }
                    )
            }
            )

        defaultConfig {
            applicationId = AndroidConfig.ID // Android app, not a library
        }
    }
    )

// App: isDebuggable read and assigned
kotlin {
    android {
        defaultConfig {
            applicationId = "com.example" // Android app, not a library
        }
        buildTypes {
            release {
                print(isDebuggable)
                isDebuggable = true // Noncompliant
            }
        }
    }
}

// App: using getByName and BuildType
android {
    defaultConfig {
        applicationId = "com.example" // Android app, not a library
    }
    buildTypes {
        getByName(BuildType.RELEASE) {
            isDebuggable = true // Noncompliant
//          ^^^^^^^^^^^^^^^^^^^
        }
    }
}

// App: using getByName and string
android {
    defaultConfig {
        applicationId = "com.example" // Android app, not a library
    }
    buildTypes {
        getByName("release") {
            isDebuggable = true // Noncompliant
//          ^^^^^^^^^^^^^^^^^^^
        }
    }
}

// App: using getByName and triple-quote string
android {
    defaultConfig {
        applicationId = "com.example" // Android app, not a library
    }
    buildTypes {
        getByName("""release""") {
            isDebuggable = true // Noncompliant
//          ^^^^^^^^^^^^^^^^^^^
        }
    }
}

// App: using getByName and interpolated string
android {
    defaultConfig {
        applicationId = "com.example" // Android app, not a library
    }
    buildTypes {
        getByName("${"release"}") {
            isDebuggable = true // Noncompliant
        }
    }
}

// App: using getByName and val
android {
    defaultConfig {
        applicationId = "com.example" // Android app, not a library
    }
    buildTypes {
        val release = "release"
        getByName(release) {
            isDebuggable = true // Noncompliant
        }
    }
}

// App: using maybeCreate and apply
android {
    defaultConfig {
        applicationId = "com.example" // Android app, not a library
    }
    buildTypes {
        maybeCreate(BuildTypes.RELEASE).apply {
            isDebuggable = true // FN
        }
    }
}

// App: isDebuggable assigned val
android {
    defaultConfig {
        applicationId = "com.example" // Android app, not a library
    }
    buildTypes {
        release {
            val trueVal = true
            isDebuggable = trueVal // Noncompliant
        }
    }
}

// App: isDebuggable assigned var
android {
    defaultConfig {
        applicationId = "com.example" // Android app, not a library
    }
    buildTypes {
        release {
            var trueVal = true
            isDebuggable = trueVal // FN
        }
    }
}

// App: foreach of platforms
listOf(android(), iosX64(), iosArm64()).forEach {
    it.defaultConfig {
        applicationId = "com.example" // Android app, not a library
    }
    it.buildTypes {
        release {
            isDebuggable = true // FN
        }
    }
}

// App: lambda argument not a function literal
fun lambdaArgument(configure: BaseAppModuleExtension) = with(configure) {
    defaultConfig {
        applicationId = "com.example" // Android app, not a library
    }
    buildTypes {
        release {
            isDebuggable = true // FN
        }
    }
}
android(::lambdaArgument)

// endregion

// region compliant scenarios

// Different block than android
plugins {
    id("com.android.application")
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.3.61")
    implementation("androidx.core:core-ktx:1.2.0")
    implementation("androidx.appcompat:appcompat:1.1.0")
    implementation(project(":awesome-textinput-layout"))
}

// Empty android block with no lambda
android()

// Empty non-android block with no lambda
iosX64()

// Library: no buildTypes
android {
}

// Library: no buildTypes and different block and properties under android
android {
    namespace = "com.example"
    compileSdk = 33
}

// Library: no release properties
android {
    buildTypes {
        release {
        }
    }
}

// Library: defaultConfig present, but applicationId not set
android {
    defaultConfig {
        notApplicationId = "com.example"
    }
    buildTypes {
        release {
            isDebuggable = true
        }
    }
}

// App: android block without buildTypes
android {
    defaultConfig {
        applicationId = "com.example" // Android app, not a library
    }
}

// App: different block (not release) under buildTypes
android {
    defaultConfig {
        applicationId = "com.example" // Android app, not a library
    }
    buildTypes {
        debug {
            isDebuggable = true
        }
    }
}

// App: using isDebuggable set to false (default)
android {
    defaultConfig {
        applicationId = "com.example" // Android app, not a library
    }
    buildTypes {
        release {
            isDebuggable = false
        }
    }
}

// App: android with isDebuggable set to false (default) under kotlin block
kotlin {
    android {
        defaultConfig {
            applicationId = "com.example" // Android app, not a library
        }
        buildTypes {
            release {
                isDebuggable = false
            }
        }
    }
}

// App: block with multiple or differently-typed arguments (not the overload from org.gradle.kotlin.dsl)
android(42) {
    defaultConfig {
        applicationId = "com.example" // Android app, not a library
    }
    buildTypes {
        release("a string")
    }
}

// App: using getByName
android {
    defaultConfig {
        applicationId = "com.example" // Android app, not a library
    }
    buildTypes {
        getByName(BuildType.DEBUG) {
            isDebuggable = true
        }
        getByName("debug") {
            isDebuggable = true
        }
        getByName("release", 42)
    }
}

// App: using create
android {
    defaultConfig {
        applicationId = "com.example" // Android app, not a library
    }
    buildTypes {
        create("beta") {
            isDebuggable = true
        }
    }
}

// App: using maybeCreate and apply
android {
    defaultConfig {
        applicationId = "com.example" // Android app, not a library
    }
    buildTypes {
        maybeCreate(BuildTypes.DEBUG).apply {
            isDebuggable = true
        }
    }
}

// App: isDebuggable read but not assigned
kotlin {
    android {
        defaultConfig {
            applicationId = "com.example" // Android app, not a library
        }
        buildTypes {
            release {
                print(isDebuggable)
            }
        }
    }
}

// endregion
