plugins {
    id("kmptemplate.kotlin.multiplatform")
}

android {
    namespace = "com.kmptemplate.libraries.kmptemplate.storage"
}

moduleConfig.storage()


kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.libraries.kmptemplate)
            implementation(projects.libraries.core)
            implementation(projects.libraries.flowroutines)
            implementation(projects.libraries.storage)
        }
    }
}