plugins {
    id("merizo.kotlin.multiplatform")
}

android {
    namespace = "com.dangerfield.merizo.libraries.core"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.datetime)
            implementation(libs.kotlin.inject.runtime.kmp)
        }
    }
}