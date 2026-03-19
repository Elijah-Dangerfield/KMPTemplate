package com.kmptemplate.libraries.kmptemplate

interface PermissionManager {
    suspend fun ensurePermission(permission: Permission): PermissionResult
    suspend fun requestPermission(permission: Permission): PermissionResult
    fun checkPermissionStatus(permission: Permission): PermissionStatus
    fun openAppSettings()
}
