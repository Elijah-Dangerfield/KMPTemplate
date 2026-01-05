package com.dangerfield.goodtimes.libraries.goodtimes.impl

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import com.dangerfield.goodtimes.libraries.core.logging.KLog
import com.dangerfield.goodtimes.libraries.goodtimes.Permission
import com.dangerfield.goodtimes.libraries.goodtimes.PermissionManager
import com.dangerfield.goodtimes.libraries.goodtimes.PermissionResult
import com.dangerfield.goodtimes.libraries.goodtimes.PermissionStatus
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

@Inject
@ContributesBinding(AppScope::class)
class AndroidPermissionManager(private val context: Context) : PermissionManager {

    override suspend fun ensurePermission(permission: Permission): PermissionResult {
        val currentStatus = checkPermissionStatus(permission)

        if (currentStatus == PermissionStatus.GRANTED) {
            return PermissionResult.Granted
        }

        return requestPermission(permission)
    }

    override suspend fun requestPermission(permission: Permission): PermissionResult {
        return when (permission) {
            Permission.AppUsageStats -> requestUsageStatsPermission()
            Permission.Notifications -> requestNotificationsPermission()
            Permission.Camera -> requestCameraPermission()
            Permission.PhotoLibrary -> requestPhotoLibraryPermission()
        }
    }

    override fun checkPermissionStatus(permission: Permission): PermissionStatus {
        return when (permission) {
            Permission.AppUsageStats -> checkUsageStatsPermission()
            Permission.Notifications -> checkNotificationsPermission()
            Permission.Camera -> checkCameraPermission()
            Permission.PhotoLibrary -> checkPhotoLibraryPermission()
        }
    }

    private fun checkUsageStatsPermission(): PermissionStatus {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }

            if (mode == AppOpsManager.MODE_ALLOWED) {
                PermissionStatus.GRANTED
            } else {
                PermissionStatus.DENIED
            }
        } catch (e: Exception) {
            KLog.e("Error checking usage stats permission", e)
            PermissionStatus.NOT_DETERMINED
        }
    }

    private fun requestUsageStatsPermission(): PermissionResult {
        return try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                data = Uri.parse("package:${context.packageName}")
            }

            context.startActivity(intent)
            KLog.i("Opened usage stats settings")

            PermissionResult.Denied(canRequestAgain = true)
        } catch (e: Exception) {
            KLog.e("Error requesting usage stats permission", e)
            try {
                val fallbackIntent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
                PermissionResult.Denied(canRequestAgain = true)
            } catch (e2: Exception) {
                KLog.e("Failed to open settings", e2)
                PermissionResult.Denied(canRequestAgain = false)
            }
        }
    }

    private fun checkNotificationsPermission(): PermissionStatus {
        return PermissionStatus.NOT_DETERMINED
    }

    private fun requestNotificationsPermission(): PermissionResult {
        return PermissionResult.Denied(canRequestAgain = true)
    }

    private fun checkCameraPermission(): PermissionStatus {
        val granted = context.checkSelfPermission(Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        return if (granted) PermissionStatus.GRANTED else PermissionStatus.DENIED
    }

    private fun requestCameraPermission(): PermissionResult {
        return PermissionResult.Denied(canRequestAgain = true)
    }

    private fun checkPhotoLibraryPermission(): PermissionStatus {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        val granted = context.checkSelfPermission(permission) ==
            PackageManager.PERMISSION_GRANTED
        return if (granted) PermissionStatus.GRANTED else PermissionStatus.DENIED
    }

    private fun requestPhotoLibraryPermission(): PermissionResult {
        return PermissionResult.Denied(canRequestAgain = true)
    }
}