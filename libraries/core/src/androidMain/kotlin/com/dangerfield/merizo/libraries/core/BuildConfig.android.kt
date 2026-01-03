package com.dangerfield.merizo.libraries.core

import com.dangerfield.merizo.buildinfo.MerizoBuildConfig
import com.dangerfield.merizo.libraries.core.BuildConfig as AndroidBuildConfig

actual object BuildInfo {
    actual val isDebug: Boolean
        get() = AndroidBuildConfig.DEBUG

    actual val platform: Platform
        get() = Platform.Android

    actual val applicationId: String
        get() = MerizoBuildConfig.APPLICATION_ID

    actual val versionName: String
        get() = MerizoBuildConfig.VERSION_NAME

    actual val versionCode: Int
        get() = MerizoBuildConfig.VERSION_CODE

    actual val releaseChannel: String
        get() = MerizoBuildConfig.RELEASE_CHANNEL

    actual val buildNumber: Int
        get() = MerizoBuildConfig.BUILD_NUMBER
}