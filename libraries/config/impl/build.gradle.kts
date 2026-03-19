plugins {
    id("kmptemplate.kotlin.multiplatform")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

moduleConfig {
    di()
    serialization()
}

android {
    namespace = "com.kmptemplate.libraries.config.impl"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.libraries.config)
            implementation(projects.libraries.core)
            implementation(projects.libraries.flowroutines)
            implementation(projects.libraries.storage)
            implementation(libs.kotlinx.serialization.json)
            implementation(compose.components.resources)
        }
    }
}

compose.resources {
    publicResClass = false
}