# KMP Template

A Kotlin Multiplatform project with a clean, modular architecture.

## Structure

### Apps

- **`apps/compose/`** - Multiplatform mobile app (Android + iOS targets)
- **`apps/ios/`** - Xcode project that consumes the mobile KMP frameworks
- **`apps/server/`** - Ktor server application

### Features

Feature modules using KMP source sets (`commonMain`/`androidMain`/`iosMain`/`jvmMain`) for platform-specific implementations.

### Libraries

Shared infrastructure and utilities. For libraries with implementation complexity, prefer `impl` sub-modules for implementations to optimize build performance.

- **`libraries/core/`** - Platform abstractions and shared constants
- **`libraries/ui/`** - Shared UI components
- **`libraries/networking/`** - Network layer
- **`libraries/database/`** - Data persistence
- **`libraries/analytics/`** - Analytics integration

## Architecture Rules

- **Features cannot depend on other features** - keeps dependency graph acyclic
- **Shared code belongs in libraries** - extract common functionality up to appropriate library
- **Use `impl` sub-modules** - separates contracts from implementations for better build performance

## Getting Started

### Mobile App

```shell
./gradlew :apps:compose:assembleDebug  # Android
```

For iOS, open `apps/ios/` in Xcode and run.

### Server

```shell
./gradlew :apps:server:run
```

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
