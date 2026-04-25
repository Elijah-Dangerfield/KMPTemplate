plugins {
    id("kmptemplate.kotlin.multiplatform")
}

android {
    namespace = "com.kmptemplate.libraries.networking"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.ktor.client.core)
            api(libs.kotlinx.serialization.json)

            implementation(projects.libraries.core)
        }
    }
}
