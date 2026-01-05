package com.dangerfield.goodtimes.libraries.core

import com.dangerfield.goodtimes.buildinfo.GoodtimesBuildConfig
import com.dangerfield.goodtimes.libraries.core.BuildConfig as AndroidBuildConfig

actual object BuildInfo {
    actual val isDebug: Boolean
        get() = AndroidBuildConfig.DEBUG

    actual val platform: Platform
        get() = Platform.Android

    actual val applicationId: String
        get() = GoodtimesBuildConfig.APPLICATION_ID

    actual val versionName: String
        get() = GoodtimesBuildConfig.VERSION_NAME

    actual val versionCode: Int
        get() = GoodtimesBuildConfig.VERSION_CODE

    actual val releaseChannel: String
        get() = GoodtimesBuildConfig.RELEASE_CHANNEL

    actual val buildNumber: Int
        get() = GoodtimesBuildConfig.BUILD_NUMBER
}