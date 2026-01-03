plugins {
    id("merizo.kotlin.multiplatform")
    alias(libs.plugins.sentryKmp)
}

moduleConfig {
    optIn("kotlin.time.ExperimentalTime")
    optIn("kotlin.uuid.ExperimentalUuidApi")
    optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
}

android {
    namespace = "com.dangerfield.merizo.libraries.merizo.impl"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.libraries.merizo)

            implementation(projects.libraries.core)
            implementation(libs.kermit)
            implementation(projects.libraries.flowroutines)
            implementation(projects.libraries.merizo.storage)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}