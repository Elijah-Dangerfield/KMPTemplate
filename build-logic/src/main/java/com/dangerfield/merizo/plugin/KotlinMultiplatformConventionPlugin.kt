package com.dangerfield.merizo.plugin

import com.android.build.gradle.LibraryExtension
import com.dangerfield.merizo.ext.ConfigurationExtension
import com.dangerfield.merizo.util.configureAndroid
import com.dangerfield.merizo.util.configureKotlinMultiplatform
import com.dangerfield.merizo.util.configureKotlinInject
import com.dangerfield.merizo.util.libs
import com.dangerfield.merizo.util.loadSupabaseMetadata
import com.dangerfield.merizo.util.loadVersionMetadata
import com.dangerfield.merizo.util.optInKotlinMarkers
import com.dangerfield.merizo.util.VersionMetadata
import com.dangerfield.merizo.util.writeCommonMetadata
import com.dangerfield.merizo.util.writeSupabaseMetadata
import com.github.gmazzo.buildconfig.BuildConfigExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.internal.config.AnalysisFlags.optIn

/**
 * Convention plugin for pure Kotlin multiplatform library modules without UI dependencies.
 * 
 * **When to use this plugin:**
 * - Pure Kotlin libraries that contain business logic, utilities, or data models
 * - Libraries that provide platform abstractions (storage, networking, etc.)
 * - Modules that don't need Compose or any UI dependencies
 * - Core libraries that other modules depend on
 * - Data layer modules (repositories, data sources, models)
 * 
 * **What this plugin provides:**
 * - Kotlin Multiplatform setup (Android, iOS, JVM targets)
 * - Android library configuration
 * - Common KMP dependencies (coroutines, kotlin-test)
 * - No UI or Compose dependencies (keeps modules lean)
 * 
 * **Examples of modules that should use this:**
 * - libraries:core (your core utilities and abstractions)
 * - libraries:database (database abstractions and implementations)
 * - libraries:networking (API clients and network utilities)
 * - libraries:storage (storage abstractions)
 * - libraries:user (user domain models and business logic)
 * 
 * **Don't use this plugin for:**
 * - Modules that need Compose UI (use merizo.compose.multiplatform instead)
 * - Feature modules with screens (use merizo.feature instead)
 * - The main application module (use merizo.android.application instead)
 */
class KotlinMultiplatformConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.multiplatform")
                apply("com.android.library")
                apply(libs.plugins.kotlinSerialization.get().pluginId)
            }

            configureKotlinMultiplatform()
            configureKotlinInject()

            project.optInKotlinMarkers("kotlin.time.ExperimentalTime")
            project.optInKotlinMarkers("kotlin.uuid.ExperimentalUuidApi")

            extensions.configure<LibraryExtension> {
                configureAndroid()
            }

            (extensions.findByName("moduleConfig") as? ConfigurationExtension)
                ?: extensions.create("moduleConfig", ConfigurationExtension::class.java)

            if (path == ":libraries:core") {
                pluginManager.apply(libs.plugins.buildconfig.get().pluginId)
                configureSharedBuildConfig(loadVersionMetadata())
            }
        }
    }

    private fun Project.configureSharedBuildConfig(metadata: VersionMetadata) {
        val supabaseMetadata = loadSupabaseMetadata()
        extensions.configure(BuildConfigExtension::class.java) {
            packageName("com.dangerfield.merizo.buildinfo")
            className("MerizoBuildConfig")
            useKotlinOutput {
                internalVisibility = false
            }
            writeCommonMetadata(metadata)
            writeSupabaseMetadata(supabaseMetadata)
        }
    }
}