# Template maintenance

Maintainer doc for the TEMPLATE repository. The init script deletes this file
from generated projects.

## How project generation works

`scripts/init_project.main.kts` (run from the repo root) creates a fresh copy
of the template with all naming rewritten. Steps, in order:

1. **Copy** the whole tree into the destination dir. Skipped: `SKIP_DIRECTORIES`
   (`.git`, `build`, `.claude`, …), the `template/` staging folder, the root
   `.github/` (that's the template repo's own CI), and `local.properties`.
2. **SETUP.md + CI choice + backend choice**: `template/SETUP.md` is placed at
   the project root. If CI is enabled, everything under `template/ci/` is
   copied into the project preserving relative paths (`.github/workflows/`,
   fastlane files, `legal/`, release-please config). If declined, `template/ci/`
   ships as-is into the project alongside `scripts/enable_ci.sh` so it can be
   enabled later with a pure file move. The backend question is asked here too
   but applied in step 7, because its doc edits have to run after the rename
   pass.
3. **Content replacement**: every `kmptemplate`/`KMPTemplate`/`kmp-template`/…
   variant and the `com.kmptemplate` package prefix are rewritten in files
   matching `TEXT_FILE_EXTENSIONS` or the extensionless allowlist in
   `shouldProcessFile` (Dockerfile, Fastfile, .env.example, …).
4. **Directory renames**: package dirs first (`com/kmptemplate` → the full
   new package path), then name-carrying dirs deepest-first.
5. **File renames**: same name variants in file names.
6. **Placeholder substitution**: `{{APP_NAME}}`, `{{CONTACT_EMAIL}}`,
   `{{LAST_UPDATED}}`, `{{APP_TAGLINE}}`, `{{APP_DESCRIPTION}}` in the staged
   CI/pages files.
7. **Cleanup**: template-only artifacts are deleted (see
   `cleanupTemplateArtifacts`), README/AGENTS.md are rewritten from
   template-framing to app-framing, the backend is removed if it was declined
   (see `removeBackend`), executable bits are restored.
8. **Git reset**: old history removed, fresh `git init` + exactly one
   initial commit.

### Non-interactive mode

```bash
./scripts/init_project.main.kts \
  --name "My App" --package com.example.myapp \
  --email you@example.com --dir /path/to/new/project \
  --ci=yes --backend=yes --yes
```

All-or-nothing: any flag present requires all of them (including `--yes`).
Exit code is non-zero on any failure. This mode exists for
`scripts/verify_template.sh` and template CI; it uses flags rather than piped
stdin so automation breaks loudly when the prompt flow changes.

## The checklist: adding modules or new file types

The init script and staged CI have allowlists that DO NOT update themselves.
When you add a module or a new kind of file, walk this list:

- **`settings.gradle.kts`**: add the `include(...)`. If `apps/server` gains a
  dependency on a `:libraries:*` module, also include it outside the
  `serverOnly` branch and add a matching COPY to `apps/server/Dockerfile`.
- **Staged CI task lists**: `template/ci/.github/workflows/ci.yml` and
  `release.yml` name Gradle tasks explicitly. New modules with tests are
  covered by `testDebugUnitTest`, but any new *app* target (e.g. an admin JS
  bundle) needs its build task added.
- **`ensureExecutableBits`** (init script): any new shell script or git hook
  needs its path added, or generated projects get non-executable copies.
- **`TEXT_FILE_EXTENSIONS` / `shouldProcessFile`** (init script): a new text
  file type that can carry the project name (new config format, extensionless
  file) must be added or it ships un-renamed. Example: detekt's ServiceLoader
  file (`detekt-rules/src/main/resources/META-INF/services/dev.detekt.api.RuleSetProvider`)
  is on the extensionless allowlist because its dotted name defeats extension
  matching but its content is the provider's `com.kmptemplate` FQN.
- **`config/detekt/baseline.xml`**: regenerate with `./gradlew detektBaseline`
  when template code adds inline-string findings you intend to keep (prefer
  fixing them via `:libraries:resources`); the baseline ships into generated
  projects as their accepted debt.
- **`cleanupTemplateArtifacts`** (init script): template-only files must be
  added to the deletion list or they ship into generated projects.
- **`removeBackend`** (init script): the `--backend=no` path rewrites the
  Gradle files, the CI workflow, `.gitignore`, `detekt.yml`, the version
  catalog, the three root docs, two practice docs, `libraries/config/README.md`,
  three networking KDoc blocks and the decision log. Whole sections go through
  `cutSection`, bullets through `dropListItems`, table rows through
  `dropTableRows`, all keyed on headings and subjects, which are stable.
  Single lines and sentences go through `applyEdits`, which matches **exact
  text**: reword one of those and the anchor stops matching. Every helper fails
  the run rather than skipping the edit, and `verify_template.sh` generates a
  client-only project on every run, so breakage surfaces here. As "update the
  anchor in init_project.main.kts", which is a job for whoever did the
  rewording. Prefer the structural helpers when you add a removal; an exact
  anchor spanning more than a few lines is a doc edit waiting to break the
  generator.
- **`Keys` in `scripts/lib/setup_store.main.kts`**: a credential the setup
  scripts should stop re-asking for needs an entry here plus a mention in the
  relevant script's `plan(...)`. `setup_credentials.main.kts` enumerates
  `Keys.all`, so the editor picks it up automatically.
- **A new `setup_*.main.kts`** needs three things: its path in
  `ensureExecutableBits`, a `Step` in `scripts/setup.main.kts` placed after
  whatever produces its inputs, and a section in `scripts/README.md`. If it only
  applies to projects with a backend, add it to `removeBackend`'s deletion list
  too.
- **Any string literal in a template file that names the template** is rewritten
  per project. That is usually what you want, and occasionally catastrophic: a
  comparison against such a literal inverts in a generated project, and
  `verify_template.sh`'s de-branding grep cannot catch it because a *renamed*
  literal is exactly what that grep wants to see. Identifiers that must stay
  equal across generated projects carry no project name (`serverOnly`, the
  `appsetup` store directory). See AGENTS.md → Known landmines.
- **`SKIP_DIRECTORIES`** (init script): new machine-local or build-output
  dirs must be skipped.
- **Run `scripts/verify_template.sh --fast`** before the closing commit of
  any milestone that touched the above.

## verify_template.sh

`scripts/verify_template.sh [--fast|--full]` generates a project into a temp
dir (non-interactive, CI and backend enabled) and asserts:

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

It then generates a **second** project with `--backend=no` and asserts that one
separately: the three backend modules and both deploy workflows are gone, no
`:apps:server` / `serverOnly` / `FLY_API_TOKEN` reference survives **anywhere
in the tree** bar two allowlisted files, every client module is still in
`settings.gradle.kts`, markdown links still resolve, and Gradle configuration
succeeds. That middle pair is the point, the `if (!serverOnly)` block wraps
every client module, so unwrapping it and deleting it both leave a settings
file that parses, and only the module list tells them apart. This runs in both
modes; it is configuration only, since no client module depends on a server
module.

The two allowlisted files are `docs/decisions.md` (a dated log records what was
decided, and the entry explaining why this project has no server has to name
the modules it removed) and `scripts/lib/setup_store.main.kts` (the credential
store is shared across every project you generate, so it still carries Fly
tokens for the ones that do have a backend). Adding to that allowlist should
feel harder than fixing the reference.

Env: `VERIFY_INCLUDE_SERVER_TESTS=1` adds server tests to `--fast` (template
CI sets this, Ubuntu runners have Docker). `VERIFY_KEEP=1` keeps the
generated dir for inspection.

## Template CI (root `.github/workflows/template-ci.yml`)

Runs on every push/PR, no path filters (this repo IS the artifact):

- **jvm-tests** (Ubuntu): de-branding grep, actionlint on both workflow sets,
  `testDebugUnitTest`, `:apps:server:test` (testcontainers).
- **ios-compile** (macOS): iOS Kotlin compile + `xcodebuild` no-codesign
  simulator build.
- **generated-smoke** (Ubuntu): `verify_template.sh --fast` with server tests.

Generated projects never receive this workflow: the init script skips the
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
