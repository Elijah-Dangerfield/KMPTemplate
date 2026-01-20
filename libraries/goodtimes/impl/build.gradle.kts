plugins {
    id("goodtimes.kotlin.multiplatform")
    alias(libs.plugins.sentryKmp)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

moduleConfig {
    optIn("kotlin.time.ExperimentalTime")
    optIn("kotlin.uuid.ExperimentalUuidApi")
    optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
}

android {
    namespace = "com.dangerfield.goodtimes.libraries.goodtimes.impl"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.libraries.goodtimes)

            implementation(projects.libraries.core)
            implementation(libs.kermit)
            implementation(projects.libraries.flowroutines)
            implementation(projects.libraries.goodtimes.storage)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(compose.components.resources)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

compose.resources {
    publicResClass = false
}

// =============================================================================
// Task JSON Validation - Catches schema errors at build time
// =============================================================================

val validateTasksJson by tasks.registering {
    group = "verification"
    description = "Validates tasks.json against expected schema (enum values, required fields)"
    
    val tasksJsonFile = file("src/commonMain/composeResources/files/tasks.json")
    inputs.file(tasksJsonFile)
    
    doLast {
        val validTaskTypes = setOf(
            "PROMPT", "DRAWING", "PHOTO_CAPTURE", "AUDIO_CAPTURE", "SELECTION",
            "INSTRUCTION", "ROUTING", "STILLNESS", "HOLD_FINGER", "WAIT_TIMER",
            "DONT_OPEN_UNTIL", "GAME"
        )
        val validCategories = setOf(
            "SOCIAL", "REFLECTION", "PLAY", "ACTION", "STILLNESS", "DISCOMFORT"
        )
        val validDifficulties = setOf("LIGHT", "MEDIUM", "HEAVY")
        val validMoods = setOf("TERRIBLE", "BAD", "OKAY", "GOOD", "GREAT")
        val validExpectedLengths = setOf("SHORT", "MEDIUM", "LONG")
        val validTimeOfDay = setOf("MORNING", "AFTERNOON", "EVENING", "NIGHT")
        
        val json = groovy.json.JsonSlurper().parseText(tasksJsonFile.readText()) as List<Map<String, Any?>>
        val errors = mutableListOf<String>()
        
        json.forEachIndexed { index, task ->
            val id = task["id"] as? String ?: "unknown"
            
            // Validate type
            val type = task["type"] as? String
            if (type == null) {
                errors.add("Task '$id' (index $index): missing required field 'type'")
            } else if (type !in validTaskTypes) {
                errors.add("Task '$id' (index $index): invalid type '$type'. Valid: $validTaskTypes")
            }
            
            // Validate categories
            val categories = task["categories"] as? List<*>
            if (categories == null) {
                errors.add("Task '$id' (index $index): missing required field 'categories'")
            } else {
                categories.forEach { cat ->
                    if (cat !in validCategories) {
                        errors.add("Task '$id' (index $index): invalid category '$cat'. Valid: $validCategories")
                    }
                }
            }
            
            // Validate difficulty
            val difficulty = task["difficulty"] as? String
            if (difficulty == null) {
                errors.add("Task '$id' (index $index): missing required field 'difficulty'")
            } else if (difficulty !in validDifficulties) {
                errors.add("Task '$id' (index $index): invalid difficulty '$difficulty'. Valid: $validDifficulties")
            }
            
            // Validate bestForMoods if present
            (task["bestForMoods"] as? List<*>)?.forEach { mood ->
                if (mood !in validMoods) {
                    errors.add("Task '$id' (index $index): invalid mood '$mood' in bestForMoods. Valid: $validMoods")
                }
            }
            
            // Validate avoidForMoods if present
            (task["avoidForMoods"] as? List<*>)?.forEach { mood ->
                if (mood !in validMoods) {
                    errors.add("Task '$id' (index $index): invalid mood '$mood' in avoidForMoods. Valid: $validMoods")
                }
            }
            
            // Validate responseStyle.expectedLength if present
            val responseStyle = task["responseStyle"] as? Map<*, *>
            val expectedLength = responseStyle?.get("expectedLength") as? String
            if (expectedLength != null && expectedLength !in validExpectedLengths) {
                errors.add("Task '$id' (index $index): invalid expectedLength '$expectedLength'. Valid: $validExpectedLengths")
            }
            
            // Validate conditions.timeOfDay if present
            val conditions = task["conditions"] as? Map<*, *>
            val timeOfDay = conditions?.get("timeOfDay") as? String
            if (timeOfDay != null && timeOfDay !in validTimeOfDay) {
                errors.add("Task '$id' (index $index): invalid timeOfDay '$timeOfDay'. Valid: $validTimeOfDay")
            }
            
            // Validate monthRange if present
            val monthRange = conditions?.get("monthRange") as? Map<*, *>
            if (monthRange != null) {
                val startMonth = (monthRange["startMonth"] as? Number)?.toInt()
                val endMonth = (monthRange["endMonth"] as? Number)?.toInt()
                if (startMonth == null || startMonth !in 1..12) {
                    errors.add("Task '$id' (index $index): invalid startMonth '$startMonth'. Must be 1-12")
                }
                if (endMonth == null || endMonth !in 1..12) {
                    errors.add("Task '$id' (index $index): invalid endMonth '$endMonth'. Must be 1-12")
                }
            }
            
            // Validate dayOfMonthRange if present
            val dayRange = conditions?.get("dayOfMonthRange") as? Map<*, *>
            if (dayRange != null) {
                val startDay = (dayRange["startDay"] as? Number)?.toInt()
                val endDay = (dayRange["endDay"] as? Number)?.toInt()
                if (startDay == null || startDay !in 1..31) {
                    errors.add("Task '$id' (index $index): invalid startDay '$startDay'. Must be 1-31")
                }
                if (endDay == null || endDay !in 1..31) {
                    errors.add("Task '$id' (index $index): invalid endDay '$endDay'. Must be 1-31")
                }
            }
        }
        
        if (errors.isNotEmpty()) {
            throw GradleException("tasks.json validation failed:\n${errors.joinToString("\n") { "  ❌ $it" }}")
        }
        
        logger.lifecycle("✅ tasks.json validated: ${json.size} tasks, all schema checks passed")
    }
}

// Run validation before compiling
tasks.matching { it.name.startsWith("compile") && it.name.contains("Kotlin") }.configureEach {
    dependsOn(validateTasksJson)
}