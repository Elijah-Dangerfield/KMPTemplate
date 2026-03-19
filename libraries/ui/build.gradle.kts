
plugins {
    id("kmptemplate.compose.multiplatform")
}

android {
    namespace = "com.kmptemplate.libraries.ui"
}

kotlin {
    sourceSets {

        androidMain.dependencies {
            api(compose.preview)
            api(compose.uiTooling)
        }

        commonMain.dependencies {
            implementation(projects.libraries.core)
            // TODO honestly the kmptemplate library should expose the component that require kmptemplate domain
            implementation(projects.libraries.kmptemplate)

            api(compose.ui)
            api(compose.uiUtil)
            api(compose.runtime)
            api(compose.foundation)
            api(compose.material3)
            api(compose.components.resources)
            api(compose.components.uiToolingPreview)
            api(compose.materialIconsExtended)
            api(compose.material3AdaptiveNavigationSuite)
            api(libs.compose.backhandler)

            api(libs.compottie)
            api(libs.compottie.resources)
            api(libs.compottie.dot)
            api(libs.compottie.lite)
            api(libs.compottie.network)
        }
    }
}