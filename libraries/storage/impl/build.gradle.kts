plugins {
    id("kmptemplate.kotlin.multiplatform")
}

android {
    namespace = "com.kmptemplate.libraries.storage.impl"
}

moduleConfig.storage()

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.libraries.storage)

            implementation(projects.libraries.core)
            implementation(projects.libraries.flowroutines)
            implementation(projects.libraries.kmptemplate)
            implementation(projects.libraries.kmptemplate.storage)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}

tasks.matching { it.name.contains("kspCommonMainKotlinMetadata", ignoreCase = true) }
    .configureEach { enabled = false }