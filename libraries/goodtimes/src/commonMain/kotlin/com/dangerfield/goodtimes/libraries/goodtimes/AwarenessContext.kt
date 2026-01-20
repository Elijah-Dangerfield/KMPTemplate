package com.dangerfield.goodtimes.libraries.goodtimes

import kotlin.time.Duration

/**
 * Unified context for all app "awareness" - everything the app knows about
 * the current moment, the user, and their history.
 * 
 * This is the single source of truth used by:
 * - Copy generation (making text feel alive)
 * - Task selection (GetNextTaskUseCase)
 * - Mood prompt decisions
 * - Any "smart" behavior
 * 
 * Organized into sub-contexts by lifecycle:
 * - [time] - Ephemeral, changes moment to moment
 * - [device] - Ephemeral, current device state
 * - [session] - Semi-ephemeral, changes per session
 * - [personality] - Persistent, learned user traits
 * - [mood] - Computed from recent history
 */
data class AwarenessContext(
    val time: TimeContext,
    val device: DeviceContext,
    val session: SessionContext,
    val personality: PersonalityContext,
    val mood: MoodContext,
) {
    // =========================================================================
    // CONVENIENCE ACCESSORS
    // These let call sites use ctx.isLateNight instead of ctx.time.isLateNight
    // =========================================================================
    
    // Time shortcuts
    val isLateNight: Boolean get() = time.isLateNight
    val isEarlyMorning: Boolean get() = time.isEarlyMorning
    val isMorning: Boolean get() = time.isMorning
    val isAfternoon: Boolean get() = time.isAfternoon
    val isEvening: Boolean get() = time.isEvening
    val isNight: Boolean get() = time.isNight
    val isWeekend: Boolean get() = time.isWeekend
    
    // Session shortcuts
    val sessionNumber: Int get() = session.sessionNumber
    val screenVisitCount: Int get() = session.screenVisitCount
    val totalTasksCompleted: Int get() = session.totalTasksCompleted
    val isFirstSession: Boolean get() = session.isFirstSession
    val isNewUser: Boolean get() = session.isNewUser
    val isRegularUser: Boolean get() = session.isRegularUser
    val isVeteranUser: Boolean get() = session.isVeteranUser
    val isReturningAfterAbsence: Boolean get() = session.isReturningAfterAbsence
    val hasBeenHereAWhile: Boolean get() = session.hasBeenHereAWhile
    val isQuickVisit: Boolean get() = session.isQuickVisit
    val isFirstVisit: Boolean get() = session.isFirstVisit
    
    // Personality shortcuts
    val isNightOwl: Boolean get() = personality.isNightOwl
    val isMorningPerson: Boolean get() = personality.isMorningPerson
    val isCurious: Boolean get() = personality.isCurious
    val isThoughtful: Boolean get() = personality.isThoughtful
    val isVisual: Boolean get() = personality.isVisual
    val isPersistent: Boolean get() = personality.isPersistent
    val isReluctantAdventurer: Boolean get() = personality.isReluctantAdventurer
    
    // Device shortcuts
    val canCapturePhoto: Boolean get() = device.canCapturePhoto
    val canRecordAudio: Boolean get() = device.canRecordAudio
    val canAccessPhotos: Boolean get() = device.canAccessPhotos
    val isBatteryLow: Boolean get() = device.isBatteryLow
    
    // Mood shortcuts
    val moodTrend: MoodTrend get() = mood.trend
    val currentMood: Mood? get() = mood.currentMood
    val isMoodDeclining: Boolean get() = mood.trend == MoodTrend.DECLINING
    val isMoodImproving: Boolean get() = mood.trend == MoodTrend.IMPROVING
}

// =============================================================================
// TIME CONTEXT
// Ephemeral - changes moment to moment
// =============================================================================

data class TimeContext(
    /** Hour of day (0-23) */
    val hour: Int,
    
    /** Day of week (1=Monday, 7=Sunday) */
    val dayOfWeek: Int,
    
    /** Month of year (1=January, 12=December) */
    val month: Int,
    
    /** Day of month (1-31) */
    val dayOfMonth: Int,
    
    /** Whether it's a weekend (Saturday or Sunday) */
    val isWeekend: Boolean,
) {
    /** Late night (midnight to 4am) */
    val isLateNight: Boolean get() = hour in 0..4
    
    /** Early morning (5am to 7am) */
    val isEarlyMorning: Boolean get() = hour in 5..7
    
    /** Morning (8am to 11am) */
    val isMorning: Boolean get() = hour in 8..11
    
    /** Afternoon (noon to 4pm) */
    val isAfternoon: Boolean get() = hour in 12..16
    
    /** Evening (5pm to 8pm) */
    val isEvening: Boolean get() = hour in 17..20
    
    /** Night (9pm to 11pm) */
    val isNight: Boolean get() = hour in 21..23
}

// =============================================================================
// DEVICE CONTEXT
// Ephemeral - current device state
// TODO: Implement platform-specific providers (expect/actual) for real values
// =============================================================================

data class DeviceContext(
    /** Battery level 0-100, null if unknown */
    val batteryLevel: Int?,
    
    /** Whether device is in low power mode */
    val isLowPowerMode: Boolean,
    
    /** Whether device has internet connectivity */
    val hasInternet: Boolean,
    
    /** Whether device is currently charging */
    val isCharging: Boolean,
    
    /** Whether headphones/audio output is connected */
    val hasAudioOutput: Boolean,
    
    /** Whether the device has a camera available */
    val hasCamera: Boolean,
    
    /** Whether microphone permission is granted */
    val hasMicrophonePermission: Boolean,
    
    /** Whether camera permission is granted */
    val hasCameraPermission: Boolean,
    
    /** Whether photo library permission is granted */
    val hasPhotoLibraryPermission: Boolean,
) {
    /** Battery is critically low (<20%) */
    val isBatteryLow: Boolean get() = batteryLevel?.let { it < 20 } ?: false
    
    /** Battery is very low (<10%) */
    val isBatteryCritical: Boolean get() = batteryLevel?.let { it < 10 } ?: false
    
    /** Can capture photos (has camera + permission) */
    val canCapturePhoto: Boolean get() = hasCamera && hasCameraPermission
    
    /** Can record audio (has mic permission) */
    val canRecordAudio: Boolean get() = hasMicrophonePermission
    
    /** Can access photo library */
    val canAccessPhotos: Boolean get() = hasPhotoLibraryPermission
    
    companion object {
        /** 
         * Default when device info is unavailable.
         * Assumes best-case scenario (all capabilities available).
         * TODO: Replace with actual platform implementations.
         */
        val Unknown = DeviceContext(
            batteryLevel = null,
            isLowPowerMode = false,
            hasInternet = true,
            isCharging = false,
            hasAudioOutput = true,
            hasCamera = true,
            hasMicrophonePermission = true,
            hasCameraPermission = true,
            hasPhotoLibraryPermission = true,
        )
    }
}

// =============================================================================
// SESSION CONTEXT
// Semi-ephemeral - changes per session
// =============================================================================

data class SessionContext(
    /** Total number of sessions the user has had (1 = first session) */
    val sessionNumber: Int,
    
    /** How long the current session has been active */
    val currentSessionDuration: Duration,
    
    /** Time since the last session ended (null if first session) */
    val timeSinceLastSession: Duration?,
    
    /** Total tasks completed across all sessions */
    val totalTasksCompleted: Int,
    
    /** Total times app has been opened */
    val totalAppOpens: Int,
    
    /** How many times this specific screen has been visited */
    val screenVisitCount: Int,
) {
    /** First time visiting this screen */
    val isFirstVisit: Boolean get() = screenVisitCount <= 1
    
    /** First session ever */
    val isFirstSession: Boolean get() = sessionNumber <= 1
    
    /** Brand new to the app (first few sessions) */
    val isNewUser: Boolean get() = sessionNumber <= 3
    
    /** Has used the app a decent amount */
    val isRegularUser: Boolean get() = sessionNumber in 4..20
    
    /** Long-time user */
    val isVeteranUser: Boolean get() = sessionNumber > 20
    
    /** Returning after being away for a while (> 7 days) */
    val isReturningAfterAbsence: Boolean 
        get() = timeSinceLastSession?.inWholeDays?.let { it >= 7 } ?: false
    
    /** Been in this session for a while (> 10 minutes) */
    val hasBeenHereAWhile: Boolean 
        get() = currentSessionDuration.inWholeMinutes >= 10
    
    /** Quick visit (< 2 minutes so far) */
    val isQuickVisit: Boolean 
        get() = currentSessionDuration.inWholeMinutes < 2
}

// =============================================================================
// PERSONALITY CONTEXT
// Persistent - learned user traits
// =============================================================================

data class PersonalityContext(
    // -------------------------------------------------------------------------
    // Time-based traits
    // -------------------------------------------------------------------------
    
    /** Opens app after midnight frequently (>30% of sessions) */
    val isNightOwl: Boolean,
    
    /** Opens app in the morning frequently (>30% of sessions) */
    val isMorningPerson: Boolean,
    
    /** Opens app around midday frequently (>30% of sessions) */
    val isMiddayRegular: Boolean,
    
    // -------------------------------------------------------------------------
    // Behavioral traits
    // -------------------------------------------------------------------------
    
    /** Clicked "no" multiple times during onboarding but eventually said yes */
    val isReluctantAdventurer: Boolean,
    
    /** Opens settings frequently relative to sessions - likes to explore */
    val isCurious: Boolean,
    
    /** Opens app but often doesn't complete tasks */
    val isHesitant: Boolean,
    
    /** Adds optional photos/media frequently (>40% when available) */
    val isVisual: Boolean,
    
    /** Takes time before responding or rewrites answers */
    val isThoughtful: Boolean,
    
    /** Clicked useless button many times - determined personality */
    val isPersistent: Boolean,
    
    /** Completed the useless button journey (clicked 10+ times) */
    val completedUselessButtonJourney: Boolean,
    
    // -------------------------------------------------------------------------
    // Engagement traits
    // -------------------------------------------------------------------------
    
    /** >80% completion rate with 10+ tasks */
    val isCommitted: Boolean,
    
    /** <50% completion rate with 10+ attempts */
    val isSelectivePlayer: Boolean,
    
    /** Writes long responses (avg >150 chars) */
    val isWordsmith: Boolean,
    
    /** Writes short responses (avg <50 chars) */
    val isBrief: Boolean,
)

// =============================================================================
// MOOD CONTEXT
// Computed from recent history
// =============================================================================

data class MoodContext(
    /** Current session's mood, if captured */
    val currentMood: Mood?,
    
    /** Trend direction based on recent sessions */
    val trend: MoodTrend,
    
    /** Recent moods (most recent first), up to 5 */
    val recentMoods: List<Mood>,
    
    /** Number of consecutive sessions with same general mood direction */
    val streakLength: Int,
) {
    /** Has user reported mood this session? */
    val hasCurrentMood: Boolean get() = currentMood != null
    
    /** Has enough data to compute trends (at least 2 moods) */
    val hasTrendData: Boolean get() = recentMoods.size >= 2
    
    /** Mood has been consistently low (3+ BAD/LOW sessions) */
    val isConsistentlyLow: Boolean get() = 
        streakLength >= 3 && recentMoods.firstOrNull()?.let { it == Mood.BAD || it == Mood.LOW } == true
    
    /** Mood has been consistently good (3+ GOOD/GREAT sessions) */
    val isConsistentlyGood: Boolean get() = 
        streakLength >= 3 && recentMoods.firstOrNull()?.let { it == Mood.GOOD || it == Mood.GREAT } == true
    
    companion object {
        /** Default when no mood data available */
        val Unknown = MoodContext(
            currentMood = null,
            trend = MoodTrend.UNKNOWN,
            recentMoods = emptyList(),
            streakLength = 0,
        )
    }
}
