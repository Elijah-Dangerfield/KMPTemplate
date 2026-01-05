plugins {
    id("goodtimes.compose.multiplatform")
}

moduleConfig {
    di()
}

android {
    namespace = "com.dangerfield.goodtimes.libraries.flowroutines"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)

            implementation(projects.libraries.core)

            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.viewmodel.savedstate)
            
            // Compose dependencies
            api(compose.runtime)
            api(compose.ui)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
        }
    }
}
