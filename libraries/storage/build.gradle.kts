plugins {
    id("merizo.kotlin.multiplatform")
    alias(libs.plugins.androidxRoom)
}

android {
    namespace = "com.dangerfield.merizo.libraries.storage"
}

room {
    schemaDirectory("$projectDir/schemas")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.androidx.datastore.core)
            api(libs.androidx.datastore.core.okio)
            api(libs.androidx.datastore.preferences)

            implementation(projects.libraries.core)
            implementation(projects.libraries.flowroutines)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}