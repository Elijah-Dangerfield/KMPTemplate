plugins {
    id("merizo.compose.multiplatform")
    alias(libs.plugins.kotlinSerialization)
}

android {
    namespace = "com.dangerfield.merizo.libraries.navigation"
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
    }
}