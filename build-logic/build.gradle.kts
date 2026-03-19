import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

group = "com.kmptemplate.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
}

repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
    api(libs.ksp.gradlePlugin)
    api(libs.androidx.room.gradlePlugin)
    api(libs.kotlinx.serialization.gradlePlugin)
    api(libs.buildconfig.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("kotlinMultiplatform") {
            id = "kmptemplate.kotlin.multiplatform"
            implementationClass = "com.kmptemplate.plugin.KotlinMultiplatformConventionPlugin"
        }
        register("composeMultiplatform") {
            id = "kmptemplate.compose.multiplatform"
            implementationClass = "com.kmptemplate.plugin.ComposeMultiplatformConventionPlugin"
        }
        register("feature") {
            id = "kmptemplate.feature"
            implementationClass = "com.kmptemplate.plugin.FeatureConventionPlugin"
        }
        register("application") {
            id = "kmptemplate.application"
            implementationClass = "com.kmptemplate.plugin.ApplicationConventionPlugin"
        }
    }
}