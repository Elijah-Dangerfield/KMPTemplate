plugins {
    id("goodtimes.kotlin.multiplatform")
}

android {
    namespace = "com.dangerfield.goodtimes.libraries.config"
}


kotlin {
    sourceSets {
        commonMain.dependencies {
            
            implementation(projects.libraries.core)
            implementation(projects.libraries.flowroutines)
            implementation(libs.kotlin.inject.runtime.kmp)
        }
    }
}