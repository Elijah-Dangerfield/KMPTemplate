plugins {
    id("goodtimes.feature")
}

android {
    namespace = "com.dangerfield.goodtimes.features.tasks.impl"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.features.tasks)
            implementation(projects.libraries.navigation)
            implementation(projects.libraries.core)
            implementation(projects.libraries.flowroutines)
            implementation(projects.libraries.ui)
            implementation(projects.libraries.goodtimes)
            implementation(projects.libraries.storage)

            implementation(libs.kotlinx.datetime)

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
        }
    }
}
