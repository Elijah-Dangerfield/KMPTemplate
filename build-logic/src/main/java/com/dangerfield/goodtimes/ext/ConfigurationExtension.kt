package com.dangerfield.goodtimes.ext

import com.dangerfield.goodtimes.util.configureKotlinInject
import com.dangerfield.goodtimes.util.getModule
import com.dangerfield.goodtimes.util.libs
import com.dangerfield.goodtimes.util.optInKotlinMarkers
import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import javax.inject.Inject

@ExtDsl
abstract class ConfigurationExtension {
    @get:Inject
    internal abstract val project: Project

    fun optIn(vararg markerClasses: String) {
        project.optInKotlinMarkers(*markerClasses)
    }

    fun storage() {
        serialization()
        project.dependencies {
            add("implementation", getModule("libraries:storage"))
        }
    }


    // Most modules will get di() from the convention plugin. But if needed this is there.
    fun di() {
        project.extensions.configure(KotlinMultiplatformExtension::class.java) {
            project.configureKotlinInject()
        }
    }


    fun ksp(configure: KspExtension.() -> Unit = {}) {
        project.pluginManager.apply("com.google.devtools.ksp")
        project.extensions.configure(configure)
    }

    fun compose() {

    }


    fun serialization() {
        project.dependencies {
            add("implementation", project.libs.kotlinx.serialization.json)
        }
    }

    fun networking() {
    }
}
