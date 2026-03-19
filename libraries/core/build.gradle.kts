plugins {
    id("kmptemplate.kotlin.multiplatform")
}

android {
    namespace = "com.kmptemplate.libraries.core"
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