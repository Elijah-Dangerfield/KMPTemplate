# Product & Engineering TODO

A living document tracking improvements, features, and technical debt.

---

## 🔴 High Priority

### ~~Adaptive Task Selection Algorithm~~ ✅ COMPLETED

**Location:** [GetNextTaskUseCaseImpl.kt](../libraries/goodtimes/impl/src/commonMain/kotlin/com/dangerfield/goodtimes/libraries/goodtimes/impl/GetNextTaskUseCaseImpl.kt)

Implemented full scoring system using `AwarenessContext`:

- [x] Score by affinity match (user's `writingAffinity` vs `allowsText`, etc.)
- [x] Score by difficulty appropriateness (new users get lighter, veterans get variety)
- [x] Score by category variety (don't repeat same category back-to-back)
- [x] Filter by mood conditions (`bestForMoods`, `avoidForMoods`, decline mood trend)
- [x] Time-of-day awareness (late night reflections, weekend social tasks)
- [x] Device capability filtering (no photo tasks without camera permission)
- [x] Personality bonuses (curious users, night owls, thoughtful users)
- [ ] Apply routing effects if active (user chose "light" → bias toward light tasks)

### Camera/Photo Integration

**Location:** [PromptScreen.kt](../features/tasks/impl/src/commonMain/kotlin/com/dangerfield/goodtimes/features/tasks/impl/templates/prompt/PromptScreen.kt#L115)

- [ ] Implement camera picker integration (currently placeholder `onClick`)
- [ ] Android: CameraX implementation ([CameraPreview.android.kt](../libraries/ui/src/androidMain/kotlin/com/dangerfield/libraries/ui/CameraPreview.android.kt#L19))
- [ ] iOS: AVFoundation camera implementation
- [ ] Gallery picker fallback
- [ ] Photo permission handling + adaptive task fallback (describe instead of capture)

### End State Handling

**Location:** [HomeViewModel.kt](../features/home/impl/src/commonMain/kotlin/com/dangerfield/goodtimes/features/home/impl/HomeViewModel.kt#L267)

When user has completed all available tasks:

- [ ] Distinguish between "genuinely finished" vs "error/empty DB"
- [ ] Design celebratory end state ("You've done it all")
- [ ] Algorithm should verify: all eligible tasks either completed or permanently skipped
- [ ] Handle edge case: new tasks added after completion

---

## 🟡 Medium Priority

### Dynamic Copy Based on User History

**Location:** [AboutMeViewModel.kt](../features/home/impl/src/commonMain/kotlin/com/dangerfield/goodtimes/features/home/impl/AboutMeViewModel.kt#L31)

The "About Me" screen copy should evolve:

- [ ] Visit count awareness (already tracked, not used)
- [ ] Reflect learned personality traits back to user
- [ ] Progressive reveal of app personality over sessions

### Useless Button Personality Signals

**Location:** [UselessButtonDialog.kt](../features/home/impl/src/commonMain/kotlin/com/dangerfield/goodtimes/features/home/impl/UselessButtonDialog.kt#L112)

Click count now contributes to personality via `AwarenessContext`:

- [x] `clickCount >= 3`: User is curious/thorough → `isCurious` trait
- [x] `clickCount >= 5`: User is persistent → `isPersistent` trait
- [x] `clickCount >= 10`: Completed journey → `completedUselessButtonJourney` flag
- [ ] Special acknowledgment in copy for persistent users
- [ ] Achievement unlock UI for journey completion?

### ~~Context Repository~~ ✅ COMPLETED

Replaced `CopyContext` with unified `AwarenessContext`:

- [x] Created `AwarenessContext` with sub-contexts (Time, Device, Session, Personality, Mood)
- [x] Time of day, weekend detection
- [x] Session frequency, returning after absence
- [x] Mood trend detection wired up
- [x] Useless button clicks → personality traits
- [x] Used by copy generation (ScreenCopy.kt)
- [ ] Wire up to task selection (GetNextTaskUseCase)
- [ ] Wire up device context (battery, internet) via platform providers

### Fresh Start Flow

**Location:** [FreshStartViewModel.kt](../features/home/impl/src/commonMain/kotlin/com/dangerfield/goodtimes/features/home/impl/FreshStartViewModel.kt#L25)

- [ ] Implement fresh start logic (reset user data?)
- [ ] Confirm destructive action with appropriate gravity
- [ ] Keep some things? (personality traits earned vs responses)

### Follow-up Reflection Input

**Location:** [InstructionScreen.kt](../features/tasks/impl/src/commonMain/kotlin/com/dangerfield/goodtimes/features/tasks/impl/templates/instruction/InstructionScreen.kt#L226)

- [ ] Add text input for FEELING follow-up type
- [ ] Let users describe how completing the task made them feel

---

## 🟢 Low Priority / Nice-to-Have

### BaseTaskViewModel Architecture

**Location:** [BaseTaskViewModel.kt](../features/tasks/impl/src/commonMain/kotlin/com/dangerfield/goodtimes/features/tasks/impl/base/BaseTaskViewModel.kt#L27)

- [ ] Consider refactoring to SEA ViewModel pattern
- [ ] Current setup has a lot of boilerplate that SEA might reduce

### Mood Trend Detection

Referenced in System_Design.md but not implemented:

- [ ] Track mood over multiple sessions
- [ ] Detect sustained low moods
- [ ] Proactively offer routing: "Things have been heavy. Light or sit with it?"

### Reschedule Intelligence

When users repeatedly skip/reschedule same task:

- [ ] Track reschedule count per task
- [ ] After 3-4 reschedules, ask: "Should I stop showing this?"
- [ ] Calculate optimal reschedule timing based on user patterns

---

## 📝 Product Ideas (Not Yet Designed)

### Reactions to Responses

The system captures `TaskResponse` but doesn't react meaningfully yet:

- Short responses → depth prompts (implemented)
- Emotional responses → acknowledgment?
- Patterns over time → personality insights?

### Task Streaks / Consistency

- Track consecutive days of engagement
- Gentle acknowledgment (not gamification)
- "You've shown up 5 days in a row. That's something."

### Seasonal / Time-Based Tasks

- Holiday-aware tasks
- Weather-aware tasks (if permission granted)
- Anniversary tasks ("One year ago you wrote...")

### Export / Time Capsule

- Let users export their responses
- "Time capsule" feature: resurface old responses after time passes
- "You wrote this 6 months ago: [response]. Still true?"

---

## 🔧 Technical Debt

| File                         | Issue                              | Priority |
| ---------------------------- | ---------------------------------- | -------- |
| `create_module.main.kts:400` | Placeholder TODO in generated code | Low      |
| `Resources.kt:6`             | Empty library placeholder          | Low      |

---

## ✅ Recently Completed

- [x] Fixed sequence for first 15 tasks (calibration phase)
- [x] Intro task for new users
- [x] Mood prompt timing (1-3 tasks for returning, 3-5 for first session)
- [x] 6-hour minimum between mood prompts
- [x] Remove global skip buttons from task screens
- [x] Debug-only skip button on onboarding
- [x] **AwarenessContext refactor** - Unified context replacing CopyContext
  - Sub-contexts: TimeContext, DeviceContext, SessionContext, PersonalityContext, MoodContext
  - Mood trend detection now available
  - Useless button clicks contribute to personality traits
  - Single source of truth for all "awareness"
- [x] **Adaptive Task Selection** - Smart scoring using AwarenessContext
  - Mood filtering (avoid bad-mood tasks, prefer appropriate difficulty when declining)
  - Affinity scoring (writing, photo, audio, drawing preferences)
  - Category variety (penalize recently shown categories)
  - Time-of-day bonuses (reflection at night, social on weekends)
  - Device capability filtering (no photo tasks without camera)
  - Personality bonuses (curious, night owl, thoughtful, visual)
- [x] **Compose Previews** - Added comprehensive previews
  - HomeScreen.kt: Loading states, reactions (quip, observation, question, acknowledgment), useless button visibility
  - TaskHost.kt: Loading state, placeholder screens for prompt/instruction/drawing/stillness tasks
  - Reusable PreviewTasks object with sample task data

---

_Last updated: January 20, 2026_
