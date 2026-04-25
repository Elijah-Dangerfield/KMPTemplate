# AGENTS.md

Guidelines for AI agents working in this KMP template repository.

## Overview

KMP (Kotlin Multiplatform) template with Compose Multiplatform. Modular architecture with Room database, navigation, and SEAViewModel pattern.

This is **Kotlin Multiplatform**—most code is shared, but some platform features (permissions, sensors, native APIs) require platform-specific implementations. When implementing something not inherently cross-platform, follow the patterns in `docs/swift-kotlin-communication-patterns.md`.

## Build Commands

```shell
./gradlew :apps:compose:assembleDebug          # Android
./gradlew :apps:compose:compileKotlinIosSimulatorArm64  # iOS Kotlin
xcodebuild -project apps/ios/iosApp.xcodeproj -scheme iOS -sdk iphonesimulator  # iOS full
```

## Module Structure

```
apps/compose/          # KMP entry point (Android + iOS)
apps/ios/              # Swift wrapper
features/<name>/       # Routes, public API
features/<name>/impl/  # Screens, ViewModels
libraries/<name>/      # Interfaces
libraries/<name>/impl/ # Implementations
```

**Rules** — enforced at Gradle configuration by the convention plugins:

- Only `:apps:*` may depend on `*:impl`. Impls are DI wiring composed by the app, not consumed by other modules.
- Feature `impl` modules may depend on another feature's `api`. Feature `api` modules may **not** depend on other feature `api`s (api-to-api is a cycle risk — shared types go in a library).
- Sub-modules of the same feature (`:features:foo:storage` → `:features:foo`) are allowed.
- `:libraries:storage:impl` is the one shared impl — it owns the `AppDatabase`.

Shared code → libraries. Main modules expose interfaces only; impl modules contain implementations.

## Conventional Commits (required)

Every commit (and every PR title — PRs are squash-merged) must follow [Conventional Commits](https://www.conventionalcommits.org/). Release-please derives the next version bump from commit history.

| Type | When | Version bump |
| --- | --- | --- |
| `feat:` | User-visible new capability | minor |
| `fix:` | Bug fix | patch |
| `perf:` | Perf improvement, user-visible | patch |
| `feat!:` / `BREAKING CHANGE:` | Breaking change | major |
| `refactor:`, `style:`, `test:`, `docs:`, `ci:`, `build:`, `chore:`, `revert:` | No user impact | none |

A local `.githooks/commit-msg` hook enforces this on every commit. The Gradle build fails with an install-hooks message if the hook isn't wired — run `./scripts/install_hooks.sh`.

## Convention Plugins

| Plugin | Use |
|--------|-----|
| `kmptemplate.kotlin.multiplatform` | Pure Kotlin |
| `kmptemplate.compose.multiplatform` | Kotlin + Compose |
| `kmptemplate.feature` | Feature modules |
| `kmptemplate.application` | apps:compose only |

Use `/scripts/create_module` for new modules.

## DI (kotlin-inject-anvil)

```kotlin
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
class MyImpl : MyInterface

// Multibinding for FeatureEntryPoints
@ContributesBinding(AppScope::class, multibinding = true)
```

No expect/actual for platform impls—bind different implementations per platform. iOS impls written in Swift get passed into the DI graph via `IosAppComponentFactory.create(...)`.

## SEAViewModel Pattern

```kotlin
class MyViewModel : SEAViewModel<State, Event, Action>(initialStateArg = State()) {
    override suspend fun handleAction(action: Action) {
        when (action) {
            is Action.Load -> action.updateState { it.copy(loading = true) }
        }
    }
}
```

- **State**: Immutable data class for UI
- **Event**: One-shot side effects (navigation, toasts)
- **Action**: Only way to mutate state via `action.updateState { }`

## Navigation

Routes are `@Serializable` data classes extending `Route`. Register in `FeatureEntryPoint.buildNavGraph()`:

```kotlin
screen<MyRoute> { backStackEntry -> MyScreen(...) }
bottomSheet<SheetRoute> { backStackEntry, sheetState -> ... }
dialog<DialogRoute> { backStackEntry, dialogState -> ... }
```

## Coding Guidelines

- Code like a staff engineer
- Use `Catching { }` from libraries/core instead of `runCatching`
- No comments in code
- Custom UI components in libraries/ui—avoid Material directly
- Check `ComposeApp.h` for Swift names of Kotlin types before using in Swift

## iOS Notes

- iOS framework compiled from `apps/compose`, embedded as `ComposeApp.xcframework`
- Swift types passed to Kotlin via `IosAppComponentFactory.create(...)`
- Reference `apps/compose/build/bin/iosSimulatorArm64/debugFramework/ComposeApp.framework/Headers/ComposeApp.h` for generated Swift interfaces
- **Use `@ObjCName("TypeName", exact = true)` on Kotlin types used from Swift** to give stable names that won't change when project is renamed:
  ```kotlin
  @file:OptIn(ExperimentalObjCName::class)
  import kotlin.experimental.ExperimentalObjCName
  import kotlin.native.ObjCName
  
  @ObjCName("MyType", exact = true)
  interface MyType { ... }
  ```
  Note: The `exact = true` parameter prevents module prefixes from being added. Without it, the Swift name would be `<ModuleName><ObjCName>` (e.g., `KmptemplateMyType`).

## Key Files

| Purpose | Path |
|---------|------|
| User model | `libraries/kmptemplate/src/.../User.kt` |
| SEAViewModel | `libraries/flowroutines/src/.../SEAViewModel.kt` |
| App DI | `apps/compose/src/.../AppComponent.kt` |
| iOS entry | `apps/ios/iosApp/iOSApp.swift` |
| Swift↔Kotlin patterns | `docs/swift-kotlin-communication-patterns.md` |

