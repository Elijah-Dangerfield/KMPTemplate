plugins {
    id("kmptemplate.kotlin.multiplatform")
    alias(libs.plugins.sentryKmp)
}

moduleConfig {
    optIn("kotlin.time.ExperimentalTime")
    optIn("kotlin.uuid.ExperimentalUuidApi")
    optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
}

android {
    namespace = "com.kmptemplate.libraries.kmptemplate.impl"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.libraries.kmptemplate)

            implementation(projects.libraries.core)
            implementation(libs.kermit)
            implementation(projects.libraries.flowroutines)
            implementation(projects.libraries.networking)
            implementation(projects.libraries.identity)
            implementation(projects.libraries.kmptemplate.storage)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
        }

        commonTest.dependencies {
            implementation(projects.libraries.kmptemplate)
            implementation(projects.libraries.networking)
            implementation(projects.libraries.identity)
            // :libraries:core for AutoInit (AppEventDispatcher's supertype —
            // the test compiler has to load it to type-check the class).
            implementation(projects.libraries.core)
            implementation(projects.libraries.flowroutines.testing)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.google.play.review)
            implementation(libs.google.play.review.ktx)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}
