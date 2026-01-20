package com.dangerfield.goodtimes.libraries.goodtimes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SessionRepository {
    val currentSession: StateFlow<Session?>
    
    val moodBannerDisabled: Flow<Boolean>
    
    val moodBannerDismissCount: Flow<Int>
    
    val moodBannerToggleCount: Flow<Int>
    
    val hasEverAnsweredMood: Flow<Boolean>
    
    /** Epoch millis when user last answered or dismissed mood prompt */
    val lastMoodInteractionAt: Flow<Long?>
    
    fun setMood(mood: Mood)
    
    fun dismissMood()
    
    fun disableMoodBannerPermanently()
    
    fun enableMoodBanner()
}
