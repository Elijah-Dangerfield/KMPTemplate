plugins {
    id("goodtimes.application")
    id("co.touchlab.skie") version "0.10.8"

}

android {
    namespace = "com.dangerfield.goodtimes"
}

kotlin {

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.work.runtime)
            implementation(compose.uiTooling)
        }

        commonMain.dependencies {
            // Project dependencies
            api(projects.libraries.core)
            implementation(projects.libraries.ui)
            implementation(projects.libraries.goodtimes)
            implementation(projects.libraries.goodtimes.impl)
            implementation(projects.libraries.flowroutines)
            implementation(projects.libraries.navigation)
            implementation(projects.libraries.navigation.impl)
            implementation(projects.libraries.resources)

            implementation(projects.libraries.storage)
            implementation(projects.libraries.storage.impl)
            implementation(projects.libraries.goodtimes.storage)
            implementation(projects.libraries.config)
            implementation(projects.libraries.config.impl)
            implementation(projects.libraries.goodtimes.storage)

            implementation(projects.features.home)
            implementation(projects.features.home.impl)
            implementation(projects.features.onboarding)
            implementation(projects.features.onboarding.impl)
            implementation(projects.features.tasks)
            implementation(projects.features.tasks.impl)

            implementation(libs.atomicfu)
            
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
        }
    }
}