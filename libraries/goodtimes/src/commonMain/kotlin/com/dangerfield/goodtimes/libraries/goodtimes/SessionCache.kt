package com.dangerfield.goodtimes.libraries.goodtimes

import com.dangerfield.goodtimes.libraries.storage.Cache
import com.dangerfield.goodtimes.libraries.storage.CacheFactory
import com.dangerfield.goodtimes.libraries.storage.versionedJsonSerializer
import kotlinx.serialization.Serializable
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * Session-specific data only.
 * 
 * For user behavior tracking, preferences, and interaction counts,
 * use [AppCache] instead. This cache should only contain data
 * directly related to session management.
 */
@Serializable
data class SessionCacheData(
    val totalSessionCount: Int = 0,
)

interface SessionCache : Cache<SessionCacheData>

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = SessionCache::class)
@Inject
class SessionCacheImpl(
    cacheFactory: CacheFactory
) : SessionCache, Cache<SessionCacheData> by cacheFactory.persistent(
    name = "session_data",
    serializer = versionedJsonSerializer(
        defaultValue = { SessionCacheData() },
    )
)
