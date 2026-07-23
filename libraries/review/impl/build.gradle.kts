plugins {
    id("kmptemplate.kotlin.multiplatform")
}

moduleConfig {
    di()
}

android {
    namespace = "com.kmptemplate.libraries.review.impl"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.libraries.review)
            implementation(projects.libraries.core)
            implementation(projects.libraries.kmptemplate)
            implementation(projects.libraries.flowroutines)
            implementation(projects.libraries.storage)
        }
        commonTest.dependencies {
            implementation(projects.libraries.flowroutines.testing)
            implementation(projects.libraries.review)
            implementation(projects.libraries.kmptemplate)
            implementation(projects.libraries.storage)
        }

        androidMain.dependencies {
            implementation(libs.google.play.review)
            implementation(libs.google.play.review.ktx)
        }
    }
}
