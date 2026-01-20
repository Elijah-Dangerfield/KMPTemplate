package com.dangerfield.goodtimes.libraries.goodtimes.impl

import com.dangerfield.goodtimes.libraries.core.logging.KLog
import com.dangerfield.goodtimes.libraries.goodtimes.Difficulty
import com.dangerfield.goodtimes.libraries.goodtimes.DayRange
import com.dangerfield.goodtimes.libraries.goodtimes.ExpectedLength
import com.dangerfield.goodtimes.libraries.goodtimes.FollowUpConfig
import com.dangerfield.goodtimes.libraries.goodtimes.FollowUpOption
import com.dangerfield.goodtimes.libraries.goodtimes.FollowUpType
import com.dangerfield.goodtimes.libraries.goodtimes.MonthRange
import com.dangerfield.goodtimes.libraries.goodtimes.Mood
import com.dangerfield.goodtimes.libraries.goodtimes.MoodTrend
import com.dangerfield.goodtimes.libraries.goodtimes.ResponseStyle
import com.dangerfield.goodtimes.libraries.goodtimes.RoutingOption
import com.dangerfield.goodtimes.libraries.goodtimes.Task
import com.dangerfield.goodtimes.libraries.goodtimes.TaskAssets
import com.dangerfield.goodtimes.libraries.goodtimes.TaskCategory
import com.dangerfield.goodtimes.libraries.goodtimes.TaskConditions
import com.dangerfield.goodtimes.libraries.goodtimes.TaskRepository
import com.dangerfield.goodtimes.libraries.goodtimes.TaskType
import com.dangerfield.goodtimes.libraries.goodtimes.storage.db.TaskEntity
import com.dangerfield.goodtimes.libraries.goodtimes.storage.db.TasksDao
import goodtimes.libraries.goodtimes.impl.generated.resources.Res
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class TaskRepositoryImpl(
    private val tasksDao: TasksDao,
) : TaskRepository {

    private val logger = KLog.withTag("TaskRepository")

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /*
    Thoughts for improvement:
    DO we need an initialize function? can we not just use the init {}?

     */

    override suspend fun initialize() {
        val count = tasksDao.getCount()
        logger.d("initialize: current task count = $count")

        if (count == 0) {
            logger.i("Database empty, seeding tasks from JSON...")
            seedFromJson()
        }
    }

    private suspend fun seedFromJson() {
        try {
            val jsonString = Res.readBytes("files/tasks.json").decodeToString()
            val tasks = json.decodeFromString<List<TaskJson>>(jsonString)
            logger.i("Parsed ${tasks.size} tasks from JSON")

            val entities = tasks.map { it.toEntity() }
            tasksDao.insertAll(entities)
            logger.i("Seeded ${entities.size} tasks into database")
        } catch (e: Exception) {
            logger.e(e) { "Failed to seed tasks from JSON" }
        }
    }

    override fun observeAllTasks(): Flow<List<Task>> {
        return tasksDao.observeAllTasks().map { entities ->
            entities.map { it.toTask() }
        }
    }

    override suspend fun getAllTasks(): List<Task> {
        return tasksDao.getAllTasks().map { it.toTask() }
    }

    override suspend fun getTask(id: String): Task? {
        return tasksDao.getTask(id)?.toTask()
    }

    override suspend fun getTaskCount(): Int {
        return tasksDao.getCount()
    }

    // ========== Mapping Functions ==========

    private fun TaskJson.toEntity(): TaskEntity {
        return TaskEntity(
            id = id,
            type = type,
            categories = json.encodeToString(categories),
            difficulty = difficulty,
            requiresSocial = requiresSocial,
            bestForMoods = bestForMoods?.let { json.encodeToString(it) },
            avoidForMoods = avoidForMoods?.let { json.encodeToString(it) },
            minimumScores = null, // Not in JSON yet
            safeToReflect = safeToReflect,
            instruction = instruction,
            allowsText = responseStyle.allowsText,
            allowsPhoto = responseStyle.allowsPhoto,
            allowsAudio = responseStyle.allowsAudio,
            allowsDrawing = responseStyle.allowsDrawing,
            expectedLength = responseStyle.expectedLength ?: "MEDIUM",
            conditionTimeAfter = conditions?.timeAfter,
            conditionTimeBefore = conditions?.timeBefore,
            conditionMinDaysAway = conditions?.minDaysSinceLastSession,
            conditionMoodTrend = conditions?.requiresMoodTrend,
            conditionMonthStart = conditions?.monthRange?.startMonth,
            conditionMonthEnd = conditions?.monthRange?.endMonth,
            conditionDayStart = conditions?.dayOfMonthRange?.startDay,
            conditionDayEnd = conditions?.dayOfMonthRange?.endDay,
            imagePath = assets?.imagePath,
            backgroundImagePath = assets?.backgroundImagePath,
            accentColor = assets?.accentColor,
            placeholder = placeholder,
            routingOptions = routingOptions?.let { json.encodeToString(it) },
            selectionOptions = selectionOptions?.let { json.encodeToString(it) },
            minSelections = minSelections,
            maxSelections = maxSelections,
            followUp = followUp?.let { json.encodeToString(it) },
            durationSeconds = durationSeconds,
            requireFrontCamera = requireFrontCamera,
            requiresDepth = requiresDepth,
            minCharacters = minCharacters,
            isIntroTask = isIntroTask,
        )
    }

    private fun TaskEntity.toTask(): Task {
        return Task(
            id = id,
            type = TaskType.valueOf(type),
            categories = parseStringList(categories).map { TaskCategory.valueOf(it) },
            difficulty = runCatching { Difficulty.valueOf(difficulty) }.getOrElse { Difficulty.MEDIUM },
            instruction = instruction,
            requiresSocial = requiresSocial,
            bestForMoods = bestForMoods?.let { parseStringList(it).map { m -> Mood.valueOf(m) } },
            avoidForMoods = avoidForMoods?.let { parseStringList(it).map { m -> Mood.valueOf(m) } },
            safeToReflect = safeToReflect,
            responseStyle = ResponseStyle(
                allowsText = allowsText,
                allowsPhoto = allowsPhoto,
                allowsAudio = allowsAudio,
                allowsDrawing = allowsDrawing,
                expectedLength = ExpectedLength.valueOf(expectedLength),
            ),
            conditions = if (conditionTimeAfter != null || conditionTimeBefore != null || conditionMinDaysAway != null || conditionMoodTrend != null || conditionMonthStart != null || conditionDayStart != null) {
                TaskConditions(
                    timeAfter = conditionTimeAfter,
                    timeBefore = conditionTimeBefore,
                    minDaysSinceLastSession = conditionMinDaysAway,
                    requiresMoodTrend = conditionMoodTrend?.let { MoodTrend.valueOf(it) },
                    monthRange = conditionMonthStart?.let { start ->
                        conditionMonthEnd?.let { end -> MonthRange(start, end) }
                    },
                    dayOfMonthRange = conditionDayStart?.let { start ->
                        conditionDayEnd?.let { end -> DayRange(start, end) }
                    },
                )
            } else null,
            assets = if (imagePath != null || backgroundImagePath != null || accentColor != null) {
                TaskAssets(
                    imagePath = imagePath,
                    backgroundImagePath = backgroundImagePath,
                    accentColor = accentColor,
                )
            } else null,
            followUp = followUp?.let { parseFollowUpConfig(it) },
            requiresDepth = requiresDepth,
            minCharacters = minCharacters,
            isIntroTask = isIntroTask,
            placeholder = placeholder,
            durationSeconds = durationSeconds,
            selectionOptions = selectionOptions?.let { parseStringList(it) },
            minSelections = minSelections,
            maxSelections = maxSelections,
            routingOptions = routingOptions?.let { parseRoutingOptions(it) },
            requireFrontCamera = requireFrontCamera,
        )
    }

    private fun parseStringList(jsonStr: String): List<String> {
        return try {
            json.decodeFromString<List<String>>(jsonStr)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseFollowUpConfig(jsonStr: String): FollowUpConfig? {
        return try {
            val jsonModel = json.decodeFromString<FollowUpConfigJson>(jsonStr)
            jsonModel.toDomain()
        } catch (e: Exception) {
            null
        }
    }

    private fun FollowUpConfigJson.toDomain(): FollowUpConfig {
        return FollowUpConfig(
            type = FollowUpType.valueOf(type),
            required = required,
            ifYes = ifYes?.toDomain(),
            options = options?.map { option ->
                FollowUpOption(
                    id = option.id,
                    text = option.text,
                    reschedule = option.reschedule,
                    skipPermanent = option.skipPermanent,
                )
            },
        )
    }

    private fun parseRoutingOptions(jsonStr: String): List<RoutingOption> {
        return try {
            json.decodeFromString<List<RoutingOptionJson>>(jsonStr).map {
                RoutingOption(
                    text = it.text,
                    preferCategory = it.preferCategory?.let { c -> TaskCategory.valueOf(c) },
                    avoidCategory = it.avoidCategory?.let { c -> TaskCategory.valueOf(c) },
                    preferDifficulty = it.preferDifficulty?.let { d -> Difficulty.valueOf(d) },
                    effectDuration = it.effectDuration,
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
