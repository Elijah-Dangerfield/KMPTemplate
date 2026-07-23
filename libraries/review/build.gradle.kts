plugins {
    id("kmptemplate.kotlin.multiplatform")
}

moduleConfig {
    serialization()
}

android {
    namespace = "com.kmptemplate.libraries.review"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.libraries.core)
            api(libs.kotlinx.coroutines.core)
        }
    }
}
