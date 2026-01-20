package com.dangerfield.goodtimes.libraries.goodtimes.impl

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * JSON model for tasks loaded from tasks.json.
 * This maps directly to the JSON structure.
 */
@Serializable
data class TaskJson(
    val id: String,
    val type: String,
    val categories: List<String>,
    val difficulty: String,
    val instruction: String,
    val requiresSocial: Boolean,
    val bestForMoods: List<String>? = null,
    val avoidForMoods: List<String>? = null,
    val safeToReflect: Boolean,
    val responseStyle: ResponseStyleJson = ResponseStyleJson(),
    val conditions: TaskConditionsJson? = null,
    val assets: TaskAssetsJson? = null,
    val followUp: FollowUpConfigJson? = null,
    val placeholder: String? = null,
    val durationSeconds: Int? = null,
    val selectionOptions: List<String>? = null,
    val minSelections: Int? = null,
    val maxSelections: Int? = null,
    val routingOptions: List<RoutingOptionJson>? = null,
    val requireFrontCamera: Boolean? = null,
    // Depth requirements
    val requiresDepth: Boolean = false,
    val minCharacters: Int? = null,
    val isIntroTask: Boolean = false,
)

@Serializable
data class ResponseStyleJson(
    val allowsText: Boolean = false,
    val allowsPhoto: Boolean = false,
    val allowsAudio: Boolean = false,
    val allowsDrawing: Boolean = false,
    val expectedLength: String? = null, // WORD, SHORT, MEDIUM, LONG - defaults to MEDIUM
)

@Serializable
data class TaskConditionsJson(
    val timeAfter: String? = null,
    val timeBefore: String? = null,
    val minDaysSinceLastSession: Int? = null,
    val requiresMoodTrend: String? = null,
    val monthRange: MonthRangeJson? = null,
    val dayOfMonthRange: DayRangeJson? = null,
)

@Serializable
data class MonthRangeJson(
    val startMonth: Int, // 1-12
    val endMonth: Int,   // 1-12
)

@Serializable
data class DayRangeJson(
    val startDay: Int, // 1-31
    val endDay: Int,   // 1-31
)

@Serializable
data class TaskAssetsJson(
    val imagePath: String? = null,
    val backgroundImagePath: String? = null,
    val accentColor: String? = null,
)

/**
 * Configuration for post-task follow-up flows.
 */
@Serializable
data class FollowUpConfigJson(
    val type: String,
    val required: Boolean = false,
    val ifYes: FollowUpConfigJson? = null,
    val options: List<FollowUpOptionJson>? = null,
)

/**
 * A follow-up option (for DID_YOU "no" options or CUSTOM follow-ups).
 */
@Serializable
data class FollowUpOptionJson(
    val id: String,
    val text: String,
    val reschedule: Boolean = false,
    val skipPermanent: Boolean = false,
)

@Serializable
data class RoutingOptionJson(
    val text: String,
    val preferCategory: String? = null,
    val avoidCategory: String? = null,
    val preferDifficulty: String? = null,
    val effectDuration: Int = 3,
)
