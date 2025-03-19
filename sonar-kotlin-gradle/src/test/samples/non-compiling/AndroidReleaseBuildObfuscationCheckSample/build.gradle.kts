// Glossary:
// - App: Android application (identified by the presence of the applicationId property in the defaultConfig block)
// - Library: Android library (identified by the absence of the applicationId property in the defaultConfig block)

// region non-compliant scenarios

// App: no buildTypes
    android { // Noncompliant {{Make sure that obfuscation is enabled in the release build configuration.}}
//  ^^^^^^^
        defaultConfig {
            applicationId = "com.example" // Android app, not a library
        }
}

// App: no buildTypes and different block and properties under android
    android { // Noncompliant {{Make sure that obfuscation is enabled in the release build configuration.}}
//  ^^^^^^^
    namespace = "com.example"
    compileSdk = 33
    defaultConfig {
        applicationId = "com.example" // Android app, not a library
    }
}

// App: no release properties
android {
    defaultConfig {
        val SOME_CONSTANT = "com.example"
        applicationId = SOME_CONSTANT // Android app, not a library
    }

    buildTypes {
        release { // Noncompliant {{Make sure that obfuscation is enabled in the release build configuration.}}
//      ^^^^^^^
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
        release({ // Noncompliant {{Make sure that obfuscation is enabled in the release build configuration.}}
//      ^^^^^^^
        })
    })
})

// App: lambda within standalone parentheses (not parentheses of argument list)
(
    android {
        (
            buildTypes {
                (
                    release { // Noncompliant {{Make sure that obfuscation is enabled in the release build configuration.}}
//                  ^^^^^^^
                    }
                )
            }
        )

        defaultConfig {
            applicationId = AndroidConfig.ID // Android app, not a library
        }
    }
)

// App: different release properties than isDebuggable, isMinifyEnabled, and proguardFiles
android {
    defaultConfig {
        applicationId = "com.example" // Android app, not a library
    }
    buildTypes {
        release { // Noncompliant {{Make sure that obfuscation is enabled in the release build configuration.}}
            namespace = "com.example"
        }
    }
}

// App: proguardFiles under release, but not isMinifyEnabled
android {
    defaultConfig {
        applicationId = "com.example" // Android app, not a library
    }
    buildTypes {
        release { // Noncompliant {{Make sure that obfuscation is enabled in the release build configuration.}}
            proguardFiles("proguard-rules.pro")
        }
    }
}

// App: isMinifyEnabled under release, but not proguardFiles
android {
    defaultConfig {
        applicationId = "com.example" // Android app, not a library
    }
    buildTypes {
        release { // Noncompliant {{Make sure that obfuscation is enabled in the release build configuration.}}
            isMinifyEnabled = true
        }
    }
}

// App: android under kotlin block - proguardFiles under release, but not isMinifyEnabled
kotlin {
    android {
        defaultConfig {
            applicationId = "com.example" // Android app, not a library
        }
        buildTypes {
            release { // Noncompliant
                proguardFiles("proguard-rules.pro")
            }
        }
    }
}

// App: isMinifyEnabled read but not assigned - proguardFiles under release, but not isMinifyEnabled
kotlin {
    android {
        defaultConfig {
            applicationId = "com.example" // Android app, not a library
        }
        buildTypes {
            release { // Noncompliant
                print(isMinifyEnabled)
                proguardFiles("proguard-rules.pro")
            }
        }
    }
}

// App: isMinifyEnabled read in a binary operation which is not an assignnment
kotlin {
    android {
        defaultConfig {
            applicationId = "com.example" // Android app, not a library
        }
        buildTypes {
            release { // Noncompliant
                isMinifyEnabled && true
                true && isMinifyEnabled

                proguardFiles("proguard-rules.pro")
            }
        }
    }
}

// App: using setter
android {
    defaultConfig {
        applicationId = "com.example" // Android app, not a library
    }
    buildTypes {
        release { // Noncompliant
            setIsMinifiedEnable(false) // Not a valid setter
            setMinifiedEnable(false) // Not a valid setter

            proguardFiles("proguard-rules.pro")
        }
    }
}

// App: using isDebuggable
android {
    defaultConfig {
        applicationId = "com.example" // Android app, not a library
    }
    buildTypes {
        release { // Noncompliant {{Enabling debugging disables obfuscation for this release build. Make sure this is safe here.}}
            isMinifyEnabled = true
            isDebuggable = true
//         <^^^^^^^^^^^^^^^^^^^
            proguardFiles("proguard-rules.pro")
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
            isMinifyEnabled = false // Noncompliant
//          ^^^^^^^^^^^^^^^^^^^^^^^
            proguardFiles("proguard-rules.pro")
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
            isMinifyEnabled = false // Noncompliant
//          ^^^^^^^^^^^^^^^^^^^^^^^
            proguardFiles("proguard-rules.pro")
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
            isMinifyEnabled = false // Noncompliant
//          ^^^^^^^^^^^^^^^^^^^^^^^
            proguardFiles("proguard-rules.pro")
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
            isMinifyEnabled = false // Noncompliant
            proguardFiles("proguard-rules.pro")
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
            isMinifyEnabled = false // Noncompliant
            proguardFiles("proguard-rules.pro")
        }
    }
}

// App: using maybeCreate and apply
android {
    defaultConfig {
        applicationId = "com.example" // Android app, not a library
    }
    buildTypes {
        maybeCreate(BuildTypes.RELEASE).apply { // FN
            isMinifyEnabled = false
            proguardFiles("proguard-rules.pro")
        }
    }
}

// App: isMinifyEnabled assigned val
android {
    defaultConfig {
        applicationId = "com.example" // Android app, not a library
    }
    buildTypes {
        release {
            val falseVal = false
            isMinifyEnabled = falseVal // Noncompliant
            proguardFiles("proguard-rules.pro")
        }
    }
}

// App: isMinifyEnabled assigned var
android {
    defaultConfig {
        applicationId = "com.example" // Android app, not a library
    }
    buildTypes {
        release { // FN
            var falseVal = true
            isMinifyEnabled = falseVal
            proguardFiles("proguard-rules.pro")
        }
    }
}

// App: isMinifyEnabled assigned boolean expression
android {
    defaultConfig {
        applicationId = "com.example" // Android app, not a library
    }
    buildTypes {
        release { // FN
            isMinifyEnabled = false && true
            proguardFiles("proguard-rules.pro")
        }
    }
}

// App: foreach of platforms
listOf(android(), iosX64(), iosArm64()).forEach {
    it.defaultConfig {
        applicationId = "com.example" // Android app, not a library
    }
    it.buildTypes {
        release { // FN
            proguardFiles("proguard-rules.pro")
        }
    }
}

// App: lambda argument not a function literal
fun lambdaArgument(configure: BaseAppModuleExtension) = with(configure) {
    defaultConfig {
        applicationId = "com.example" // Android app, not a library
    }
    buildTypes {
        release { // FN
            namespace = "com.example"
            proguardFiles("proguard-rules.pro")
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
            isMinifyEnabled = true
        }
    }
}

// App: different block (not release) under buildTypes
android {
    defaultConfig {
        applicationId = "com.example" // Android app, not a library
    }
    buildTypes {
        debug {
            isMinifyEnabled = true
        }
    }
}

// App: android with isMinifiedEnabled set and proguardFiles, under kotlin block
kotlin {
    android {
        defaultConfig {
            applicationId = "com.example" // Android app, not a library
        }
        buildTypes {
            release {
                isMinifyEnabled = true
                proguardFiles("proguard-rules.pro")
            }
        }
    }
}

// App: isMinifiedEnabled set and empty proguardFiles
android {
    defaultConfig {
        applicationId = "com.example" // Android app, not a library
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles()
        }
    }
}

// App: isMinifiedEnabled set and proguardFiles called in nested block
android {
    defaultConfig {
        applicationId = "com.example" // Android app, not a library
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            someNestedBlock {
                proguardFiles("proguard-rules.pro")
            }
        }
    }
}

// App: proguardFiles called with multiple parameters, variable and not
android {
    defaultConfig {
        applicationId = "com.example" // Android app, not a library
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            val aValParameter = "proguard-rules-1.pro"
            var aVarParameter = "proguard-rules-2.pro"
            proguardFiles(aValParameter, aVarParameter, "proguard-rules-3.pro")
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
            isMinifyEnabled = false
        }
        getByName("debug") {
            isMinifyEnabled = false
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
            isMinifyEnabled = false
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
            isMinifyEnabled = false
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
            isMinifyEnabled = true
            isDebuggable = false
            proguardFiles("proguard-rules.pro")
        }
    }
}

// endregion
