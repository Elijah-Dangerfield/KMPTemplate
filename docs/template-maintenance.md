# Template maintenance

Maintainer doc for the TEMPLATE repository. The init script deletes this file
from generated projects.

## How project generation works

`scripts/init_project.main.kts` (run from the repo root) creates a fresh copy
of the template with all naming rewritten. Steps, in order:

1. **Copy** the whole tree into the destination dir. Skipped: `SKIP_DIRECTORIES`
   (`.git`, `build`, `.claude`, …), the `template/` staging folder, the root
   `.github/` (that's the template repo's own CI), and `local.properties`.
2. **SETUP.md + CI choice** — `template/SETUP.md` is placed at the project
   root. If CI is enabled, everything under `template/ci/` is copied into the
   project preserving relative paths (`.github/workflows/`, fastlane files,
   `pages/`, release-please config). If declined, `template/ci/` ships as-is
   into the project alongside `scripts/enable_ci.sh` so it can be enabled
   later with a pure file move.
3. **Content replacement** — every `kmptemplate`/`KMPTemplate`/`kmp-template`/…
   variant and the `com.kmptemplate` package prefix are rewritten in files
   matching `TEXT_FILE_EXTENSIONS` or the extensionless allowlist in
   `shouldProcessFile` (Dockerfile, Fastfile, .env.example, …).
4. **Directory renames** — package dirs first (`com/kmptemplate` → the full
   new package path), then name-carrying dirs deepest-first.
5. **File renames** — same name variants in file names.
6. **Placeholder substitution** — `{{APP_NAME}}`, `{{CONTACT_EMAIL}}`,
   `{{LAST_UPDATED}}`, `{{APP_TAGLINE}}`, `{{APP_DESCRIPTION}}` in the staged
   CI/pages files.
7. **Cleanup** — template-only artifacts are deleted (see
   `cleanupTemplateArtifacts`), README/AGENTS.md are rewritten from
   template-framing to app-framing, executable bits are restored.
8. **Git reset** — old history removed, fresh `git init` + exactly one
   initial commit.

### Non-interactive mode

```bash
./scripts/init_project.main.kts \
  --name "My App" --package com.example.myapp \
  --email you@example.com --dir /path/to/new/project \
  --ci=yes --yes
```

All-or-nothing: any flag present requires all of them (including `--yes`).
Exit code is non-zero on any failure. This mode exists for
`scripts/verify_template.sh` and template CI; it uses flags rather than piped
stdin so automation breaks loudly when the prompt flow changes.

## The checklist: adding modules or new file types

The init script and staged CI have allowlists that DO NOT update themselves.
When you add a module or a new kind of file, walk this list:

- **`settings.gradle.kts`** — add the `include(...)`. If `apps/server` gains a
  dependency on a `:libraries:*` module, also include it outside the
  `serverOnly` branch and add a matching COPY to `apps/server/Dockerfile`.
- **Staged CI task lists** — `template/ci/.github/workflows/ci.yml` and
  `release.yml` name Gradle tasks explicitly. New modules with tests are
  covered by `testDebugUnitTest`, but any new *app* target (e.g. an admin JS
  bundle) needs its build task added.
- **`ensureExecutableBits`** (init script) — any new shell script or git hook
  needs its path added, or generated projects get non-executable copies.
- **`TEXT_FILE_EXTENSIONS` / `shouldProcessFile`** (init script) — a new text
  file type that can carry the project name (new config format, extensionless
  file) must be added or it ships un-renamed.
- **`cleanupTemplateArtifacts`** (init script) — template-only files must be
  added to the deletion list or they ship into generated projects.
- **`SKIP_DIRECTORIES`** (init script) — new machine-local or build-output
  dirs must be skipped.
- **Run `scripts/verify_template.sh --fast`** before the closing commit of
  any milestone that touched the above.

## verify_template.sh

`scripts/verify_template.sh [--fast|--full]` generates a project into a temp
dir (non-interactive, CI enabled) and asserts:

- zero residual template naming (`kmp[ ._-]?template`, case-insensitive)
- zero Cards branding (`cards|downcard|warm-felt`, case-insensitive, small
  allowlist for the history docs)
- no unsubstituted `{{PLACEHOLDER}}` tokens
- executable bits on gradlew/scripts/hooks; template-only artifacts deleted;
  `install_hooks.sh` runs clean; exactly one git commit
- `actionlint` passes on the generated workflows
- the generated project builds: `--fast` = Android compile + unit tests;
  `--full` adds the iOS Kotlin compile, server tests (needs Docker), the
  admin JS bundle and a server `docker build` once those exist.

Env: `VERIFY_INCLUDE_SERVER_TESTS=1` adds server tests to `--fast` (template
CI sets this — Ubuntu runners have Docker). `VERIFY_KEEP=1` keeps the
generated dir for inspection.

## Template CI (root `.github/workflows/template-ci.yml`)

Runs on every push/PR, no path filters (this repo IS the artifact):

- **jvm-tests** (Ubuntu): de-branding grep, actionlint on both workflow sets,
  `testDebugUnitTest`, `:apps:server:test` (testcontainers).
- **ios-compile** (macOS): iOS Kotlin compile + `xcodebuild` no-codesign
  simulator build.
- **generated-smoke** (Ubuntu): `verify_template.sh --fast` with server tests.

Generated projects never receive this workflow — the init script skips the
root `.github/`.

## Local baseline gate

Before any commit:

```bash
./gradlew :apps:compose:compileDebugKotlinAndroid :apps:compose:compileKotlinIosSimulatorArm64 :apps:compose:assembleDebug testDebugUnitTest
```

Add for server-touching commits (needs Docker running):

```bash
./gradlew :apps:server:test
```

Add for Swift-touching commits:

```bash
xcodebuild -project apps/ios/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' build CODE_SIGNING_ALLOWED=NO
```

Lint staged workflows after CI-staging changes:

```bash
actionlint template/ci/.github/workflows/*.yml
```
