plugins {
    id("merizo.kotlin.multiplatform")
}

android {
    namespace = "com.dangerfield.merizo.libraries.storage.impl"
}

moduleConfig.storage()

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.libraries.storage)

            implementation(projects.libraries.core)
            implementation(projects.libraries.flowroutines)
            implementation(projects.libraries.merizo)
            implementation(projects.libraries.merizo.storage)
            implementation(projects.libraries.merizo.storage)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}

tasks.matching { it.name.contains("kspCommonMainKotlinMetadata", ignoreCase = true) }
    .configureEach { enabled = false }