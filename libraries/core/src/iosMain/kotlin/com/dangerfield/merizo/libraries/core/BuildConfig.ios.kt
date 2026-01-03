package com.dangerfield.merizo.libraries.core

import com.dangerfield.merizo.buildinfo.MerizoBuildConfig
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform as NativePlatform

@OptIn(ExperimentalNativeApi::class)
actual object BuildInfo {
    actual val isDebug: Boolean
        get() = NativePlatform.isDebugBinary

    actual val platform: Platform = Platform.iOS

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