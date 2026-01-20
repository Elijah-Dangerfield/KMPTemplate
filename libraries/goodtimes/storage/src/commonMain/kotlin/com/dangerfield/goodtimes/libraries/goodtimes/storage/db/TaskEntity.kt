package com.dangerfield.goodtimes.libraries.goodtimes.storage.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Database entity for Task definitions.
 * Tasks are shipped as JSON and inserted on first launch.
 */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey
    val id: String,

    val type: String, // TaskType enum name

    val categories: String, // JSON list: ["SOCIAL", "REFLECTION"]

    val difficulty: String, // Difficulty enum name

    @ColumnInfo(name = "requires_social")
    val requiresSocial: Boolean,

    @ColumnInfo(name = "best_for_moods")
    val bestForMoods: String?, // JSON list of Mood enum names

    @ColumnInfo(name = "avoid_for_moods")
    val avoidForMoods: String?, // JSON list of Mood enum names

    @ColumnInfo(name = "minimum_scores")
    val minimumScores: String?, // JSON map: {"SOCIAL_COMFORT": 30}

    @ColumnInfo(name = "safe_to_reflect")
    val safeToReflect: Boolean,

    val instruction: String,

    // Response style flags (queryable for affinity matching)
    @ColumnInfo(name = "allows_text")
    val allowsText: Boolean,

    @ColumnInfo(name = "allows_photo")
    val allowsPhoto: Boolean,

    @ColumnInfo(name = "allows_audio")
    val allowsAudio: Boolean,

    @ColumnInfo(name = "allows_drawing")
    val allowsDrawing: Boolean,
    
    @ColumnInfo(name = "expected_length")
    val expectedLength: String = "MEDIUM", // ExpectedLength enum: WORD, SHORT, MEDIUM, LONG

    // Conditions (for time/mood gating)
    @ColumnInfo(name = "condition_time_after")
    val conditionTimeAfter: String?, // "22:00"

    @ColumnInfo(name = "condition_time_before")
    val conditionTimeBefore: String?, // "05:00"

    @ColumnInfo(name = "condition_min_days_away")
    val conditionMinDaysAway: Int?,

    @ColumnInfo(name = "condition_mood_trend")
    val conditionMoodTrend: String?, // MoodTrend enum name
    
    @ColumnInfo(name = "condition_month_start")
    val conditionMonthStart: Int? = null, // 1-12, start of month range
    
    @ColumnInfo(name = "condition_month_end")
    val conditionMonthEnd: Int? = null, // 1-12, end of month range
    
    @ColumnInfo(name = "condition_day_start")
    val conditionDayStart: Int? = null, // 1-31, start of day-of-month range
    
    @ColumnInfo(name = "condition_day_end")
    val conditionDayEnd: Int? = null, // 1-31, end of day-of-month range

    // Assets
    @ColumnInfo(name = "image_path")
    val imagePath: String?,

    @ColumnInfo(name = "background_image_path")
    val backgroundImagePath: String?,

    @ColumnInfo(name = "accent_color")
    val accentColor: String?, // hex color

    // Type-specific fields
    val placeholder: String?, // PROMPT

    @ColumnInfo(name = "routing_options")
    val routingOptions: String?, // ROUTING - JSON blob

    @ColumnInfo(name = "selection_options")
    val selectionOptions: String?, // SELECTION - JSON list

    @ColumnInfo(name = "min_selections")
    val minSelections: Int?, // SELECTION

    @ColumnInfo(name = "max_selections")
    val maxSelections: Int?, // SELECTION

    @ColumnInfo(name = "follow_up")
    val followUp: String?, // FollowUpConfig - JSON blob

    @ColumnInfo(name = "duration_seconds")
    val durationSeconds: Int?, // timed types, STILLNESS, AUDIO_CAPTURE

    @ColumnInfo(name = "require_front_camera")
    val requireFrontCamera: Boolean?, // PHOTO_CAPTURE
    
    // Depth requirements
    @ColumnInfo(name = "requires_depth")
    val requiresDepth: Boolean = false, // If true, short answers trigger follow-up
    
    @ColumnInfo(name = "min_characters")
    val minCharacters: Int? = null, // Minimum characters for depth (default 20 if requiresDepth)
    
    @ColumnInfo(name = "is_intro_task")
    val isIntroTask: Boolean = false, // If true, this is shown first to new users
)
