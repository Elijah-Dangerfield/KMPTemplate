# The App of Good Times — System Design

This is a KMP app inspired by "The Book of Good Times"—a fictional book from Hank Green's novel "An Absolutely Remarkable Thing." In the novel, the book is found in a trash can and is semi-sentient. It wants to become human. Its pages change based on the reader, asking them to do things that subtly improve their life: talk to a stranger, invest in a stock, pay attention to something they'd normally ignore.

Hank released a real physical version, but a printed book can't adapt. This app can.

The app presents "tasks"—invitations to do things. Take a photo with a stranger. Draw something in the dark. Sing into a recording. Journal about a memory. Play a small game. Some tasks are playful, some are reflective, some push social comfort.

How the user responds teaches the app about them. Not just whether they complete a task, but _how_: Did they skip it? Did they take twice as long as usual? Did they write three words or three paragraphs? Each response adjusts internal scores (social comfort, openness, playfulness, patience, reflection depth), and those scores influence which tasks appear next.

We also track mood. Each session can optionally capture how the user is feeling, and we track the trend over time. If someone's been low for several sessions, the app might ask: "I've noticed things have been heavy lately. Do you want something light, or do you want to sit with it?" Their answer shapes what comes next.

The app has a final page. It ends. When the user reaches it, we show them what we learned and thank them. No streaks, no notifications, no infinite engagement. Just a beginning, middle, and end—like any good book.

Target: 200-300 tasks, with the selection algorithm ensuring each user's path feels personal.

No AI. Just thoughtful conditional logic and careful observation.

---

## Platform & Tech Stack

**Kotlin Multiplatform (KMP)** with Compose Multiplatform for shared UI.

| Layer            | Technology                    |
| ---------------- | ----------------------------- |
| UI               | Compose Multiplatform         |
| State Management | SEAViewModel (custom pattern) |
| Navigation       | Voyager                       |
| DI               | kotlin-inject-anvil           |
| Database         | Room (multiplatform)          |
| Serialization    | kotlinx.serialization         |
| Time             | kotlinx.datetime              |

**Platform structure:**

```
apps/
    compose/                  # Shared KMP compose app
        src/commonMain/       # All shared code
        src/androidMain/      # Android-specific (minimal)
        src/iosMain/          # iOS-specific (minimal)
    ios/                      # Native iOS wrapper
        iosApp/               # SwiftUI entry point
        DeviceActivityMonitor/  # Family Controls extension
        ShieldConfig/         # Shield configuration
```

Most code lives in `commonMain`. Platform-specific code is minimal:

- **Android:** App icon theming, notification channels
- **iOS:** Native platform bindings, Family Controls integration

---

## Entities

### User

Singleton. One per install.

```kotlin
data class User(
    val id: String = "user",
    val name: String?,
    val createdAt: Instant,
    val lastSessionAt: Instant,

    // Personality scores (0-100, start at 50)
    val socialComfort: Int,
    val openness: Int,
    val playfulness: Int,
    val patience: Int,
    val reflectionDepth: Int,

    // Response style scores (0-100, start at 50)
    val writingAffinity: Int,
    val photoAffinity: Int,
    val audioAffinity: Int,
    val drawingAffinity: Int,
    val gameAffinity: Int,

    // Note: Mood tracking is derived from SessionEntity.mood
    // Use SessionDao.getRecentSessionsWithMood() to compute mood trends
    // This avoids data redundancy since each Session already stores its mood

    // Flags
    val hasCompletedOnboarding: Boolean,
    val hasBeenAskedAboutSocialSkips: Boolean,
    val hasBeenAskedForName: Boolean,           // asked on session 2-3
    val hasSeenDecliningMoodRouting: Boolean,   // one-time routing injection
    val hasSeenStopAskingRouting: Boolean,      // one-time routing injection
    val currentTaskId: String?,

    // Routing effects (temporary, from ROUTING tasks)
    val activeRoutingEffects: RoutingEffects?,

    // Stats
    val sessionsCount: Int,
    val tasksCompleted: Int,
    val tasksSkipped: Int,

    // Behavioral signals (raw counts for easter eggs & goodbye reel)
    val appOpenCount: Int,
    val settingsOpenCount: Int,
    val aboutOpenCount: Int,
    val noClickCountOnboarding: Int,
    val bugReportCount: Int,
    val permissionDenialCount: Int,
    val backButtonPressCount: Int,
    val shakeCount: Int,
    val lateNightSessionCount: Int,        // opened after midnight
    val morningSessionCount: Int,          // opened 6am-10am
    val middaySessionCount: Int,           // opened 11am-2pm
    val averageHesitationMs: Long?,
    val optionalMediaAddedCount: Int,
    val optionalMediaOpportunities: Int,   // tasks where media was optional
    val deleteAndRewriteCount: Int,
    val quickExitCount: Int,              // closed app within 10s of opening
    val idleSessionCount: Int,            // opened but didn't do anything

    // Text response stats (for personality insights)
    val textTasksCompleted: Int,
    val totalTextLength: Int,              // sum of all text responses
)

// Mood is stored per-session in SessionEntity.mood and can be queried via SessionDao
// Use SessionDao.getRecentSessionsWithMood() to get mood history when needed

enum class Mood {
    GREAT, GOOD, OKAY, LOW, BAD, COMPLICATED
}

// MoodTrend is computed from recent sessions, not stored
enum class MoodTrend {
    IMPROVING, STABLE, DECLINING, UNKNOWN
}

data class RoutingEffects(
    val preferCategory: TaskCategory?,
    val avoidCategory: TaskCategory?,
    val preferDifficulty: Difficulty?,
    val remainingTasks: Int,
)
```

---

## Behavioral Signals & Easter Eggs

The app is alive. It notices things you don't expect it to notice. We store raw counts on User and compute derived insights when needed.

### Derived Insights

Traits should feel earned, not arbitrary. We use **ratios, patterns, and sustained behavior** rather than raw counts.

```kotlin
// =========================================================================
// TIME PATTERNS - When do they show up?
// =========================================================================

/** Opens app after midnight frequently (>30% of sessions) */
val User.isNightOwl: Boolean
    get() = sessionsCount >= 5 && lateNightSessionCount.toFloat() / sessionsCount > 0.3f

/** Opens app in the morning frequently (>30% of sessions, 6am-10am) */
val User.isMorningPerson: Boolean
    get() = sessionsCount >= 5 && morningSessionCount.toFloat() / sessionsCount > 0.3f

/** Opens app around midday frequently (>30% of sessions, 11am-2pm) */
val User.isMiddayRegular: Boolean
    get() = sessionsCount >= 5 && middaySessionCount.toFloat() / sessionsCount > 0.3f

/** Returns at consistent times - computed from session timestamp clustering */
val User.isRitualistic: Boolean
    get() = /* computed: >50% of sessions within 2-hour window of each other */

// =========================================================================
// ENGAGEMENT PATTERNS - How do they interact?
// =========================================================================

/** Completes most tasks they start (>80% completion rate) */
val User.isCommitted: Boolean
    get() = tasksCompleted >= 10 &&
            tasksCompleted.toFloat() / (tasksCompleted + tasksSkipped) > 0.8f

/** Skips many tasks (>40% skip rate) - selective, not disengaged */
val User.isSelective: Boolean
    get() = tasksCompleted >= 10 &&
            tasksSkipped.toFloat() / (tasksCompleted + tasksSkipped) > 0.4f

/** Opens app but often doesn't complete tasks (>30% idle sessions) */
val User.isHesitant: Boolean
    get() = sessionsCount >= 10 &&
            idleSessionCount.toFloat() / sessionsCount > 0.3f

/** Clicked no multiple times during onboarding but eventually said yes */
val User.isReluctantAdventurer: Boolean
    get() = noClickCountOnboarding >= 3

// =========================================================================
// RESPONSE STYLE - How do they express themselves?
// =========================================================================

/** Takes time before responding (median hesitation > 20s) */
val User.isThoughtful: Boolean
    get() = (averageHesitationMs ?: 0) > 20_000

/** Tends to write long responses (average > 150 chars on text tasks) */
val User.isVerbose: Boolean
    get() = averageTextLength?.let { it > 150 } ?: false

/** Tends to write short responses (average < 30 chars on text tasks) */
val User.isConcise: Boolean
    get() = averageTextLength?.let { it in 1..30 } ?: false

/** Frequently rewrites answers before submitting */
val User.isCarefulWriter: Boolean
    get() = deleteAndRewriteCount >= 5

/** Adds optional media frequently (>40% of tasks that allow it) */
val User.isVisual: Boolean
    get() = optionalMediaOpportunities >= 5 &&
            optionalMediaAddedCount.toFloat() / optionalMediaOpportunities > 0.4f

// =========================================================================
// CURIOSITY & EXPLORATION
// =========================================================================

/** Opens settings more than typical (>1x per 5 sessions) */
val User.isCurious: Boolean
    get() = sessionsCount >= 5 &&
            settingsOpenCount.toFloat() / sessionsCount > 0.2f

/** Reads about pages */
val User.isInquisitive: Boolean
    get() = aboutOpenCount >= 2

// =========================================================================
// EXPERIENCE LEVEL
// =========================================================================

val User.isNewUser: Boolean get() = sessionsCount <= 3
val User.isRegularUser: Boolean get() = sessionsCount in 4..20
val User.isVeteranUser: Boolean get() = sessionsCount > 20
val User.hasReachedMidpoint: Boolean get() = tasksCompleted >= totalTaskCount / 2
val User.isNearingEnd: Boolean get() = tasksCompleted >= (totalTaskCount * 0.9).toInt()

// Computed from stored totals
val User.averageTextLength: Int?
    get() = if (textTasksCompleted > 0) totalTextLength / textTasksCompleted else null
```

### Revealing Observations (About You Screen)

The "About You" screen gradually reveals what the app has noticed. This makes the app feel alive without being creepy.

**Design Principles:**

- **Earned revelations** — Only show traits after enough data (using ratio-based thresholds)
- **Framed as noticing, not tracking** — "I've noticed you..." not "Data shows..."
- **Positive or neutral, never judgmental** — Observations, not evaluations
- **Builds over time** — First visit is sparse, later visits reveal more
- **Some mystery** — Don't reveal everything, leave things unsaid
- **Immediate feedback** — Name-based observations show instantly after setting name

**Name Tracking:**

User model tracks name timestamps for dynamic copy:

```kotlin
data class User(
    val name: String?,
    val nameSetAt: Long?,      // epoch millis - when name was first set
    val nameUpdatedAt: Long?,  // epoch millis - when name was last changed
    // ...
)
```

**Observation Categories:**

| Category      | Description                                   |
| ------------- | --------------------------------------------- |
| `IDENTITY`    | Name-related observations (shown immediately) |
| `TIMING`      | When you use the app (time patterns)          |
| `ENGAGEMENT`  | How you engage with tasks                     |
| `EXPRESSION`  | Your communication style                      |
| `PERSONALITY` | Things that make you you                      |

**Identity Observations (Name-based, no waiting required):**

| Condition                   | Copy Example                                                |
| --------------------------- | ----------------------------------------------------------- |
| Name just set (< 1 min)     | "Nice to officially meet you, Alex."                        |
| Name recently set (< 1 hr)  | "Alex. I like saying it."                                   |
| Name just changed (< 1 min) | "Oh, Alex now? Okay, I'll remember that."                   |
| Name changed (< 1 hr)       | "I'm still getting used to calling you Alex. It suits you." |
| Funny/playful name          | "\"Batman\"—I see you. Nice choice."                        |
| Mysterious name (initials)  | "Just J.? Keeping it mysterious. I can work with that."     |
| Full name provided          | "Alex Johnson. The full thing. I appreciate the formality." |
| Nickname (short)            | "Alex. Short, sweet. Easy to remember."                     |
| Known for > 1 week          | "I've been calling you Alex for a while now. It fits."      |
| Default                     | "Alex. I remember."                                         |

**Early Engagement Observations (no 5-session wait):**

| Condition        | Copy                                                              |
| ---------------- | ----------------------------------------------------------------- |
| 1 task completed | "You did one. That's how it starts."                              |
| 2-3 tasks        | "You've done 3 things now. I'm starting to get a sense of you."   |
| 4-9 tasks        | "We're at 7 things together. It's starting to feel like a habit." |
| 10-19 tasks      | "14 tasks. Double digits. You're sticking around."                |

**Behavioral Observations (require thresholds):**

| Trait                 | Threshold                             | How to Phrase                                                                                                    |
| --------------------- | ------------------------------------- | ---------------------------------------------------------------------------------------------------------------- |
| isNightOwl            | >30% late night sessions, 5+ sessions | "I've noticed you often come by after midnight. About X% of our time together is in the quiet hours. Night owl?" |
| isMorningPerson       | >30% morning sessions, 5+ sessions    | "You seem to be an early riser—X% of your visits happen between 6 and 10am. I like that energy."                 |
| isCommitted           | >80% completion rate, 10+ tasks       | "You finish what you start—X% completion rate. That's not nothing."                                              |
| isWordsmith           | Avg text >150 chars, 5+ text tasks    | "You have a lot to say—your responses average X characters. Words seem to come naturally to you."                |
| isBrief               | Avg text <50 chars, 5+ text tasks     | "You keep things concise. Your responses are short and to the point. Nothing wrong with that."                   |
| isVisual              | >40% media added when available       | "You like adding photos—X% of the time when you could, you did. Capturing moments matters to you."               |
| isThoughtful          | High hesitation or rewrites           | "You take your time. You think before you respond, sometimes rewrite. That care comes through."                  |
| isReluctantAdventurer | 3+ no clicks in onboarding            | "I remember you almost didn't start. You said no a few times before saying yes. I'm glad you did."               |
| isVeteran             | 50+ tasks completed                   | "We've done X things together now. That's... a lot of good times."                                               |
| socialComfort >= 70   | Personality score threshold           | "Social tasks don't seem to faze you. You dive into people-focused prompts pretty readily."                      |
| openness >= 70        | Personality score threshold           | "You seem open to trying new things. The weird prompts don't scare you off."                                     |
| playfulness >= 70     | Personality score threshold           | "There's a playfulness to how you engage. You don't take this too seriously—in the best way."                    |
| patience >= 70        | Personality score threshold           | "You have patience. The longer tasks, the ones that take a bit more effort—you stick with them."                 |
| reflectionDepth >= 70 | Personality score threshold           | "You seem to go deep. The reflective prompts seem to resonate with you more than others."                        |
| isHesitant            | Hesitant pattern detected             | "Sometimes you stop by but don't stay long. That's okay. I'm here when you're ready."                            |

**What NOT to Reveal:**

- Skip rates (feels judgmental)
- Hesitancy/idle patterns (mentioned gently but not with numbers)
- Exact raw counts ("You opened settings 47 times")
- Mood patterns (too personal to reflect back)

**Progressive Disclosure:**

The `GetUserObservationsUseCase` limits observations based on session count:

- **New users (≤3 sessions):** Max 2 gentle observations
- **Established users (4-10 sessions):** Max 5 observations
- **Veterans (10+ sessions):** All applicable observations

```kotlin
// Simplified from actual implementation
fun getObservations(user: User): List<Observation> {
    val all = buildList {
        // Identity observations (name - always first, shown immediately)
        if (user.name != null) add(identityObservation(user))

        // Early engagement observations (no threshold required)
        if (user.tasksCompleted in 1..9) add(earlyEngagementObservation(user))

        // Timing observations (night owl, morning person, etc.)
        if (user.isNightOwl) add(timingObservation("night_owl", ...))

        // Engagement observations (committed, veteran, etc.)
        if (user.isCommitted) add(engagementObservation("committed", ...))

        // Expression observations (wordsmith, visual, etc.)
        if (user.isWordsmith) add(expressionObservation("wordsmith", ...))

        // Personality observations (playful, patient, etc.)
        if (user.playfulness >= 70) add(personalityObservation("playful", ...))
    }

    val maxObservations = when {
        user.sessionsCount <= 3 -> 2
        user.sessionsCount <= 10 -> 5
        else -> all.size
    }

    return all.sortedByDescending { it.priority }.take(maxObservations)
}
```

**Screen Layout:**

The About You screen has two modes for the name section:

```
┌─────────────────────────────────────────┐
│  About You                              │
├─────────────────────────────────────────┤
│                                         │
│  ╔═══════════════════════════════════╗  │
│  ║ NAME DISPLAY (when name is set)   ║  │
│  ║                                   ║  │
│  ║ I call you                        ║  │
│  ║ Alex                      [edit]  ║  │
│  ╚═══════════════════════════════════╝  │
│                                         │
│  ╔═══════════════════════════════════╗  │
│  ║ NAME EDIT (no name or editing)    ║  │
│  ║                                   ║  │
│  ║ What should I call you?           ║  │
│  ║ (or "What should I call you       ║  │
│  ║ instead?" when changing)          ║  │
│  ║ [Name input]       [Save]         ║  │
│  ╚═══════════════════════════════════╝  │
│                                         │
│  ─────────────────────────              │
│                                         │
│  What I've noticed                      │
│                                         │
│  Alex. I like saying it.                │
│  (identity observation - immediate)     │
│                                         │
│  You did one. That's how it starts.     │
│  (early engagement - after 1 task)      │
│                                         │
│  ─────────────────────────              │
│                                         │
│  As I learn more about you—your         │
│  patterns, your preferences, what makes │
│  a good time for you—I'll add it here.  │
│                                         │
└─────────────────────────────────────────┘
```

**Empty State (New Users):**

"Not much yet. We're just getting started."

### Easter Egg Triggers

Some are one-time (show once, track with a flag). Some are recurring.

| Trigger                                       | Message/Behavior                                                                                                              | One-time?     |
| --------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------- | ------------- |
| `settingsOpenCount >= 10`                     | "Looking for something? I don't have many secrets. But I do have some."                                                       | Yes           |
| `isNightOwl`                                  | Unlocks night-only task: "We keep meeting like this."                                                                         | No (task)     |
| `noClickCountOnboarding >= 5`                 | Later in app: "You almost didn't do this, remember? I'm glad you changed your mind."                                          | Yes           |
| `backButtonPressCount >= 5` on a single task  | "I felt that. The urge to peek. I understand."                                                                                | Yes           |
| `appOpenCount == 100`                         | "This is the 100th time you've opened me. That means something. I'm not sure what."                                           | Yes           |
| `aboutOpenCount >= 1`                         | "You read my about page. Most people don't. Thank you for being curious about me."                                            | Yes           |
| Camera permission denied + photo task         | "I'd ask you to take a photo, but you told me I couldn't use your camera. Describe what you would have photographed instead." | No (adaptive) |
| 5 consecutive GOOD/GREAT moods                | "You've been doing well lately. I'm glad."                                                                                    | Yes           |
| `bugReportCount >= 1`                         | "You told me something was broken. Thank you for helping me get better."                                                      | Yes           |
| All tasks completed in <1 min                 | "You move fast. Is that how you do everything?"                                                                               | Yes           |
| `averageHesitationMs > 30000`                 | "You think before you write. I like that."                                                                                    | Yes           |
| Shake detected (non-shake task)               | "I felt that."                                                                                                                | No            |
| Open, close, reopen within 30s                | "Forgot something?"                                                                                                           | No            |
| `idleSessionCount >= 5`                       | "You keep coming back but not staying. That's okay. I'll be here."                                                            | Yes           |
| `deleteAndRewriteCount >= 3` on single prompt | "You rewrote that a lot. The first draft is usually more honest."                                                             | Yes           |
| Last 10% of tasks                             | "We're almost at the end. You know that, right?"                                                                              | Yes           |

### Easter Egg State

Track which one-time easter eggs have fired:

```kotlin
data class EasterEggState(
    val hasSeenSettingsCurious: Boolean = false,
    val hasSeenReluctantAdventurer: Boolean = false,
    val hasSeenAppOpen100: Boolean = false,
    val hasSeenAboutThankYou: Boolean = false,
    val hasSeenMoodStreakGood: Boolean = false,
    val hasSeenBugReportThankYou: Boolean = false,
    val hasSeenSpeedRunner: Boolean = false,
    val hasSeenThoughtful: Boolean = false,
    val hasSeenIdleVisitor: Boolean = false,
    val hasSeenRewriteHonesty: Boolean = false,
    val hasSeenNearingEnd: Boolean = false,
)
```

### Goodbye Reel Integration

These counts enrich the ending:

- "You hesitated before writing. Average of 12 seconds. You chose your words."
- "You almost left 3 times during onboarding. But you didn't."
- "You opened settings 34 times. I don't know what you were looking for, but I hope you found it."
- "23 of your 47 visits were after midnight."
- "You shook me 7 times. I felt every one."

---

### Session

A session represents a continuous period of app usage. Created when the app opens. Ends when:

- User explicitly closes the app
- App is backgrounded for 10+ minutes (session rolls over on next foreground)
- App is force-killed

```kotlin
data class Session(
    val id: String,
    val startedAt: Instant,
    val endedAt: Instant?,
    val mood: Mood?,                      // set via optional banner, null if dismissed
    val moodDismissed: Boolean,           // true if user X'd the mood banner
    val tasksCompleted: Int,
    val tasksSkipped: Int,
    val previousSessionId: String?,       // for detecting session rollover
)
```

Session lifecycle:

```kotlin
class SessionManager(
    private val sessionRepository: SessionRepository,
    private val clock: Clock,
) {
    private val sessionTimeoutMs = 10.minutes.inWholeMilliseconds

    fun onAppForegrounded() {
        val lastSession = sessionRepository.getLatestSession()
        val now = clock.now()

        val shouldCreateNew = lastSession == null
            || lastSession.endedAt != null
            || (now - lastSession.startedAt).inWholeMilliseconds > sessionTimeoutMs

        if (shouldCreateNew) {
            sessionRepository.endSession(lastSession?.id)
            sessionRepository.createSession(
                Session(
                    id = uuid(),
                    startedAt = now,
                    previousSessionId = lastSession?.id,
                )
            )
        }
    }

    fun onAppBackgrounded() {
        // Don't end session immediately — let the timeout handle it
        // This way quick app switches don't create new sessions
    }
}
```

### Mood Prompt Rules

The mood prompt appears as a bottom sheet when conditions are met. We don't want to annoy the user.

**Show mood prompt if ALL of these are true:**

- Session has no mood set (`session.mood == null`)
- User hasn't dismissed the mood prompt this session
- Mood banner isn't permanently disabled by user
- At least 6 hours have passed since last mood interaction (set or dismiss)
- Haven't shown the prompt yet this session

The 6-hour gap ensures we don't repeatedly ask across quick successive sessions.

```kotlin
val shouldShowMoodPrompt = session != null
    && session.mood == null
    && !session.moodDismissed
    && !moodBannerDisabled
    && !hasShownMoodPromptThisSession
    && (lastMoodInteractionAt == null || timeSinceLastInteraction >= 6.hours)
```

**Settings Screen Mood Entry:**

The Settings screen can show a manual mood entry option when `canAnswerMood` is true. The supporting text is context-aware:

| Context         | Copy                                   |
| --------------- | -------------------------------------- |
| First time ever | "I'd like to know"                     |
| Returning user  | "You haven't told me this session yet" |

The toggle for mood checks also has playful copy that evolves with repeated toggles:

| Toggle Count | When Off                              | When On                                         |
| ------------ | ------------------------------------- | ----------------------------------------------- |
| 0-1          | "I won't ask. But I'm still curious." | "I'll check in at the start of each session..." |
| 2-3          | "Okay okay, I'll stop asking..."      | "Welcome back! Missed you..."                   |
| 4-5          | "You keep toggling me. Very weird..." | "Changed your mind again?..."                   |
| 6+           | Increasingly meta commentary          | ...escalating playfulness                       |

---

### Task

The static definition of a task. Shipped as JSON, inserted into DB on first launch.

```kotlin
data class Task(
    val id: String,
    val type: TaskType,
    val categories: List<TaskCategory>,  // a task can be both SOCIAL and REFLECTION
    val difficulty: Difficulty,

    // Selection criteria
    val requiresSocial: Boolean,
    val bestForMoods: List<Mood>?,
    val avoidForMoods: List<Mood>?,
    val minimumScores: Map<ScoreDimension, Int>?,
    val conditions: TaskConditions?,  // time/mood gating

    // What kind of response this task accepts
    val responseStyle: ResponseStyle,

    // For the goodbye reel
    val safeToReflect: Boolean,

    // Presentation
    val instruction: String,
    val assets: TaskAssets?,
)

data class ResponseStyle(
    val allowsText: Boolean = false,
    val allowsPhoto: Boolean = false,
    val allowsAudio: Boolean = false,
    val allowsDrawing: Boolean = false,
)

data class TaskConditions(
    val timeAfter: LocalTime?,           // only show after this time (e.g., 22:00)
    val timeBefore: LocalTime?,          // only show before this time (e.g., 05:00)
    val minDaysSinceLastSession: Int?,   // only show if they've been away
    val requiresMoodTrend: MoodTrend?,   // only show if mood trending this way
)

data class TaskAssets(
    val imagePath: String?,
    val backgroundImagePath: String?,
    val accentColor: String?,  // hex
)

enum class TaskType {
    /** Text input with optional media */
    PROMPT,
    /** Freeform drawing canvas */
    DRAWING,
    /** Take a photo */
    PHOTO_CAPTURE,
    /** Voice recording */
    AUDIO_CAPTURE,
    /** Pick from options (single or multi) */
    SELECTION,
    /** "Go do this" with completion button */
    INSTRUCTION,
    /** Mood/preference questions for routing */
    ROUTING,
    /** Hold still challenge (accelerometer) */
    STILLNESS,
    /** Keep finger on screen challenge */
    HOLD_FINGER,
    /** Countdown timer challenge */
    WAIT_TIMER,
    /** Time-locked content */
    DONT_OPEN_UNTIL,
    /** Custom game (routes by task.id) */
    GAME,
}

enum class TaskCategory {
    SOCIAL,
    REFLECTION,
    PLAY,
    ACTION,
    STILLNESS,
    DISCOMFORT,
}

enum class Difficulty {
    LIGHT, MEDIUM, HEAVY
}
```

#### Task Storage Strategy

Tasks are shipped as a `tasks.json` asset and inserted into the database on first launch. We use a single table with nullable columns for type-specific fields. This keeps everything queryable without JSON parsing at query time.

```kotlin
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey
    val id: String,
    val type: TaskType,
    val categories: String,         // JSON list: ["SOCIAL", "REFLECTION"]
    val difficulty: Difficulty,
    val requiresSocial: Boolean,
    val bestForMoods: String?,      // JSON list
    val avoidForMoods: String?,     // JSON list
    val minimumScores: String?,     // JSON map
    val safeToReflect: Boolean,
    val instruction: String,

    // Response style (queryable for affinity matching)
    val allowsText: Boolean,
    val allowsPhoto: Boolean,
    val allowsAudio: Boolean,
    val allowsDrawing: Boolean,

    // Conditions (for time/mood gating)
    val conditionTimeAfter: String?,     // "22:00"
    val conditionTimeBefore: String?,    // "05:00"
    val conditionMinDaysAway: Int?,
    val conditionMoodTrend: String?,     // "DECLINING"

    // Assets
    val imagePath: String?,
    val backgroundImagePath: String?,
    val accentColor: String?,

    // Type-specific fields
    val placeholder: String?,            // PROMPT
    val routingOptions: String?,         // ROUTING - JSON blob
    val selectionOptions: String?,       // SELECTION - JSON list
    val minSelections: Int?,             // SELECTION
    val maxSelections: Int?,             // SELECTION
    val followUp: String?,               // FollowUpConfig - JSON blob
    val durationSeconds: Int?,           // timed types, STILLNESS, AUDIO_CAPTURE
    val requireFrontCamera: Boolean?,    // PHOTO_CAPTURE
)
```

#### Example Task JSON

Tasks in `tasks.json` look like this (shape varies by type):

```json
[
  {
    "id": "draw_turtle_dark",
    "type": "DRAWING",
    "categories": ["PLAY"],
    "difficulty": "LIGHT",
    "requiresSocial": false,
    "bestForMoods": ["OKAY", "GOOD", "GREAT"],
    "avoidForMoods": ["BAD"],
    "safeToReflect": false,
    "responseStyle": { "allowsDrawing": true },
    "instruction": "Go to the darkest place you can find. Turn off all the lights. Draw a turtle. I hope it's bad."
  },
  {
    "id": "used_to_be",
    "type": "PROMPT",
    "categories": ["REFLECTION"],
    "difficulty": "MEDIUM",
    "requiresSocial": false,
    "bestForMoods": ["OKAY", "GOOD", "COMPLICATED"],
    "avoidForMoods": ["BAD"],
    "safeToReflect": true,
    "responseStyle": {
      "allowsText": true,
      "allowsPhoto": true,
      "allowsAudio": true
    },
    "instruction": "Finish this sentence: I used to be...",
    "placeholder": "I used to be..."
  },
  {
    "id": "question_game_stranger",
    "type": "GAME",
    "categories": ["SOCIAL", "REFLECTION"],
    "difficulty": "HEAVY",
    "requiresSocial": true,
    "safeToReflect": false,
    "responseStyle": {},
    "instruction": "Find someone you don't know. Play the question game. You go first."
  },
  {
    "id": "whats_keeping_you_up",
    "type": "PROMPT",
    "categories": ["REFLECTION"],
    "difficulty": "MEDIUM",
    "requiresSocial": false,
    "safeToReflect": true,
    "responseStyle": { "allowsText": true, "allowsAudio": true },
    "conditions": {
      "timeAfter": "22:00",
      "timeBefore": "05:00"
    },
    "instruction": "You're here late. What's keeping you up?"
  },
  {
    "id": "hold_finger_30",
    "type": "HOLD_FINGER",
    "categories": ["STILLNESS"],
    "difficulty": "LIGHT",
    "requiresSocial": false,
    "safeToReflect": false,
    "responseStyle": {},
    "durationSeconds": 30,
    "instruction": "Hold your finger here. Don't let go."
  },
  {
    "id": "mood_check_routing",
    "type": "ROUTING",
    "categories": ["REFLECTION"],
    "difficulty": "LIGHT",
    "requiresSocial": false,
    "safeToReflect": false,
    "responseStyle": {},
    "conditions": {
      "requiresMoodTrend": "DECLINING"
    },
    "instruction": "I've noticed things have been heavy lately. What do you want right now?",
    "routingOptions": [
      {
        "text": "Something light",
        "preferCategory": "PLAY",
        "avoidCategory": "REFLECTION",
        "preferDifficulty": "LIGHT",
        "effectDuration": 3
      },
      {
        "text": "I want to sit with it",
        "preferCategory": "REFLECTION",
        "preferDifficulty": "MEDIUM",
        "effectDuration": 3
      }
    ]
  },
  {
    "id": "compliment_stranger",
    "type": "INSTRUCTION",
    "categories": ["SOCIAL", "DISCOMFORT"],
    "difficulty": "MEDIUM",
    "requiresSocial": true,
    "bestForMoods": ["GOOD", "GREAT"],
    "safeToReflect": false,
    "responseStyle": {},
    "instruction": "Give a genuine compliment to someone you don't know. It doesn't have to be big. It just has to be true.",
    "followUp": {
      "type": "DID_YOU",
      "ifYes": { "type": "FEELING" },
      "options": [
        { "id": "not_ready", "text": "Not today", "reschedule": true },
        { "id": "not_for_me", "text": "Not my thing", "skipPermanent": true }
      ]
    }
  }
]
```

---

### Supporting Types

```kotlin
data class SelectionOption(
    val text: String,
    val signals: List<Signal>?,
)

/**
 * Configuration for post-task follow-up flows.
 * Any task can have a follow-up to gather more context about the experience.
 */
data class FollowUpConfig(
    val type: FollowUpType,
    val required: Boolean = false,  // If false, app decides based on context
    val ifYes: FollowUpConfig? = null,  // For DID_YOU type
    val options: List<FollowUpOption>? = null,
)

data class FollowUpOption(
    val id: String,
    val text: String,
    val reschedule: Boolean = false,
    val skipPermanent: Boolean = false,
)

enum class FollowUpType {
    /** "How did that feel?" - Shows a feeling scale */
    FEELING,
    /** "Did you actually do it?" - Yes/No with options if No */
    DID_YOU,
    /** "Any thoughts on that?" - Optional text reflection */
    REFLECTION,
    /** Task-specific options defined in followUp.options */
    CUSTOM,
}

data class RoutingOption(
    val text: String,
    val preferCategory: TaskCategory?,
    val avoidCategory: TaskCategory?,
    val preferDifficulty: Difficulty?,
    val effectDuration: Int,
)
```

---

### TaskProgress

Tracks user's status on each task.

```kotlin
data class TaskProgress(
    val taskId: String,
    val status: TaskStatus,
    val attempts: Int,
    val lastAttemptAt: Instant?,
    val completedAt: Instant?,
    val rescheduledUntil: Instant?,
)

enum class TaskStatus {
    LOCKED,
    AVAILABLE,
    CURRENT,
    COMPLETED,
    SKIPPED_PERMANENT,
    SKIPPED_RESCHEDULE,
}
```

---

### TaskResult

What the user actually did. Multiple can exist per task (retries, "maybe later" reschedules).

```kotlin
data class TaskResult(
    val id: String,
    val taskId: String,
    val sessionId: String,
    val attemptNumber: Int,
    val completedAt: Instant,
    val timeSpentMs: Long,
    val outcome: TaskOutcome,
    val response: TaskResponse?,
    val signals: List<Signal>,
)

enum class TaskOutcome {
    COMPLETED,
    SKIPPED_PERMANENT,
    SKIPPED_RESCHEDULE,
}

sealed class TaskResponse {
    data class Text(val value: String) : TaskResponse()
    data class Selection(val selected: String) : TaskResponse()
    data class MultiSelect(val selected: List<String>) : TaskResponse()
    data class Numeric(val value: Int) : TaskResponse()
    data class Media(val type: MediaType, val filePath: String) : TaskResponse()
    data class Drawing(val filePath: String) : TaskResponse()
    data class Compound(val text: String?, val media: List<Media>?) : TaskResponse()
    data class Routing(val option: RoutingOption) : TaskResponse()  // for ROUTING tasks
    object None : TaskResponse()
}

/**
 * Data captured from follow-up flows after task completion.
 */
data class FollowUpResult(
    val didComplete: Boolean?,      // from DID_YOU
    val feelingScore: Int?,         // from FEELING (1-5)
    val reflection: String?,        // from REFLECTION
    val selectedOptionId: String?,  // from CUSTOM or DID_YOU no options
)

// Convert routing response to immediate effect
fun TaskResponse.Routing.toRoutingResult(): RoutingResult = RoutingResult(
    preferCategory = option.preferCategory,
    avoidCategory = option.avoidCategory,
    preferDifficulty = option.preferDifficulty,
    effectDuration = option.effectDuration,
)

enum class MediaType {
    PHOTO, AUDIO, VIDEO
}

data class Signal(
    val dimension: ScoreDimension,
    val adjustment: Int,
    val reason: String?,
)

enum class ScoreDimension {
    // Personality
    SOCIAL_COMFORT,
    OPENNESS,
    PLAYFULNESS,
    PATIENCE,
    REFLECTION_DEPTH,

    // Response style
    WRITING_AFFINITY,
    PHOTO_AFFINITY,
    AUDIO_AFFINITY,
    DRAWING_AFFINITY,
    GAME_AFFINITY,
}
```

---

## Task Reaction System

Reactions are brief, surprising moments where the app comments on something specific about how the user completed a task. They make the app feel alive without being annoying.

### Reactions vs Follow-Ups

These are conceptually different systems:

| System        | Purpose                      | Example                               |
| ------------- | ---------------------------- | ------------------------------------- |
| **Follow-Up** | Verification/accountability  | "Did you actually do it?"             |
| **Reaction**  | Commentary on their behavior | "A person of few words. Interesting." |

Follow-ups are task-defined. Reactions are computed holistically by the `TaskReactionEngine`.

### Flow

```
User completes task
    │
    ▼
TaskCompletionResult created (signals, response, time spent)
    │
    ▼
TaskReactionEngine.considerReaction(result, context)
    │
    ▼
Returns Reaction? (nullable, ~15% base probability)
    │
    ├─ null → Proceed directly to next task
    └─ Reaction → Show reaction overlay
                   │
                   ▼
              User taps to dismiss
                   │
                   ▼
              Load next task
```

### Reaction Model

```kotlin
data class Reaction(
    val message: String,
    val style: ReactionStyle,
    val followUpPrompt: String? = null,  // optional "Tell me more?"
)

enum class ReactionStyle {
    /** Quick witty remark ("A person of few words.") */
    QUIP,
    /** Observational comment ("You think before you write.") */
    OBSERVATION,
    /** Rhetorical question ("Is that how you do everything?") */
    QUESTION,
    /** Acknowledgment ("That one felt different, didn't it?") */
    ACKNOWLEDGMENT,
}
```

### ReactionContext

The engine needs context to make good decisions:

```kotlin
data class ReactionContext(
    val task: Task,
    val sessionNumber: Int,
    val tasksCompletedThisSession: Int,
    val totalTasksCompleted: Int,
    val currentMood: Mood?,
    val isLateNight: Boolean,
    val isFirstSession: Boolean,
    val recentReactionIds: List<String>,  // avoid repetition
)
```

### Probability Adjustments

Base probability: ~15%

| Condition                          | Adjustment |
| ---------------------------------- | ---------- |
| Notable response (very short/long) | +15%       |
| Late night session                 | +5%        |
| User in BAD/LOW mood               | -10%       |
| Many tasks completed this session  | -5%        |

Final probability clamped to 5%-35%.

### Reaction Types

Reactions are generated based on:

1. **Response characteristics**
   - Very short text (<15 chars): "A person of few words. Interesting."
   - Very long text (>250 chars): "Do all humans talk this much? Interesting."
   - Thoughtful length: "You thought about that one."

2. **Time spent**
   - Very quick (<8s for prompt): "You move fast. Is that how you do everything?"
   - Very long (>3min): "You took your time. I noticed."

3. **Context**
   - Late night: "It's late. You're still here."
   - Milestones: "That's 10. We're getting somewhere."

4. **Signals captured**
   - High writing affinity: "You like to write. I can tell."
   - Hesitancy signal: "You think before you write. I like that."

### Design Principles

- **Low frequency**: Most completions don't get reactions (~85%)
- **Non-repetitive**: Track recently used reaction IDs
- **Mood-aware**: Back off when user is struggling
- **Never judgmental about skips**: Only react to completed tasks
- **Let them settle in**: Don't react in first session
- **Genuine**: Never sarcastic about real struggles

---

## Home Screen Flow & Task Loading

The home screen is the app's main hub. It shows the current task (or loads the next one) and handles the interstitial routing flow.

### Task Flow FSM

The home screen uses a finite state machine for task flow:

```
[Loading] → [ShowingTask] → (task completed) → [ShowingReaction]? → [Loading] → ...
                 ↑                                       |
                 |_______________________________________|
                           (reaction dismissed)
```

Reactions are inline state, not navigation destinations. This keeps the flow simple and prevents "dialog-on-dialog" issues.

```kotlin
sealed class TaskFlowState {
    /**
     * Loading the next task - shows a "thinking" message to make it feel alive.
     * The message changes based on context to feel dynamic, not mechanical.
     */
    data class Loading(val thinkingMessage: String = "...") : TaskFlowState()

    /** Displaying a task for the user to complete */
    data class ShowingTask(val task: Task) : TaskFlowState()

    /** Showing a reaction to the user's task completion */
    data class ShowingReaction(val reaction: Reaction) : TaskFlowState()
}
```

The `HomeScreen` composable uses `AnimatedContent` to smoothly transition between states.

---

## Thinking Messages System

Between tasks, the app shows contextual "thinking" messages that make transitions feel alive. This prevents the app from feeling like a mechanical rules-based system.

### ThinkingMessageProvider

Generates contextual messages based on session state:

```kotlin
data class ThinkingContext(
    val isFirstTask: Boolean = false,
    val justCompletedTask: Boolean = false,
    val justSkipped: Boolean = false,
    val consecutiveSkips: Int = 0,
    val tasksCompletedThisSession: Int = 0,
    val currentMood: Mood? = null,
    val isLateNight: Boolean = false,
    val sessionNumber: Int = 1,
)
```

### Message Categories

**First Task of Session:**

- "Let's see...", "Thinking...", "Where to start..."
- Late night additions: "Late night thoughts...", "Quiet hours..."
- Returning users: "Welcome back...", "You again..."

**After Skipping:**

- "Okay, something else...", "Fair enough...", "Moving on..."
- Multiple skips: "Looking for the right one...", "Trying something different..."

**After Completion:**

- "Okay...", "Got it...", "Noted...", "Interesting..."
- Good mood: "Good energy...", "Let's keep going..."
- Low mood: "Something lighter...", "Easy one..."

### Timing

The delay varies to feel organic:

- **After skips:** 800ms - 1200ms (faster, acknowledging they want to move on)
- **After completions:** 1200ms - 2000ms (slower, feeling more considered)
- **Random variance:** Added to prevent predictability

### UI

The thinking message appears centered, in secondary text color, subtle but present. It's not a loading spinner - it's a pause between thoughts.

---

### HomeViewModel

```kotlin
class HomeViewModel(
    private val selectNextTaskUseCase: SelectNextTaskUseCase,
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository,
    private val clock: Clock,
) : ViewModel() {

    data class State(
        val currentTask: Task?,
        val showMoodBanner: Boolean,
        val showNamePrompt: Boolean,
        val isLoading: Boolean,
        val routingResult: RoutingResult?,  // when a ROUTING task was just completed
    )

    private val _state = MutableStateFlow(State(isLoading = true))
    val state: StateFlow<State> = _state

    init {
        checkInitialState()
    }

    private fun checkInitialState() {
        viewModelScope.launch {
            val user = userRepository.getUser()
            val session = sessionRepository.getCurrentSession()

            _state.update {
                it.copy(
                    showMoodBanner = session.mood == null && !session.moodDismissed,
                    showNamePrompt = shouldPromptForName(user),
                )
            }

            // If user has a current task in progress, resume it
            // Otherwise load next
            val currentTaskId = user.currentTaskId
            if (currentTaskId != null) {
                loadTask(currentTaskId)
            } else {
                loadNextTask(routingResult = null)
            }
        }
    }

    private fun shouldPromptForName(user: User): Boolean {
        // Ask for name on session 2 or 3, never on first session
        // and only once
        return user.name == null
            && user.sessionsCount in 2..3
            && !user.hasBeenAskedForName
    }

    fun loadNextTask(routingResult: RoutingResult? = null) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, routingResult = routingResult) }

            val nextTask = selectNextTaskUseCase.execute(routingResult)
            userRepository.setCurrentTaskId(nextTask.id)

            _state.update { it.copy(currentTask = nextTask, isLoading = false) }
        }
    }

    fun onTaskCompleted(result: TaskResult) {
        viewModelScope.launch {
            userRepository.clearCurrentTaskId()

            // If the completed task was a ROUTING task, extract the routing result
            // and pass it to the next task selection
            val routingResult = if (result.response is TaskResponse.Routing) {
                result.response.toRoutingResult()
            } else {
                null
            }

            loadNextTask(routingResult)
        }
    }

    fun onMoodSelected(mood: Mood) {
        viewModelScope.launch {
            sessionRepository.setMood(mood)
            _state.update { it.copy(showMoodBanner = false) }
        }
    }

    fun onMoodDismissed() {
        viewModelScope.launch {
            sessionRepository.setMoodDismissed(true)
            _state.update { it.copy(showMoodBanner = false) }
        }
    }

    fun onNameProvided(name: String) {
        viewModelScope.launch {
            userRepository.setName(name)
            userRepository.setHasBeenAskedForName(true)
            _state.update { it.copy(showNamePrompt = false) }
        }
    }

    fun onNamePromptDismissed() {
        viewModelScope.launch {
            userRepository.setHasBeenAskedForName(true)
            _state.update { it.copy(showNamePrompt = false) }
        }
    }
}

data class RoutingResult(
    val preferCategory: TaskCategory?,
    val avoidCategory: TaskCategory?,
    val preferDifficulty: Difficulty?,
    val effectDuration: Int,
)
```

### Interstitial Task Flow

Some tasks (ROUTING type) affect what comes next. The flow is:

```
User completes ROUTING task
       │
       ▼
HomeViewModel receives TaskResult with RoutingResult
       │
       ▼
loadNextTask(routingResult) is called
       │
       ▼
SelectNextTaskUseCase uses routingResult to influence selection
       │
       ▼
User also gets RoutingEffects stored on User entity
(for effects lasting multiple tasks)
       │
       ▼
Next task is displayed
```

The routing can come from two places:

1. **Explicit ROUTING tasks** — "I've noticed things have been heavy. What do you want?"
2. **Algorithm-injected interstitials** — When the algorithm detects a pattern (e.g., declining mood trend), it can inject a ROUTING task into the queue

---

## Screen Architecture

### Design Philosophy

The app has ~200-300 tasks. Some can share screens (prompts, photos), others need custom implementations (games, touch challenges). The architecture supports both:

1. **Template Screens** — Reusable composables configured by Task JSON
2. **Custom Screens** — Unique implementations for games, challenges, special moments
3. **Follow-Up Flows** — Post-completion interactions that can attach to ANY task

Expect roughly 50/50 split between template and custom screens.

### Screen Categories

| Category      | Examples                                 | Approach                          |
| ------------- | ---------------------------------------- | --------------------------------- |
| **Template**  | Prompt, Photo, Audio, Drawing, Selection | Reusable, configured by Task JSON |
| **Challenge** | Hold Finger, Stillness, Wait Timer       | Custom (touch/sensor logic)       |
| **Game**      | Ball in Hole, Memory, etc.               | Fully custom each                 |
| **Special**   | Multi-input prompts, branching           | Custom when templates don't fit   |

### TaskType → Screen Mapping

| TaskType        | Screen                | Template? | Notes                               |
| --------------- | --------------------- | --------- | ----------------------------------- |
| PROMPT          | `PromptScreen`        | ✓         | Text input + optional media         |
| DRAWING         | `DrawingScreen`       | ✓         | Canvas with optional background     |
| PHOTO_CAPTURE   | `PhotoCaptureScreen`  | ✓         | Front/back camera configurable      |
| AUDIO_CAPTURE   | `AudioCaptureScreen`  | ✓         | Voice recording with timer          |
| SELECTION       | `SelectionScreen`     | ✓         | Single or multi-select from options |
| INSTRUCTION     | `InstructionScreen`   | ✓         | "Go do this" with completion button |
| ROUTING         | `RoutingScreen`       | ✓         | Mood/preference questions           |
| HOLD_FINGER     | `HoldFingerScreen`    | ✗         | Custom touch detection              |
| STILLNESS       | `StillnessScreen`     | ✗         | Custom accelerometer challenge      |
| WAIT_TIMER      | `WaitTimerScreen`     | ✗         | Custom countdown with anticipation  |
| DONT_OPEN_UNTIL | `DontOpenUntilScreen` | ✗         | Custom time-locked challenge        |
| GAME            | Route by `task.id`    | ✗         | Each game is fully custom           |

### Follow-Up System

Instead of CONFIRMATION being a separate TaskType, follow-ups attach to ANY task completion:

```
User completes task
    │
    ▼
Task has followUp config?
    │
    ├─ No  → Save result, move to next task
    └─ Yes → Is followUp.required?
              │
              ├─ Yes → Show follow-up flow
              └─ No  → Should show based on context?
                       (completion time, mood, session length, etc.)
                        │
                        ├─ No  → Save result, move to next task
                        └─ Yes → Show follow-up flow
                                  │
                                  ├─ FollowUp.FEELING → "How did that feel?" (scale)
                                  ├─ FollowUp.DID_YOU → "Did you actually do it?" (yes/no/later)
                                  ├─ FollowUp.REFLECTION → "Any thoughts?" (optional text)
                                  └─ FollowUp.CUSTOM → Task-specific options
                                  │
                                  ▼
                              Save result with follow-up data
```

Follow-up config in Task JSON:

```json
{
  "id": "compliment_stranger",
  "type": "INSTRUCTION",
  "instruction": "Find a stranger. Give them a genuine compliment.",
  "followUp": {
    "type": "DID_YOU",
    "required": false,
    "ifYes": { "type": "FEELING", "required": true },
    "options": [
      { "id": "not_ready", "text": "Not ready yet", "reschedule": true },
      { "id": "not_for_me", "text": "Not my thing", "skipPermanent": true }
    ]
  }
}
```

### CopyContext Integration

Every screen has two types of copy:

1. **Task instruction** — Static from JSON, the actual task
2. **Ambient copy** — Dynamic using `CopyContext`, makes the app feel alive

Ambient copy appears in:

- Intro text before showing the task ("Another one for you...")
- Encouragement during long tasks
- Transition text after completion ("Nice. That one felt different.")

```kotlin
// In a ViewModel
val introCopy: String = when {
    copyContext.isLateNight && copyContext.isNightOwl ->
        "We keep meeting like this."
    copyContext.isNewUser ->
        "Here's your next page."
    copyContext.hasBeenHereAWhile ->
        "Still here? Good. This one's worth it."
    else ->
        "Ready for another?"
}
```

### ViewModel Architecture

```kotlin
/**
 * Base class for all task ViewModels.
 * Handles timing, signals, completion flow, and result saving.
 */
abstract class BaseTaskViewModel<S : TaskState, E : TaskEvent, A : TaskAction>(
    protected val task: Task,
    protected val taskRepository: TaskRepository,
    protected val userRepository: UserRepository,
    protected val sessionRepository: SessionRepository,
    protected val getCopyContext: GetCopyContextUseCase,
) : ViewModel() {

    protected val startedAt: Instant = Clock.System.now()

    abstract val state: StateFlow<S>

    // Subclasses compute signals based on user behavior
    abstract fun computeSignals(): List<Signal>

    // Common completion flow
    protected suspend fun complete(
        response: TaskResponse?,
        outcome: TaskOutcome,
        followUpData: FollowUpResult? = null,
    ) {
        val timeSpentMs = Clock.System.now().toEpochMilliseconds() - startedAt.toEpochMilliseconds()

        val result = TaskResult(
            id = uuid(),
            taskId = task.id,
            sessionId = sessionRepository.currentSession.value?.id,
            attemptNumber = taskRepository.getAttemptCount(task.id) + 1,
            completedAt = Clock.System.now(),
            timeSpentMs = timeSpentMs,
            outcome = outcome,
            response = response,
            signals = computeSignals(),
            followUpData = followUpData,
        )

        taskRepository.saveResult(result)
        userRepository.onTaskCompleted(
            taskId = task.id,
            signals = result.signals,
            responseTimeMs = timeSpentMs,
            characterCount = (response as? TaskResponse.Text)?.text?.length,
        )

        if (outcome == TaskOutcome.SKIPPED_RESCHEDULE) {
            taskRepository.reschedule(task.id)
        }
    }
}

// Base interfaces for type-safe state/events/actions
interface TaskState {
    val isLoading: Boolean
}

interface TaskEvent

interface TaskAction
```

Example template ViewModel:

```kotlin
class PromptViewModel(
    task: Task,
    // ... dependencies
) : BaseTaskViewModel<PromptState, PromptEvent, PromptAction>(task, ...) {

    private val _state = MutableStateFlow(PromptState(
        instruction = task.instruction,
        placeholder = task.placeholder ?: "",
        allowsPhoto = task.responseStyle.allowsPhoto,
        allowsAudio = task.responseStyle.allowsAudio,
    ))
    override val state = _state.asStateFlow()

    override fun computeSignals(): List<Signal> {
        val text = _state.value.text
        val hesitationMs = _state.value.firstKeystrokeAt?.let {
            it - startedAt.toEpochMilliseconds()
        }

        return buildList {
            // Writing affinity signal based on length
            if (text.length > 200) add(Signal(WRITING_AFFINITY, +2))
            if (text.length < 20) add(Signal(WRITING_AFFINITY, -1))

            // Thoughtfulness from hesitation
            if (hesitationMs != null && hesitationMs > 30_000) {
                add(Signal(REFLECTION_DEPTH, +1))
            }
        }
    }

    fun onAction(action: PromptAction) {
        when (action) {
            is PromptAction.UpdateText -> {
                if (_state.value.firstKeystrokeAt == null) {
                    _state.update { it.copy(firstKeystrokeAt = Clock.System.now().toEpochMilliseconds()) }
                }
                _state.update { it.copy(text = action.text) }
            }
            is PromptAction.Submit -> viewModelScope.launch {
                complete(TaskResponse.Text(_state.value.text), TaskOutcome.COMPLETED)
            }
        }
    }
}

data class PromptState(
    val instruction: String,
    val placeholder: String,
    val text: String = "",
    val allowsPhoto: Boolean = false,
    val allowsAudio: Boolean = false,
    val firstKeystrokeAt: Long? = null,
    override val isLoading: Boolean = false,
) : TaskState
```

Example custom ViewModel:

```kotlin
class HoldFingerViewModel(
    task: Task,
    // ... dependencies
) : BaseTaskViewModel<HoldFingerState, HoldFingerEvent, HoldFingerAction>(task, ...) {

    private val requiredDurationMs = (task.durationSeconds ?: 30) * 1000L
    private var fingerDownAt: Long? = null
    private var liftCount = 0

    override fun computeSignals(): List<Signal> = buildList {
        // Patience signal based on how many times they lifted
        if (liftCount == 0) add(Signal(PATIENCE, +3))
        else if (liftCount <= 2) add(Signal(PATIENCE, +1))
        else add(Signal(PATIENCE, -1))
    }

    fun onFingerDown() {
        fingerDownAt = Clock.System.now().toEpochMilliseconds()
        // Start progress animation
    }

    fun onFingerUp() {
        val downAt = fingerDownAt ?: return
        val heldMs = Clock.System.now().toEpochMilliseconds() - downAt

        if (heldMs >= requiredDurationMs) {
            // Success!
            viewModelScope.launch { complete(null, TaskOutcome.COMPLETED) }
        } else {
            // Lifted too early
            liftCount++
            fingerDownAt = null
            _state.update { it.copy(progress = 0f, message = getEncouragementCopy()) }
        }
    }
}
```

### Directory Structure

```
features/tasks/
├── api/                          # Public module interface
│   └── TaskNavigation.kt
│
├── impl/                         # Implementation module
│   ├── base/
│   │   ├── BaseTaskViewModel.kt
│   │   ├── TaskState.kt
│   │   ├── TaskEvent.kt
│   │   ├── TaskAction.kt
│   │   └── FollowUpSheet.kt      # Renders any follow-up flow
│   │
│   ├── templates/                # Reusable screens
│   │   ├── prompt/
│   │   │   ├── PromptScreen.kt
│   │   │   └── PromptViewModel.kt
│   │   ├── photo/
│   │   │   ├── PhotoCaptureScreen.kt
│   │   │   └── PhotoCaptureViewModel.kt
│   │   ├── audio/
│   │   ├── drawing/
│   │   ├── selection/
│   │   ├── instruction/
│   │   └── routing/
│   │
│   ├── challenges/               # Custom challenge screens
│   │   ├── holdFinger/
│   │   ├── stillness/
│   │   ├── waitTimer/
│   │   └── dontOpenUntil/
│   │
│   ├── games/                    # Each game is unique
│   │   ├── ballInHole/
│   │   ├── memoryGame/
│   │   └── ...
│   │
│   ├── special/                  # One-off special moments
│   │   └── ...
│   │
│   ├── components/               # Shared UI components
│   │   ├── TaskInstructionCard.kt
│   │   ├── TaskProgressIndicator.kt
│   │   ├── FollowUpOptionButton.kt
│   │   └── AmbientCopyText.kt    # Animated "alive" text
│   │
│   └── copy/
│       └── TaskCopy.kt           # CopyContext-aware copy for tasks
```

---

## Data Initialization

On first launch, we load `tasks.json` from assets, convert to entities, and insert into the database. This gives us a single source of truth—everything reads from Room.

```kotlin
class AppInitializer(
    private val context: Context,
    private val database: GoodTimesDatabase,
    private val json: Json,
) {
    suspend fun initializeIfNeeded() {
        val taskCount = database.taskDao().getCount()
        if (taskCount > 0) return // already initialized

        val tasksJson = context.assets.open("tasks.json").bufferedReader().readText()
        val tasks: List<Task> = json.decodeFromString(tasksJson)

        database.withTransaction {
            database.taskDao().insertAll(tasks.map { it.toEntity() })
            database.taskProgressDao().insertAll(
                tasks.map { TaskProgressEntity(taskId = it.id, status = LOCKED) }
            )
        }
    }
}
```

For app updates that ship new tasks:

```kotlin
suspend fun migrateTasksIfNeeded() {
    val tasksJson = context.assets.open("tasks.json").bufferedReader().readText()
    val shippedTasks: List<Task> = json.decodeFromString(tasksJson)
    val existingIds = database.taskDao().getAllIds().toSet()

    val newTasks = shippedTasks.filter { it.id !in existingIds }

    if (newTasks.isNotEmpty()) {
        database.withTransaction {
            database.taskDao().insertAll(newTasks.map { it.toEntity() })
            database.taskProgressDao().insertAll(
                newTasks.map { TaskProgressEntity(taskId = it.id, status = LOCKED) }
            )
        }
    }
}
```

---

## Data Flow

```
App Launch (or Foreground after 10+ min)
    │
    ▼
SessionManager.onAppForegrounded()
    │
    ├─ Creates new Session if needed (10 min timeout)
    └─ Increments user.appOpenCount
    │
    ▼
HomeViewModel.checkInitialState()
    │
    ├─ Show mood banner? (see Mood Prompt Rules below)
    ├─ Show name prompt? (sessions 2-3, not yet asked)
    └─ Load current or next task
    │
    ▼
SelectNextTaskUseCase.execute(routingResult?)
    │
    ├─ Data Sources:
    │   - UserRepository (scores, mood trend, routing effects, flags)
    │   - TaskRepository (definitions, progress, recent results)
    │   - SessionRepository (current session mood, timestamps)
    │   - Clock (for time-gated tasks)
    │   - routingResult param (from just-completed ROUTING task)
    │
    ├─ Mode:
    │   - If tasksCompleted < 15 → Fixed sequence
    │   - Else → Adaptive algorithm
    │
    └─ May inject ROUTING interstitial (declining mood, frequent reschedules)
    │
    ▼
Navigate to TaskScreen(taskId)
    │
    ▼
User interacts / ViewModel tracks
    │
    ├─ Hesitation time (PROMPT)
    ├─ Delete/rewrite count (PROMPT)
    ├─ Back button presses
    ├─ Optional media added
    └─ Time spent
    │
    ▼
User completes, skips permanently, or reschedules
    │
    ▼
TaskViewModel.complete()
    │
    ├─ Save TaskResult (with signals)
    ├─ Update TaskProgress
    ├─ Apply signals to User scores
    ├─ Decrement routing effects if active
    ├─ Update Session stats
    └─ Clear user.currentTaskId
    │
    ▼
HomeViewModel.onTaskCompleted(result)
    │
    ├─ If ROUTING task → extract RoutingResult
    └─ Call loadNextTask(routingResult)
    │
    ▼
Is this the final task?
    │
    ├─ No  → Loop back to SelectNextTaskUseCase
    └─ Yes → Navigate to GoodbyeScreen
```

---

## Intro Task System

New users need context before diving into tasks. The intro task is always the first task shown, setting expectations about the value exchange and app personality.

### Implementation

The `Task` model has a flag:

```kotlin
data class Task(
    // ... existing fields
    val isIntroTask: Boolean = false,  // If true, shown first to new users
)
```

The `User` model tracks completion:

```kotlin
data class User(
    // ... existing fields
    val hasCompletedIntroTask: Boolean = false,
)
```

`GetNextTaskUseCase` prioritizes the intro task:

```kotlin
override suspend fun invoke(): Task? {
    val user = userRepository.getUser()
    val tasks = taskRepository.getAllTasks()

    // If user hasn't completed intro task, return it first
    if (user != null && !user.hasCompletedIntroTask) {
        val introTask = tasks.find { it.isIntroTask }
        if (introTask != null) {
            return introTask
        }
    }

    // Filter intro task from normal rotation, then use fixed sequence or adaptive
    val normalTasks = tasks.filter { !it.isIntroTask }
    // ... selection logic (see Fixed Sequence section)
}
```

### Intro Task Content

The intro task (`id: "intro_welcome"`) sets the tone:

```
"I'm going to ask you things. Some will be small, some will be strange,
some might make you think. There's no right answer. Just honest ones.

Let's start simple: What's one word that describes how today has been so far?"
```

This intro:

- Explains what the app does (asks things)
- Sets expectations (small, strange, thoughtful)
- Establishes the value proposition (no right answers, just honesty)
- Starts with an easy, low-barrier first action
- Uses the app's conversational personality

---

## Depth Checking for Responses

Some tasks benefit from thoughtful responses. The depth checking system gently nudges users who provide very short answers.

### Task Configuration

```kotlin
data class Task(
    // ... existing fields
    val requiresDepth: Boolean = false,  // If true, short answers trigger follow-up
    val minCharacters: Int? = null,      // Minimum characters (default 20 if requiresDepth)
)
```

### PromptViewModel Flow

When a user submits a response:

1. Check if `task.requiresDepth` is true
2. If response length < `minCharacters` (default 20), show depth prompt dialog
3. User can choose "I'll add more" or "Keep it short"
4. If they keep it short, allow submission anyway (no blocking)

### Depth Prompt Messages

Randomized from a set of gentle prompts:

- "Just that? I'm curious to hear more if you're up for it."
- "Okay, but walk me through it a little?"
- "That's a start. What made you say that?"
- "Could you tell me more? Even a few more words help."
- "I want to understand. Can you expand on that?"

The goal is to invite elaboration without making users feel judged for brevity.

---

## Skip Pattern Detection

The app notices when users skip multiple tasks in a row and responds with understanding rather than persistence.

### Tracking

`HomeViewModel` tracks:

- `skipsThisSession: Int` - total skips this session
- `consecutiveSkips: Int` - skips in a row (resets on completion)

### ReactionContext Enhancement

```kotlin
data class ReactionContext(
    // ... existing fields
    val skipsThisSession: Int = 0,
    val consecutiveSkips: Int = 0,
)
```

### Skip Pattern Reactions

After 2+ consecutive skips, the `TaskReactionEngine` may show:

- "You've passed on a few. Not feeling it today?"
- "Some days are like that. These questions aren't going anywhere."
- "No rush. I'll be here when the timing feels right."

After 4+ consecutive skips:

- "We can stop for today if you're not feeling it. No pressure."

### Skip Button UX

The skip buttons are labeled to feel less like rejection:

- "Not this one" - Skip this specific task (will be rescheduled)
- "I'm done for now" - End the session gracefully

This follows Hanks' "one page at a time" philosophy - we're not asking them to commit to anything beyond this moment.

---

## Task Selection Algorithm

The algorithm balances personalization with calibration needs. It pulls from multiple data sources and operates in two modes: **fixed sequence** (early) and **adaptive** (once calibrated).

### Data Sources Inventory

Everything the algorithm considers:

| Source                 | Repository        | Data                                             | Purpose                           |
| ---------------------- | ----------------- | ------------------------------------------------ | --------------------------------- |
| User scores            | UserRepository    | personality + affinity scores                    | Match tasks to user profile       |
| User state             | UserRepository    | `currentTaskId`, `activeRoutingEffects`          | Resume/routing context            |
| User flags             | UserRepository    | `hasBeenAskedAboutSocialSkips`, etc.             | One-time prompts                  |
| User behavioral counts | UserRepository    | `idleSessionCount`, `backButtonPressCount`, etc. | Easter eggs, goodbye reel         |
| Mood trend             | UserRepository    | `moodTrend`, `recentMoods`                       | Filter heavy tasks when declining |
| Current session        | SessionRepository | `mood`, `tasksCompleted`, `startedAt`            | Session context                   |
| All sessions           | SessionRepository | timestamps, moods                                | Patterns (night owl, ritualistic) |
| Task definitions       | TaskRepository    | all static task data                             | Filtering, scoring                |
| Task progress          | TaskRepository    | status, attempts, reschedule times               | Eligibility                       |
| Task results           | TaskRepository    | outcomes, responses, time spent                  | Recency, patterns                 |
| Local clock            | Clock             | current time                                     | Time-gated tasks                  |
| Routing result         | Passed in         | from just-completed ROUTING task                 | Immediate influence               |

### Fixed Sequence (First 15 Tasks)

The first 15 tasks are in a fixed order. This solves the cold start problem — we can't personalize without signals.

Goals for the fixed sequence:

- Touch multiple `ScoreDimension`s for calibration
- Vary `TaskType` to discover preferences
- Stay mostly `Difficulty.LIGHT`
- Set the tone (weird, thoughtful, not a productivity app)
- Build trust before asking harder things

```kotlin
// These are task IDs in the order they should appear
// Design rationale for each position:
// 1. first_task - Simple observation, low barrier, gets them writing
// 2. light_or_deep - Selection task, reveals preference, gives them agency
// 3. hold_finger_30 - Non-verbal, tests patience, introduces app's quirky side
// 4. three_sounds - Mindfulness-lite, easy to complete, varies the rhythm
// 5. favorite_word - Playful prompt, shows the app isn't all serious
// 6. draw_turtle_dark - Drawing task, playful, tests drawing affinity
// 7. stranger_or_alone - Selection, reveals social comfort (key signal)
// 8. photo_something_old - First photo task, low creative pressure
// 9. describe_sky - Writing prompt, tests willingness for observation tasks
// 10. grateful_for_small - Gentle reflection, positive framing
// 11. hum_something - Audio task, tests audio affinity (some will skip)
// 12. photo_your_hands - Photo, slightly more personal
// 13. stillness_60 - Patience task (harder), by now they trust the app
// 14. draw_your_mood - Drawing + reflection, calibration checkpoint
// 15. used_to_be - Medium depth prompt, they've earned deeper questions

val FIXED_SEQUENCE = listOf(
    "first_task",           // PROMPT - observation, low barrier
    "light_or_deep",        // SELECTION - preference signal
    "hold_finger_30",       // HOLD_FINGER - patience, introduces quirky side
    "three_sounds",         // PROMPT - mindfulness, easy
    "favorite_word",        // PROMPT - playful, shows personality
    "draw_turtle_dark",     // DRAWING - drawing affinity
    "stranger_or_alone",    // SELECTION - social comfort signal
    "photo_something_old",  // PHOTO - photo affinity, low pressure
    "describe_sky",         // PROMPT - observation depth
    "grateful_for_small",   // PROMPT - gentle reflection
    "hum_something",        // AUDIO - audio affinity
    "photo_your_hands",     // PHOTO - slightly more personal
    "stillness_60",         // STILLNESS - patience (harder)
    "draw_your_mood",       // DRAWING - calibration checkpoint
    "used_to_be",           // PROMPT - medium depth, earned trust
)

val CALIBRATION_THRESHOLD = 15
```

Transition to adaptive mode:

- After 15 tasks **completed** (not just shown)
- Skips don't count toward the 15 (but they do give signals)
- Once past 15, algorithm takes over

```kotlin
class SelectNextTaskUseCase(
    private val taskRepository: TaskRepository,
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository,
    private val clock: Clock,
) {
    suspend fun execute(routingResult: RoutingResult? = null): Task {
        val user = userRepository.getUser()
        val session = sessionRepository.getCurrentSession()
        val progress = taskRepository.getAllProgress()

        // Apply routing result to user if provided
        if (routingResult != null) {
            userRepository.setRoutingEffects(routingResult.toRoutingEffects())
        }

        val completedCount = progress.count { it.status == COMPLETED }

        return if (completedCount < CALIBRATION_THRESHOLD) {
            selectFromFixedSequence(progress)
        } else {
            selectAdaptive(user, session, progress, routingResult)
        }
    }

    private suspend fun selectFromFixedSequence(progress: List<TaskProgress>): Task {
        val completedIds = progress.filter { it.status == COMPLETED }.map { it.taskId }.toSet()
        val skippedIds = progress.filter { it.status == SKIPPED_PERMANENT }.map { it.taskId }.toSet()

        // Find next task in sequence that isn't completed or permanently skipped
        val nextId = FIXED_SEQUENCE.firstOrNull { id ->
            id !in completedIds && id !in skippedIds
        }

        return if (nextId != null) {
            taskRepository.getTask(nextId)!!
        } else {
            // Edge case: user skipped everything in fixed sequence
            // Fall back to adaptive early
            selectAdaptive(
                userRepository.getUser(),
                sessionRepository.getCurrentSession(),
                progress,
                routingResult = null
            )
        }
    }

    private suspend fun selectAdaptive(
        user: User,
        session: Session,
        progress: List<TaskProgress>,
        routingResult: RoutingResult?,
    ): Task {
        val allTasks = taskRepository.getAllTasks()
        val recentResults = taskRepository.getRecentResults(limit = 10)

        val completedIds = progress
            .filter { it.status == COMPLETED }
            .map { it.taskId }
            .toSet()

        val permanentlySkippedIds = progress
            .filter { it.status == SKIPPED_PERMANENT }
            .map { it.taskId }
            .toSet()

        val notYetRescheduled = progress
            .filter { it.status == SKIPPED_RESCHEDULE }
            .filter { it.rescheduledUntil?.let { time -> time > clock.now() } == true }
            .map { it.taskId }
            .toSet()

        // Check if we should inject a ROUTING interstitial
        val injectedRouting = checkForRoutingInjection(user, session, progress)
        if (injectedRouting != null) {
            return injectedRouting
        }

        return allTasks
            .filter { it.id !in completedIds }
            .filter { it.id !in permanentlySkippedIds }
            .filter { it.id !in notYetRescheduled }
            .filter { it.id !in FIXED_SEQUENCE }  // Don't re-serve fixed tasks
            .filter { matchesConditions(it, session) }
            .filter { matchesMood(it, session.mood, user.moodTrend) }
            .filter { matchesScores(it, user) }
            .filter { matchesRoutingEffects(it, user, routingResult) }
            .sortedByDescending { relevanceScore(it, user, progress, recentResults) }
            .let { candidates ->
                // Add randomness so it doesn't feel algorithmic
                candidates.take(5).randomOrNull() ?: candidates.first()
            }
    }

    private suspend fun checkForRoutingInjection(
        user: User,
        session: Session,
        progress: List<TaskProgress>,
    ): Task? {
        // Inject mood-check routing if mood has been declining
        if (user.moodTrend == MoodTrend.DECLINING && !user.hasSeenDecliningMoodRouting) {
            return taskRepository.getTask("mood_check_routing")
        }

        // Inject "stop asking?" routing if user has rescheduled same task 3+ times
        val frequentReschedules = progress
            .filter { it.attempts >= 3 && it.status == SKIPPED_RESCHEDULE }
            .firstOrNull()
        if (frequentReschedules != null && !user.hasSeenStopAskingRouting) {
            // This would need to be a parameterized task or we generate the text
            return taskRepository.getTask("stop_asking_routing")
        }

        return null
    }

    private fun matchesConditions(task: Task, session: Session): Boolean {
        val conditions = task.conditions ?: return true
        val now = clock.now().toLocalDateTime(TimeZone.currentSystemDefault())

        conditions.timeAfter?.let { after ->
            if (now.time < after) return false
        }
        conditions.timeBefore?.let { before ->
            if (now.time > before) return false
        }
        conditions.requiresMoodTrend?.let { requiredTrend ->
            if (userRepository.getUser().moodTrend != requiredTrend) return false
        }

        return true
    }

    private fun matchesMood(task: Task, currentMood: Mood?, trend: MoodTrend?): Boolean {
        if (currentMood == null) return task.difficulty != HEAVY
        if (task.avoidForMoods?.contains(currentMood) == true) return false
        if (trend == DECLINING && task.difficulty == HEAVY) return false
        return true
    }

    private fun matchesScores(task: Task, user: User): Boolean {
        if (task.requiresSocial && user.socialComfort < 20) return false
        task.minimumScores?.forEach { (dimension, minimum) ->
            val userScore = user.getScore(dimension)
            if (userScore < minimum) return false
        }
        return true
    }

    private fun matchesRoutingEffects(
        task: Task,
        user: User,
        routingResult: RoutingResult?,
    ): Boolean {
        // Immediate routing result takes precedence
        val effects = routingResult?.toRoutingEffects() ?: user.activeRoutingEffects ?: return true
        if (effects.avoidCategory in task.categories) return false
        return true
    }

    private fun relevanceScore(
        task: Task,
        user: User,
        progress: List<TaskProgress>,
        recentResults: List<TaskResult>,
    ): Float {
        var score = 0f

        // === Variety Boosts ===

        // Boost categories they're underexposed to
        val completedTasks = progress.filter { it.status == COMPLETED }
        val categoryCounts = completedTasks
            .flatMap { p -> taskRepository.getTask(p.taskId)?.categories ?: emptyList() }
            .groupingBy { it }
            .eachCount()
        task.categories.forEach { cat ->
            val thisCount = categoryCounts[cat] ?: 0
            val avgCount = if (categoryCounts.isNotEmpty()) categoryCounts.values.average() else 0.0
            if (thisCount < avgCount) score += 20f
        }

        // Recency penalty — don't repeat same TaskType back-to-back
        val recentTypes = recentResults.take(3).mapNotNull {
            taskRepository.getTask(it.taskId)?.type
        }
        if (task.type in recentTypes) {
            score -= 15f * (3 - recentTypes.indexOf(task.type))  // stronger penalty for more recent
        }

        // === Mood Matching ===

        val session = sessionRepository.getCurrentSession()
        if (session.mood in (task.bestForMoods ?: emptyList())) {
            score += 15f
        }

        // === Routing Effects ===

        val effects = user.activeRoutingEffects
        if (effects?.preferCategory in task.categories) {
            score += 25f
        }
        if (effects?.preferDifficulty == task.difficulty) {
            score += 10f
        }

        // === Re-engagement ===

        // Slight boost for tasks they've attempted before but rescheduled
        val taskProgress = progress.find { it.taskId == task.id }
        if (taskProgress?.status == SKIPPED_RESCHEDULE && taskProgress.attempts > 0) {
            score += 5f
        }

        // === Affinity Matching ===

        if (task.responseStyle.allowsPhoto && user.photoAffinity > 60) score += 10f
        if (task.responseStyle.allowsAudio && user.audioAffinity > 60) score += 10f
        if (task.responseStyle.allowsDrawing && user.drawingAffinity > 60) score += 10f
        if (task.type == GAME && user.gameAffinity > 60) score += 10f
        if (task.responseStyle.allowsText && user.writingAffinity > 60) score += 10f

        // === Discovery Nudges ===

        // Occasional nudge toward things they avoid (20% chance)
        if (task.responseStyle.allowsDrawing && user.drawingAffinity < 40 && Random.nextFloat() < 0.2f) {
            score += 5f
        }

        return score
    }
}

Rescheduling Logic
When a user selects "Maybe later" on a task:
kotlinfun calculateRescheduleTime(task: Task, user: User, attemptNumber: Int): Instant {
    val baseDelayHours = when {
        SOCIAL in task.categories -> 72      // social tasks get more breathing room
        DISCOMFORT in task.categories -> 48
        else -> 24
    }

    // Exponential backoff for repeated reschedules
    val multiplier = (attemptNumber).coerceAtMost(4)
    val delayHours = baseDelayHours * multiplier

    return Clock.System.now().plus(delayHours.hours)
}
After 3-4 reschedules on the same task, the app might surface a routing question:

"I've asked you about [task summary] a few times now. Should I stop asking, or do you want me to keep trying?"


Goodbye Reel
When user reaches the final task, we compile their journey:
kotlindata class GoodbyeData(
    val userName: String?,
    val journeyDurationDays: Int,
    val sessionsCount: Int,
    val tasksCompleted: Int,
    val tasksSkipped: Int,
    val totalTimeSpentMs: Long,

    // Personality portrait
    val finalScores: Map<ScoreDimension, Int>,
    val dominantTraits: List<ScoreDimension>, // top 2
    val growthAreas: List<ScoreDimension>,    // most improved

    // Reflections — things safe to echo back
    val reflections: List<Reflection>,

    // Patterns
    val mostActiveTimeOfDay: TimeOfDay?, // MORNING, AFTERNOON, EVENING, NIGHT
    val averageMood: Mood?,
    val moodJourney: MoodTrend?,

    // Retry stories
    val retriedTasks: List<RetriedTask>,

    // Category breakdown
    val favoriteCategory: TaskCategory?,    // most completed
    val avoidedCategory: TaskCategory?,     // most skipped
)

data class Reflection(
    val taskId: String,
    val prompt: String,
    val response: String,
    val attemptNumber: Int,
)

data class RetriedTask(
    val taskId: String,
    val taskSummary: String,
    val attempts: Int,
    val finalOutcome: TaskOutcome,
    val totalTimeSpentMs: Long,
)

enum class TimeOfDay {
    MORNING,    // 5am - 12pm
    AFTERNOON,  // 12pm - 5pm
    EVENING,    // 5pm - 9pm
    NIGHT,      // 9pm - 5am
}
Goodbye Reel Compilation
kotlinclass CompileGoodbyeDataUseCase(
    private val userRepository: UserRepository,
    private val taskRepository: TaskRepository,
    private val sessionRepository: SessionRepository,
) {
    suspend fun execute(): GoodbyeData {
        val user = userRepository.getUser()
        val allResults = taskRepository.getAllResults()
        val allSessions = sessionRepository.getAllSessions()
        val allTasks = taskRepository.getAllTasks()

        val reflections = allResults
            .filter { it.outcome == COMPLETED }
            .filter { result ->
                val task = allTasks.find { it.id == result.taskId }
                task?.safeToReflect == true
            }
            .mapNotNull { result ->
                val task = allTasks.find { it.id == result.taskId } ?: return@mapNotNull null
                val responseText = when (val r = result.response) {
                    is TaskResponse.Text -> r.value
                    is TaskResponse.Selection -> r.selected
                    else -> null
                } ?: return@mapNotNull null

                Reflection(
                    taskId = result.taskId,
                    prompt = task.extractPromptText(),
                    response = responseText,
                    attemptNumber = result.attemptNumber,
                )
            }

        val retriedTasks = allResults
            .groupBy { it.taskId }
            .filter { (_, results) -> results.size > 1 }
            .map { (taskId, results) ->
                val task = allTasks.find { it.id == taskId }
                val finalResult = results.maxByOrNull { it.completedAt }!!
                RetriedTask(
                    taskId = taskId,
                    taskSummary = task?.extractSummary() ?: "a task",
                    attempts = results.size,
                    finalOutcome = finalResult.outcome,
                    totalTimeSpentMs = results.sumOf { it.timeSpentMs },
                )
            }
            .filter { it.attempts >= 2 }

        val sessionTimes = allSessions.map { it.startedAt.toLocalDateTime(TimeZone.currentSystemDefault()).hour }
        val mostActiveTimeOfDay = sessionTimes
            .groupingBy { hour ->
                when (hour) {
                    in 5..11 -> TimeOfDay.MORNING
                    in 12..16 -> TimeOfDay.AFTERNOON
                    in 17..20 -> TimeOfDay.EVENING
                    else -> TimeOfDay.NIGHT
                }
            }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key

        return GoodbyeData(
            userName = user.name,
            journeyDurationDays = /* calculate from first to last session */,
            sessionsCount = allSessions.size,
            tasksCompleted = user.tasksCompleted,
            tasksSkipped = user.tasksSkipped,
            totalTimeSpentMs = allResults.sumOf { it.timeSpentMs },
            finalScores = mapOf(
                // Personality
                SOCIAL_COMFORT to user.socialComfort,
                OPENNESS to user.openness,
                PLAYFULNESS to user.playfulness,
                PATIENCE to user.patience,
                REFLECTION_DEPTH to user.reflectionDepth,
                // Response style
                WRITING_AFFINITY to user.writingAffinity,
                PHOTO_AFFINITY to user.photoAffinity,
                AUDIO_AFFINITY to user.audioAffinity,
                DRAWING_AFFINITY to user.drawingAffinity,
                GAME_AFFINITY to user.gameAffinity,
            ),
            dominantTraits = /* top 2 scores */,
            growthAreas = /* compare to starting scores if tracked */,
            reflections = reflections,
            mostActiveTimeOfDay = mostActiveTimeOfDay,
            averageMood = /* most common mood */,
            moodJourney = user.moodTrend,
            retriedTasks = retriedTasks,
            favoriteCategory = /* most completed */,
            avoidedCategory = /* most skipped */,
        )
    }
}
```

---

## File Storage

Media stored locally in app's private directory:

````
/data/data/com.goodtimes.app/files/
    /drawings/
        {taskId}_{timestamp}.png
    /photos/
        {taskId}_{timestamp}.jpg
    /audio/
        {taskId}_{timestamp}.m4a
    /video/
        {taskId}_{timestamp}.mp4
Paths stored in TaskResult.response. Deleted automatically on app uninstall.

---

## Architecture Patterns

### SEAViewModel Pattern

All ViewModels in this app use the SEA (State-Event-Action) pattern for unidirectional data flow:

- **State (S)** — Immutable data class representing current UI state
- **Event (E)** — One-time occurrences (navigation, toasts) sent via Channel
- **Action (A)** — User or system actions that trigger state changes

```kotlin
abstract class SEAViewModel<S : Any, E : Any, A : Any>(
    private val initialStateArg: S? = null,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {

    val stateFlow: StateFlow<S>
    val eventFlow: Flow<E>
    val state: S get() = stateFlow.value

    abstract suspend fun handleAction(action: A)

    fun takeAction(action: A) { /* queues action for processing */ }
    fun sendEvent(event: E) { /* sends one-time event to UI */ }

    // From within handleAction, actions can update state:
    // action.updateState { currentState -> currentState.copy(...) }
}
```

**Usage pattern:**

```kotlin
class HomeViewModel @Inject constructor(...) : SEAViewModel<HomeState, HomeEvent, HomeAction>(
    initialStateArg = HomeState()
) {
    override suspend fun handleAction(action: HomeAction) {
        when (action) {
            is HomeAction.LoadNextTask -> {
                action.updateState { it.copy(taskFlowState = TaskFlowState.Loading) }
                val task = getNextTask()
                action.updateState { it.copy(taskFlowState = TaskFlowState.ShowingTask(task)) }
            }
            // ...
        }
    }
}
```

### Dependency Injection

Uses `kotlin-inject-anvil` for compile-time DI:

- `@Inject` — Constructor injection
- `@SingleIn(AppScope::class)` — Singleton scoping
- `@ContributesBinding(AppScope::class)` — Auto-binding to interface

```kotlin
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class UserRepositoryImpl(
    private val userDao: UserDao,
    private val sessionDao: SessionDao,
    private val clock: Clock,
) : UserRepository { ... }
```

### Module Structure

The app uses a modular architecture:

```
apps/compose/         # Main app module
features/
    home/             # Home feature
        api/          # Public interfaces, navigation
        impl/         # Implementation (ViewModels, Screens)
    onboarding/       # Onboarding feature
        api/
        impl/
libraries/
    core/             # Core utilities
    config/           # App configuration
    flowroutines/     # SEAViewModel, Flow utilities
    goodtimes/        # Domain models, interfaces
        api/          # Public interfaces
        impl/         # Implementations
        storage/      # Room database
    navigation/       # Navigation infrastructure
    resources/        # Shared resources
    storage/          # Storage abstractions
    ui/               # Design system, shared UI
```

---

## Repositories

The app uses three main repositories to abstract data access:

### UserRepository

Central repository for all user data and behavioral signals. Provides a unified `User` domain model.

```kotlin
interface UserRepository {
    // Observe
    fun observeUser(): Flow<User?>

    // Read
    suspend fun getUser(): User?
    suspend fun getMoodTrend(): MoodTrend
    suspend fun getRecentMoods(limit: Int = 5): List<Mood>

    // Profile
    suspend fun setName(name: String?)
    suspend fun setHasBeenAskedForName(asked: Boolean)

    // Task state
    suspend fun setCurrentTaskId(taskId: String?)
    suspend fun clearCurrentTaskId()
    suspend fun setRoutingEffects(effectsJson: String?)

    // Task completion signals
    suspend fun onTaskCompleted(taskId: String, signals: List<Signal>, responseTimeMs: Long, characterCount: Int?)
    suspend fun onTaskSkipped(taskId: String, signals: List<Signal>)

    // Session signals (called by SessionRepository)
    suspend fun onSessionStarted(hour: Int)  // tracks time-of-day patterns
    suspend fun onIdleSession()
    suspend fun onQuickExit()

    // Behavioral signals
    suspend fun onAppOpened()
    suspend fun onSettingsOpened()
    suspend fun onAboutOpened()
    suspend fun onOnboardingNoClick()
    suspend fun onBugReported()
    suspend fun onOptionalMediaAdded()
    suspend fun onDeleteAndRewrite()
    suspend fun onResponseHesitation(hesitationMs: Long)

    // Flags
    suspend fun setOnboardingComplete()
    suspend fun setHasAskedAboutSocialSkips()
    suspend fun setHasSeenDecliningMoodRouting()

    // Reset
    suspend fun deleteAll()
}
````

The `User` domain model includes derived traits computed from behavioral signals using **ratio-based thresholds**:

```kotlin
data class User(
    // Basic info, personality scores, response affinities, flags, stats, behavioral counts,
    // PLUS new fields for ratio-based traits:
    val optionalMediaOpportunities: Int,  // Tasks where optional media was available
    val textTasksCompleted: Int,          // Tasks requiring text responses
    val totalTextLength: Int,             // Cumulative characters typed
) {
    // Computed averages
    val averageTextLength: Int get() =
        if (textTasksCompleted > 0) totalTextLength / textTasksCompleted else 0

    val completionRate: Float get() {
        val total = tasksCompleted + tasksSkipped
        return if (total > 0) tasksCompleted.toFloat() / total else 0f
    }

    val mediaAddedRate: Float get() =
        if (optionalMediaOpportunities > 0) optionalMediaAddedCount.toFloat() / optionalMediaOpportunities else 0f

    // Derived traits using RATIOS (not raw counts)
    val isNightOwl: Boolean get() = sessionsCount >= 5 && (lateNightSessionCount.toFloat() / sessionsCount) > 0.3f
    val isMorningPerson: Boolean get() = sessionsCount >= 5 && (morningSessionCount.toFloat() / sessionsCount) > 0.3f
    val isMiddayRegular: Boolean get() = sessionsCount >= 5 && (middaySessionCount.toFloat() / sessionsCount) > 0.3f
    val isReluctantAdventurer: Boolean get() = noClickCountOnboarding >= 3
    val isCurious: Boolean get() = sessionsCount >= 5 && (settingsOpenCount.toFloat() / sessionsCount) > 0.3f
    val isHesitant: Boolean get() = sessionsCount >= 5 && (idleSessionCount.toFloat() / sessionsCount) > 0.3f
    val isVisual: Boolean get() = optionalMediaOpportunities >= 5 && mediaAddedRate > 0.4f
    val isThoughtful: Boolean get() = (averageHesitationMs ?: 0) > 30_000 ||
        (textTasksCompleted >= 3 && deleteAndRewriteCount.toFloat() / textTasksCompleted > 0.2f)
    val isVeteran: Boolean get() = tasksCompleted >= 50
    val isNewUser: Boolean get() = sessionsCount <= 3
    val isCommitted: Boolean get() = tasksCompleted >= 10 && completionRate > 0.8f
    val isSelectivePlayer: Boolean get() = (tasksCompleted + tasksSkipped) >= 10 && completionRate < 0.5f
    val isWordsmith: Boolean get() = textTasksCompleted >= 5 && averageTextLength > 150
    val isBrief: Boolean get() = textTasksCompleted >= 5 && averageTextLength < 50
}
```

**Key insight:** Raw counts are meaningless without context. "5 late night sessions" means nothing—but "30% of sessions are after midnight" tells a story.

### SessionRepository

Manages app sessions and mood tracking preferences.

```kotlin
interface SessionRepository {
    val currentSession: StateFlow<Session?>
    val moodBannerDisabled: Flow<Boolean>
    val moodBannerDismissCount: Flow<Int>    // tracks how many times user has dismissed
    val moodBannerToggleCount: Flow<Int>     // tracks toggle on/off (for playful copy)
    val hasEverAnsweredMood: Flow<Boolean>   // for first-ever mood prompt copy
    val lastMoodInteractionAt: Flow<Long?>   // epoch millis, for 6-hour gap enforcement

    fun setMood(mood: Mood)
    fun dismissMood()
    fun disableMoodBannerPermanently()
    fun enableMoodBanner()
}
```

### TaskRepository

Manages task definitions, progress, and results. (See Task Selection Algorithm section.)

---

Database Schema (Room)
kotlin@Database(
entities = [
UserEntity::class,
SessionEntity::class,
TaskEntity::class,
TaskProgressEntity::class,
TaskResultEntity::class,
],
version = 1,
)
@TypeConverters(Converters::class)
abstract class GoodTimesDatabase : RoomDatabase() {
abstract fun userDao(): UserDao
abstract fun sessionDao(): SessionDao
abstract fun taskDao(): TaskDao
abstract fun taskProgressDao(): TaskProgressDao
abstract fun taskResultDao(): TaskResultsDao
}
Type Converters
kotlinclass Converters {
@TypeConverter
fun fromMood(mood: Mood?): String? = mood?.name

    @TypeConverter
    fun toMood(value: String?): Mood? = value?.let { Mood.valueOf(it) }

    @TypeConverter
    fun fromInstant(instant: Instant?): Long? = instant?.toEpochMilliseconds()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let { Instant.fromEpochMilliseconds(it) }

    @TypeConverter
    fun fromSignalList(signals: List<Signal>): String = Json.encodeToString(signals)

    @TypeConverter
    fun toSignalList(value: String): List<Signal> = Json.decodeFromString(value)

    @TypeConverter
    fun fromTaskResponse(response: TaskResponse?): String? = response?.let { Json.encodeToString(it) }

    @TypeConverter
    fun toTaskResponse(value: String?): TaskResponse? = value?.let { Json.decodeFromString(it) }

    // ... etc

}

```

```
