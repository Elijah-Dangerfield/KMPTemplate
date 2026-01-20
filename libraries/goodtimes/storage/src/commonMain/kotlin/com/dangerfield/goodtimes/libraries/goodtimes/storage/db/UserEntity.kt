package com.dangerfield.goodtimes.libraries.goodtimes.storage.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * User entity - singleton, one per install.
 * Stores personality scores, behavioral counts, and flags.
 */
@Entity(tableName = "user")
data class UserEntity(
    @PrimaryKey
    val id: String = "user", // Singleton

    val name: String?,
    
    @ColumnInfo(name = "name_set_at")
    val nameSetAt: Long?, // epoch millis - when name was first set
    
    @ColumnInfo(name = "name_updated_at")
    val nameUpdatedAt: Long?, // epoch millis - when name was last changed

    @ColumnInfo(name = "created_at")
    val createdAt: Long, // epoch millis

    @ColumnInfo(name = "last_session_at")
    val lastSessionAt: Long?, // epoch millis

    // Personality scores (0-100, start at 50)
    @ColumnInfo(name = "social_comfort")
    val socialComfort: Int = 50,

    val openness: Int = 50,

    val playfulness: Int = 50,

    val patience: Int = 50,

    @ColumnInfo(name = "reflection_depth")
    val reflectionDepth: Int = 50,

    // Response style scores (0-100, start at 50)
    @ColumnInfo(name = "writing_affinity")
    val writingAffinity: Int = 50,

    @ColumnInfo(name = "photo_affinity")
    val photoAffinity: Int = 50,

    @ColumnInfo(name = "audio_affinity")
    val audioAffinity: Int = 50,

    @ColumnInfo(name = "drawing_affinity")
    val drawingAffinity: Int = 50,

    @ColumnInfo(name = "game_affinity")
    val gameAffinity: Int = 50,

    // Note: Mood tracking is derived from SessionEntity.mood
    // Query recent sessions to compute mood trend

    // Flags
    @ColumnInfo(name = "has_completed_onboarding")
    val hasCompletedOnboarding: Boolean = false,
    
    @ColumnInfo(name = "has_completed_intro_task")
    val hasCompletedIntroTask: Boolean = false,

    @ColumnInfo(name = "has_been_asked_about_social_skips")
    val hasBeenAskedAboutSocialSkips: Boolean = false,

    @ColumnInfo(name = "has_been_asked_for_name")
    val hasBeenAskedForName: Boolean = false,

    @ColumnInfo(name = "has_seen_declining_mood_routing")
    val hasSeenDecliningMoodRouting: Boolean = false,

    @ColumnInfo(name = "has_seen_stop_asking_routing")
    val hasSeenStopAskingRouting: Boolean = false,

    @ColumnInfo(name = "current_task_id")
    val currentTaskId: String?,

    // Routing effects (temporary, from ROUTING tasks)
    @ColumnInfo(name = "routing_effects_json")
    val routingEffectsJson: String?, // JSON RoutingEffects

    // Stats
    @ColumnInfo(name = "sessions_count")
    val sessionsCount: Int = 0,

    @ColumnInfo(name = "tasks_completed")
    val tasksCompleted: Int = 0,

    @ColumnInfo(name = "tasks_skipped")
    val tasksSkipped: Int = 0,

    // Behavioral signals (raw counts for easter eggs & goodbye reel)
    @ColumnInfo(name = "app_open_count")
    val appOpenCount: Int = 0,

    @ColumnInfo(name = "settings_open_count")
    val settingsOpenCount: Int = 0,

    @ColumnInfo(name = "about_open_count")
    val aboutOpenCount: Int = 0,

    @ColumnInfo(name = "no_click_count_onboarding")
    val noClickCountOnboarding: Int = 0,

    @ColumnInfo(name = "bug_report_count")
    val bugReportCount: Int = 0,

    @ColumnInfo(name = "permission_denial_count")
    val permissionDenialCount: Int = 0,

    @ColumnInfo(name = "back_button_press_count")
    val backButtonPressCount: Int = 0,

    @ColumnInfo(name = "shake_count")
    val shakeCount: Int = 0,

    @ColumnInfo(name = "late_night_session_count")
    val lateNightSessionCount: Int = 0,

    @ColumnInfo(name = "morning_session_count")
    val morningSessionCount: Int = 0, // 6am-10am
    
    @ColumnInfo(name = "midday_session_count")
    val middaySessionCount: Int = 0, // 11am-2pm

    @ColumnInfo(name = "average_hesitation_ms")
    val averageHesitationMs: Long?,

    @ColumnInfo(name = "optional_media_added_count")
    val optionalMediaAddedCount: Int = 0,

    @ColumnInfo(name = "delete_and_rewrite_count")
    val deleteAndRewriteCount: Int = 0,

    @ColumnInfo(name = "quick_exit_count")
    val quickExitCount: Int = 0, // closed app within 10s of opening

    @ColumnInfo(name = "idle_session_count")
    val idleSessionCount: Int = 0, // opened but didn't do anything

    // New fields for ratio-based traits
    @ColumnInfo(name = "optional_media_opportunities")
    val optionalMediaOpportunities: Int = 0, // Tasks where optional media was available
    
    @ColumnInfo(name = "text_tasks_completed")
    val textTasksCompleted: Int = 0, // Tasks requiring text responses
    
    @ColumnInfo(name = "total_text_length")
    val totalTextLength: Int = 0, // Cumulative characters typed in text responses

    // Easter egg state
    @ColumnInfo(name = "easter_egg_state_json")
    val easterEggStateJson: String = "{}", // JSON EasterEggState
)
