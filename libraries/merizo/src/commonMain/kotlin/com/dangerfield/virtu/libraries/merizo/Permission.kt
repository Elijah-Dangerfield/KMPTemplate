package com.dangerfield.merizo.libraries.merizo

sealed class Permission {
    data object AppUsageStats : Permission()
    data object Notifications : Permission()
}

enum class PermissionStatus {
    GRANTED, DENIED, NOT_DETERMINED
}

sealed class PermissionResult {
    data object Granted : PermissionResult()
    data class Denied(val canRequestAgain: Boolean) : PermissionResult()
}