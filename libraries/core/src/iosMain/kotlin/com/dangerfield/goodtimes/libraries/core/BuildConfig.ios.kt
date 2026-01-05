package com.dangerfield.goodtimes.libraries.core

import com.dangerfield.goodtimes.buildinfo.GoodtimesBuildConfig
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform as NativePlatform

@OptIn(ExperimentalNativeApi::class)
actual object BuildInfo {
    actual val isDebug: Boolean
        get() = NativePlatform.isDebugBinary

    actual val platform: Platform = Platform.iOS

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