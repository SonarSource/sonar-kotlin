// region non-compliant scenarios

// No release properties
android {
    buildTypes {
        release { // Noncompliant {{Enable obfuscation by setting isMinifiedEnabled.}}
//      ^^^^^^^
        }
    }
}

// Lambda in argument list within parentheses
android({
    buildTypes({
        release({ // Noncompliant
//      ^^^^^^^
        })
    })
})

// Lambda within standalone parentheses (not parentheses of argument list)
(
    android {
        (
            buildTypes {
                (
                    release { // Noncompliant {{Enable obfuscation by setting isMinifiedEnabled.}}
//                  ^^^^^^^
                    }
                )
            }
        )
    }
)

// Different release properties
android {
    buildTypes {
        release { // Noncompliant
            namespace = "com.example"
        }
    }
}

// android under kotlin block
kotlin {
    android {
        buildTypes {
            release { // Noncompliant
            }
        }
    }
}

// isMinifyEnabled read but not assigned
kotlin {
    android {
        buildTypes {
            release { // Noncompliant
                print(isMinifyEnabled)
            }
        }
    }
}

// isMinifyEnabled read in a binary operation which is not an assignnment
kotlin {
    android {
        buildTypes {
            release { // Noncompliant
                isMinifyEnabled && true
                true && isMinifyEnabled
            }
        }
    }
}

// using setter
android {
    buildTypes {
        release { // Noncompliant
            setIsMinifiedEnable(false) // Not a valid setter
            setMinifiedEnable(false) // Not a valid setter
        }
    }
}

// using getByName and BuildType
android {
    buildTypes {
        getByName(BuildType.RELEASE) { // FN
            isMinifyEnabled = false
        }
    }
}

// using getByName and string
android {
    buildTypes {
        getByName("release") { // FN
            isMinifyEnabled = false
        }
    }
}

// using maybeCreate and apply
android {
    buildTypes {
        maybeCreate(BuildTypes.RELEASE).apply { // FN
            isMinifyEnabled = false
        }
    }
}

// isMinifyEnabled assigned val
android {
    buildTypes {
        release { // FN
            val trueVal = false
            isMinifyEnabled = trueVal
        }
    }
}

// isMinifyEnabled assigned var
android {
    buildTypes {
        release { // FN
            var trueVal = false
            isMinifyEnabled = trueVal
        }
    }
}

// isMinifyEnabled assigned boolean expression
android {
    buildTypes {
        release { // FN
            isMinifyEnabled = false && true
        }
    }
}

// foreach of platforms
listOf(android(), iosX64(), iosArm64()).forEach {
    it.buildTypes {
        release { // FN
        }
    }
}

// lambda argument not a function literal
fun lambdaArgument(configure: BaseAppModuleExtension) = with(configure) {
    buildTypes {
        release { // FN
            namespace = "com.example"
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

// Different block and properties under android
android {
    namespace = "com.example"
    compileSdk = 33
    defaultConfig {
        applicationId = "com.example"
    }
}

// Different block under buildTypes
android {
    buildTypes {
        debug {
            isMinifyEnabled = true
        }
    }
}

// android under kotlin block
kotlin {
    android {
        buildTypes {
            release {
                isMinifyEnabled = true
            }
        }
    }
}

// android under kotlin block
kotlin {
    android {
        buildTypes {
            release {
                val trueVal = true
                isMinifyEnabled = trueVal
            }
        }
    }
}

// block with multiple or differently-typed arguments (not the overload from org.gradle.kotlin.dsl)
android(42) {
    buildTypes {
        release("a string")
    }
}

// using getByName
android {
    buildTypes {
        getByName(BuildType.DEBUG) {
            isMinifyEnabled = false
        }
        getByName("debug") {
            isMinifyEnabled = false
        }
    }
}

// using create
android {
    buildTypes {
        create("beta") {
            isMinifyEnabled = false
        }
    }
}

// using maybeCreate and apply
android {
    buildTypes {
        maybeCreate(BuildTypes.DEBUG).apply {
            isMinifyEnabled = false
        }
    }
}

// endregion
