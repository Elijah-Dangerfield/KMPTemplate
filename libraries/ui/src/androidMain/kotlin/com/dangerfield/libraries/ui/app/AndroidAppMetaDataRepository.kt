package com.dangerfield.libraries.ui.app

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.dangerfield.merizo.libraries.merizo.PlatformApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.LinkedHashMap

class AndroidAppMetaDataRepository(
    private val context: Context
) : AppMetaDataRepository {

    private val cache = LinkedHashMap<String, AppMetaData>(64, 0.75f, true)

    override suspend fun getMetaData(selection: PlatformApp): AppMetaData {
        synchronized(cache) {
            cache[selection.id]?.let { return it }
        }

        val meta = when (selection) {
            is PlatformApp.AndroidPlatformApp -> loadFromPackage(selection)
            else -> fallback(selection)
        }

        synchronized(cache) {
            cache[selection.id] = meta
            if (cache.size > 128) {
                val iterator = cache.entries.iterator()
                if (iterator.hasNext()) {
                    iterator.next()
                    iterator.remove()
                }
            }
        }
        return meta
    }

    private suspend fun loadFromPackage(selection: PlatformApp.AndroidPlatformApp): AppMetaData = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val label = runCatching {
            val applicationInfo = pm.getApplicationInfo(selection.packageName, 0)
            pm.getApplicationLabel(applicationInfo)?.toString()
        }.getOrNull()

        val iconBitmap: ImageBitmap? = runCatching {
            pm.getApplicationIcon(selection.packageName)
                .toBitmap()
                .asImageBitmap()
        }.getOrNull()

        AppMetaData(
            displayName = label ?: selection.displayName ?: selection.id,
            icon = iconBitmap
        )
    }

    private fun fallback(selection: PlatformApp): AppMetaData {
        return AppMetaData(selection.displayName ?: selection.id)
    }
}
