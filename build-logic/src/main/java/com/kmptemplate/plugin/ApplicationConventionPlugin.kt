package com.kmptemplate.plugin

import com.android.build.api.dsl.ApplicationExtension
import com.kmptemplate.ext.ConfigurationExtension
import com.kmptemplate.util.SharedConstants
import com.kmptemplate.util.configureAndroid
import com.kmptemplate.util.configureKotlinInject
import com.kmptemplate.util.configureKotlinMultiplatform
import com.kmptemplate.util.libs
import com.kmptemplate.util.loadSupabaseMetadata
import com.kmptemplate.util.loadVersionMetadata
import com.kmptemplate.util.optInKotlinMarkers
import com.kmptemplate.util.VersionMetadata
import com.kmptemplate.util.writeCommonMetadata
import com.kmptemplate.util.writeSupabaseMetadata
import com.github.gmazzo.buildconfig.BuildConfigExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Convention plugin for the main Android application module.
 *
 * **When to use this plugin:**
 * - The main app module that gets installed on devices
 * - The module that contains MainActivity and app-level configuration
 * - The module that defines the applicationId and app metadata
 *
 * **What this plugin provides:**
 * - Android application plugin configuration
 * - Kotlin Multiplatform setup with Android and iOS targets
 * - Compose and Compose Compiler plugins
 * - iOS framework configuration for KMP
 * - Application-specific build configuration (version codes, signing, etc.)
 * - Activity Compose dependencies
 *
 * **Examples of modules that should use this:**
 * - apps:compose (your main app)
 * - apps:desktop (if you have a desktop app variant)
 *
 * **Don't use this plugin for:**
 * - Feature modules (use kmptemplate.feature instead)
 * - Library modules (use kmptemplate.compose.multiplatform or kmptemplate.kotlin.multiplatform)
 * - Server modules (these wouldn't be Android applications)
 */
class ApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val versionMetadata = loadVersionMetadata()
            with(pluginManager) {
                apply("org.jetbrains.kotlin.multiplatform")
                apply("com.android.application")
                apply("org.jetbrains.compose")
                apply("org.jetbrains.kotlin.plugin.compose")
                apply(libs.plugins.kotlinSerialization.get().pluginId)
                apply(libs.plugins.buildconfig.get().pluginId)
            }

            project.optInKotlinMarkers("kotlin.time.ExperimentalTime")
            project.optInKotlinMarkers("kotlin.uuid.ExperimentalUuidApi")

            configureKotlinMultiplatform {
                binaries.framework {
                    baseName = "ComposeApp"
                    isStatic = true
                    binaryOption("bundleId", "com.kmptemplate")
                    export(project(":libraries:core"))
                }
            }
            configureKotlinInject()

            extensions.configure<ApplicationExtension> {
                configureAndroid()

                defaultConfig {
                    applicationId = versionMetadata.applicationId
                    targetSdk = SharedConstants.targetSdk
                    versionCode = versionMetadata.versionCode
                    versionName = versionMetadata.versionName
                }

                packaging {
                    resources {
                        excludes += "/META-INF/{AL2.0,LGPL2.1}"
                    }
                }

                buildTypes {
                    debug {
                        applicationIdSuffix = ".debug"
                    }
                    release {
                        isMinifyEnabled = false
                    }
                }
            }

            if (extensions.findByName("moduleConfig") == null) {
                extensions.create("moduleConfig", ConfigurationExtension::class.java)
            }
            configureAppBuildConfig(versionMetadata)
        }
    }

    private fun Project.configureAppBuildConfig(metadata: VersionMetadata) {
        val supabaseMetadata = loadSupabaseMetadata()
        extensions.configure(BuildConfigExtension::class.java) {
            packageName("${metadata.applicationId}.appconfig")
            className("AppBuildConfig")
            useKotlinOutput {
                internalVisibility = false
            }
            writeCommonMetadata(metadata)
            writeSupabaseMetadata(supabaseMetadata)
        }
    }
}