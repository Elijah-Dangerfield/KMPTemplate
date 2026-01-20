plugins {
    id("goodtimes.feature")
}

android {
    namespace = "com.dangerfield.goodtimes.features.tasks"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.libraries.navigation)
            implementation(projects.libraries.core)
            implementation(projects.libraries.ui)
            api(projects.libraries.goodtimes)

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
        }
    }
}
