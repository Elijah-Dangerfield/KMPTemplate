plugins {
    id("kmptemplate.compose.multiplatform")
    alias(libs.plugins.kotlinSerialization)
}

android {
    namespace = "com.kmptemplate.libraries.navigation"

    // FloatingWindowHostTest drives a real composition on the host JVM, and Compose's runtime
    // calls android.os.Trace when it disposes one. Left unstubbed, that kills the test in teardown.
    testOptions.unitTests.isReturnDefaultValues = true
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.libraries.core)
            implementation(projects.libraries.ui)
            implementation(projects.libraries.flowroutines)
            api(libs.jetbrains.navigation.compose)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}