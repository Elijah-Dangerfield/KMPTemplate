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
 * User profile data that we learn over time.
 * 
 * This cache stores information about the user that they've told us
 * or that we've inferred from their behavior. As we learn more,
 * this data will grow.
 */
@Serializable
data class UserData(
    // Basic info
    val name: String? = null,
    
    // Personality traits inferred from behavior
    // TODO: Add more as we learn about the user
    // - curiosity level (based on exploration behavior)
    // - persistence (based on useless button, etc.)
    // - mood patterns
)

interface UserCache : Cache<UserData>

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = UserCache::class)
@Inject
class UserCacheImpl(
    cacheFactory: CacheFactory
) : UserCache, Cache<UserData> by cacheFactory.persistent(
    name = "user_data",
    serializer = versionedJsonSerializer(
        defaultValue = {
            UserData()
        },
    )
)
