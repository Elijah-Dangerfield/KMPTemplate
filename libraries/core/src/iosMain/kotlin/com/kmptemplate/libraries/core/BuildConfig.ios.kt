package com.kmptemplate.libraries.core

import com.kmptemplate.buildinfo.KMPTemplateBuildConfig
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform as NativePlatform

@OptIn(ExperimentalNativeApi::class)
actual object BuildInfo {
    actual val isDebug: Boolean
        get() = NativePlatform.isDebugBinary

    actual val platform: Platform = Platform.iOS

    actual val applicationId: String
        get() = KMPTemplateBuildConfig.APPLICATION_ID

    actual val versionName: String
        get() = KMPTemplateBuildConfig.VERSION_NAME

    actual val versionCode: Int
        get() = KMPTemplateBuildConfig.VERSION_CODE

    actual val releaseChannel: String
        get() = KMPTemplateBuildConfig.RELEASE_CHANNEL

    actual val buildNumber: Int
        get() = KMPTemplateBuildConfig.BUILD_NUMBER
}