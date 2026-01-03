import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

group = "com.dangerfield.merizo.buildlogic"

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
    api(libs.kotlinx.serialization.gradlePlugin)
    api(libs.buildconfig.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("kotlinMultiplatform") {
            id = "merizo.kotlin.multiplatform"
            implementationClass = "com.dangerfield.merizo.plugin.KotlinMultiplatformConventionPlugin"
        }
        register("composeMultiplatform") {
            id = "merizo.compose.multiplatform"
            implementationClass = "com.dangerfield.merizo.plugin.ComposeMultiplatformConventionPlugin"
        }
        register("feature") {
            id = "merizo.feature"
            implementationClass = "com.dangerfield.merizo.plugin.FeatureConventionPlugin"
        }
        register("application") {
            id = "merizo.application"
            implementationClass = "com.dangerfield.merizo.plugin.ApplicationConventionPlugin"
        }
    }
}