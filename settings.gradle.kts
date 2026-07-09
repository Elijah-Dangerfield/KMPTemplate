enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "KMPTemplate"

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        google()
        mavenCentral()
    }
}

// `-DserverOnly=true` (set by apps/server/Dockerfile) trims the build graph to
// just :apps:server so a server image build needs no Android SDK or Kotlin/Native
// toolchain. The property name is intentionally project-agnostic so the rename
// tooling can't break the Dockerfile↔settings contract. The server is a plain JVM
// module with no client-library deps, so nothing else has to be included. If
// apps/server ever depends on a :libraries:* module, add an always-included
// `include(...)` for it here (outside the `if`) and a matching COPY in the Dockerfile.
val serverOnly = System.getProperty("serverOnly") == "true"

// Apps (always included)
include(":apps")
include(":apps:server")

if (!serverOnly) {
    include(":apps:compose")
    // Note: iOS app is not a Gradle module - it's an Xcode project in apps/ios/

    // Features
    include(":features:home")
    include(":features:home:impl")

    // Libraries
    include(":libraries:config")
    include(":libraries:config:impl")
    include(":libraries:core")
    include(":libraries:flowroutines")
    include(":libraries:navigation")
    include(":libraries:navigation:impl")
    include(":libraries:networking")
    include(":libraries:networking:impl")
    include(":libraries:identity")
    include(":libraries:identity:impl")
    include(":libraries:resources")
    include(":libraries:storage")
    include(":libraries:storage:impl")
    include(":libraries:kmptemplate")
    include(":libraries:kmptemplate:impl")
    include(":libraries:kmptemplate:storage")
    include(":libraries:ui")
}