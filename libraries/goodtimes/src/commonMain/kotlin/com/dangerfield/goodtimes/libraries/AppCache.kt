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
 * Centralized user behavior tracking data.
 * 
 * This cache stores all user interactions and preferences that might:
 * - Impact the task recommendation algorithm
 * - Influence dynamic copy throughout the app
 * - Help the app feel more alive and personalized
 * 
 * Screen visits are tracked automatically via [screenVisits] map.
 * Any TrackableRoute will have its visits counted using its trackingKey.
 */
@Serializable
data class AppData(
    // Onboarding
    val hasUserOnboarded: Boolean = false,
    val onboardingNoClicks: Int = 0,
    
    // Mood tracking
    val hasEverAnsweredMood: Boolean = false,
    val moodBannerDisabled: Boolean = false,
    val moodBannerDismissCount: Int = 0,
    val moodBannerToggleCount: Int = 0,
    val lastMoodInteractionAt: Long? = null, // epoch millis - when user last answered or dismissed mood
    
    // Easter eggs
    val uselessButtonClicks: Int = 0,
    val secretOptionShown: Boolean = false,
    val secretOptionDismissed: Boolean = false,
    val madeWithLoveClicks: Int = 0,
    val madeWithLovePersistenceUnlocked: Boolean = false,
    val persistenceDialogOpens: Int = 0,
    val fakeSkipButtonClicked: Boolean = false,
    
    // Screen visits - automatically tracked for any TrackableRoute
    val screenVisits: Map<String, Int> = emptyMap(),
    
    // User actions
    val feedbacksGiven: Int = 0,
    val bugsReported: Int = 0,
    val freshStartsCompleted: Int = 0,
    
    // Tooltips
    val hasSeenPinchToZoomTooltip: Boolean = false,
) {
    /**
     * Get the visit count for a screen by its tracking key.
     * Returns 0 if the screen has never been visited.
     */
    fun getVisitCount(trackingKey: String): Int = screenVisits[trackingKey] ?: 0
    
    /**
     * Increment the visit count for a screen.
     * Returns a new AppData with the updated count.
     */
    fun incrementVisit(trackingKey: String): AppData = copy(
        screenVisits = screenVisits + (trackingKey to (getVisitCount(trackingKey) + 1))
    )
}

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