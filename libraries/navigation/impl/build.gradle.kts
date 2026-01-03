plugins {
    id("merizo.compose.multiplatform")
    alias(libs.plugins.kotlinSerialization)
}

android {
    namespace = "com.dangerfield.merizo.libraries.navigation.impl"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.libraries.core)
            implementation(projects.libraries.navigation)
            implementation(projects.libraries.ui)
            implementation(projects.libraries.flowroutines)
            api(libs.jetbrains.navigation.compose)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}