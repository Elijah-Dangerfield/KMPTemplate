package com.kmptemplate.libraries.core

import com.kmptemplate.buildinfo.KMPTemplateBuildConfig
import com.kmptemplate.libraries.core.BuildConfig as AndroidBuildConfig

actual object BuildInfo {
    actual val isDebug: Boolean
        get() = AndroidBuildConfig.DEBUG

    actual val platform: Platform
        get() = Platform.Android

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