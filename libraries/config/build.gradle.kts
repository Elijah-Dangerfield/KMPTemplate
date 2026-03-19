plugins {
    id("kmptemplate.kotlin.multiplatform")
}

android {
    namespace = "com.kmptemplate.libraries.config"
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