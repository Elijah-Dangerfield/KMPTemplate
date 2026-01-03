plugins {
    id("merizo.kotlin.multiplatform")
}

android {
    namespace = "com.dangerfield.merizo.libraries.merizo.storage"
}

moduleConfig.storage()


kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.libraries.merizo)
            implementation(projects.libraries.core)
            implementation(projects.libraries.flowroutines)
            implementation(projects.libraries.storage)
        }
    }
}