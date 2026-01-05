plugins {
    id("goodtimes.kotlin.multiplatform")
}

moduleConfig {
    optIn("kotlin.time.ExperimentalTime")
    serialization()
}

android {
    namespace = "com.dangerfield.goodtimes.libraries.goodtimes"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.libraries.core)
            implementation(projects.libraries.flowroutines)
            api(projects.libraries.storage)
            implementation(libs.configuration.annotations)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
        }

        androidMain.dependencies {
            implementation(libs.androidx.lifecycle.process)
        }
    }
}