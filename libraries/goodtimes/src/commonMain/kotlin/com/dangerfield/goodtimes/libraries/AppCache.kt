package com.dangerfield.goodtimes.libraries.goodtimes

import com.dangerfield.goodtimes.libraries.storage.Cache
import com.dangerfield.goodtimes.libraries.storage.CacheFactory
import com.dangerfield.goodtimes.libraries.storage.versionedJsonSerializer
import kotlinx.serialization.Serializable
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@Serializable
data class AppData(
    val hasUserOnboarded: Boolean = false,
    val numberOfTimesNoChecked: Int = 0
)

interface AppCache : Cache<AppData>

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = AppCache::class)
@Inject
class AppCacheImpl(
    cacheFactory: CacheFactory
) : AppCache, Cache<AppData> by cacheFactory.persistent(
    name = "app_data",
    serializer = versionedJsonSerializer(
        defaultValue = {
            AppData()
        },
    )
)