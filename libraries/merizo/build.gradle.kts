plugins {
    id("merizo.kotlin.multiplatform")
}

moduleConfig {
    optIn("kotlin.time.ExperimentalTime")
    serialization()
}

android {
    namespace = "com.dangerfield.merizo.libraries.merizo"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.libraries.core)
            implementation(projects.libraries.flowroutines)
            api(projects.libraries.storage)
            implementation(libs.configuration.annotations)
            implementation(libs.kotlinx.serialization.json)
        }

        androidMain.dependencies {
            implementation(libs.androidx.lifecycle.process)
        }
    }
}