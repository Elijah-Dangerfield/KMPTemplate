plugins {
    id("goodtimes.kotlin.multiplatform")
}

android {
    namespace = "com.dangerfield.goodtimes.libraries.goodtimes.storage"
}

moduleConfig.storage()


kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.libraries.goodtimes)
            implementation(projects.libraries.core)
            implementation(projects.libraries.flowroutines)
            implementation(projects.libraries.storage)
        }
    }
}