package com.kmptemplate.libraries.kmptemplate

sealed class Permission {
    data object AppUsageStats : Permission()
    data object Notifications : Permission()
    data object Camera : Permission()
    data object PhotoLibrary : Permission()
    data object Microphone : Permission()
}

enum class PermissionStatus {
    GRANTED, DENIED, NOT_DETERMINED
}

sealed class PermissionResult {
    data object Granted : PermissionResult()
    data class Denied(val canRequestAgain: Boolean) : PermissionResult()
}