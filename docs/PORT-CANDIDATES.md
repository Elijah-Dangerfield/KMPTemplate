# Port candidates

Work for this repo, found by the apps built from it. Two kinds qualify: something a downstream app proved in production that a brand-new app would want, and a bug in code this template already ships. The second matters more, because every generated app already has it. See AGENTS.md → "This template is fed by the apps built from it" for how to write an entry: say what broke, how it looked from the outside, and why it was hard to spot.

This is a queue, not a changelog. Delete an entry when you land it.

Closed questions live in [decisions.md](decisions.md); traps that can only be avoided rather than fixed live in AGENTS.md → "Known landmines".

---

## Ask at init time whether the generated app has a backend

**What it is.** `scripts/init_project.main.kts` asks for a display name, a package, a contact email, a destination, and whether to enable CI. It never asks whether the project wants a server. Every generated app therefore gets `:apps:server`, `:apps:admin`, `:apps:integration`, and a live Fly.io deploy pipeline, whether or not anyone intends to run a backend. Add a backend question next to the CI question and honour a "no".

**How it looks from the outside.** A freshly generated, client-only app gets red CI on one of its first pushes to `main`, for a backend it never asked for. `template/ci/.github/workflows/server-deploy.yml` and `template/ci/.github/workflows/server-deploy-prod.yml` both trigger on `push` to `main` filtered on `apps/server/**`, `apps/admin/**`, `build-logic/**`, `gradle/libs.versions.toml`, `settings.gradle.kts` and `build.gradle.kts`. Those last three mean an ordinary root-build edit arms them, which happens in week one of any project. With no Fly app and no `FLY_API_TOKEN_DEV` secret they fail at the deploy step. `template/ci/.github/workflows/ci.yml` costs the same app less visibly: the `server-test` job runs `./gradlew :apps:server:test` and the `integration-test` job runs `./gradlew :apps:integration:testDebugUnitTest`, both Docker-backed, on every PR.

**Why it is hard to spot.** The failure reads as an unfinished setup step rather than as unwanted machinery. `template/SETUP.md` lists the Fly secrets in its own checklist ("Server deploy (Fly.io)", with `FLY_API_TOKEN_DEV` and `FLY_API_TOKEN_PROD`), so a red deploy job looks like "I have not done that step yet" instead of "this workflow should not exist in my repo." The obvious fix is worse than it looks: downstream reached for `gh workflow disable` on both workflows, which sets GitHub-side state that lives nowhere in the repository. It does not appear in a diff, it does not survive a fresh clone or a repo transfer, and a reviewer reading the checked-in workflows cannot tell it happened.

**What a "no" answer has to remove.** Delete outright:

- `apps/server/` (carries `fly.toml`, `fly.prod.toml`, `Dockerfile`, `docker-compose.yml`, `DEPLOY.md`, `README.md`, `admin-web/`)
- `apps/admin/` (the console is served by the server at `/admin` and ships inside the server image, so it cannot outlive it)
- `apps/integration/` (`apps/integration/build.gradle.kts` declares `implementation(projects.apps.server)`)
- `template/ci/.github/workflows/server-deploy.yml`
- `template/ci/.github/workflows/server-deploy-prod.yml`

Edit rather than delete:

- `settings.gradle.kts`: drop `include(":apps:server")`, `include(":apps:integration")` and `include(":apps:admin")`. Also retire the `serverOnly` mechanism, which exists only to slim the graph for the server's Docker image and is dead weight once there is no image. That means deleting its comment and `val` (lines 35 to 42) and unwrapping the `if (!serverOnly) {` at line 48, whose body runs to line 104 and contains every client module. Unwrap it, do not delete it. Deleting the block removes `:apps:compose`, every feature and every library, and the mistake is easy to make because the opening lines read like a small self-contained section
- `template/ci/.github/workflows/ci.yml`: drop the `server-test` and `integration-test` jobs
- `template/SETUP.md`: drop the "Server deploy (Fly.io)" section, its two `FLY_API_TOKEN_*` rows, the matching checklist line, and the `SUPABASE_URL` server-env item
- `AGENTS.md`: drop the "Server (`:apps:server`)" section; `README.md`: drop the server rows from the layout table, the doc index, and the run/test commands

**Where it hooks in.** The shape already exists in the script. `maybeEnableCi` (init_project.main.kts:474) prints an explanation, prompts `y/N`, and returns a `Boolean` that `cleanupTemplateArtifacts` takes as a parameter to decide what to remove. A backend prompt is that pattern with a longer removal list, plus a `--backend` flag in `parseCliConfig` alongside the existing `--ci` so `--yes` runs stay non-interactive.

One thing does not transfer. The CI prompt can decline safely because the declined files stay staged under `template/ci/` for `scripts/enable_ci.sh` to install later. Backend removal is destructive by comparison: the modules leave `settings.gradle.kts` and the source is gone. Decide deliberately whether "no" stages `apps/server/` somewhere for a later opt-in or whether adding a backend afterwards is a template-diff job. Do not let the CI precedent decide it by default.

**Open question worth answering in the prompt text.** "Does your app have a backend" invites the wrong reading. `:libraries:identity` (Supabase auth) and `:libraries:networking` do not depend on `:apps:server`, and a client-only app can still sign in against Supabase and call third-party HTTP. The prompt should say it is asking about *this project's own* server, not about network access in general.

**Provenance.** Drop 2048, 2026-09-16. Drop 2048 does have a backend, so this was not fatal there; both deploy workflows are disabled only until its Fly apps exist. What ports back is the prompt, not the disable.

---

## The iOS build command in AGENTS.md names a scheme that does not exist

**What it is.** `AGENTS.md:16` gives the full iOS build as:

```
xcodebuild -project apps/ios/iosApp.xcodeproj -scheme iOS -sdk iphonesimulator
```

There is no scheme called `iOS`. The only one is `iosApp`, which is what
`.github/workflows/template-ci.yml:145` already passes. The doc is the odd one
out, so the fix is a one word change.

**Why it is hard to spot.** Nobody reads AGENTS.md's build commands until they
need one, and by then they are usually debugging something else and read
`xcodebuild: error: The project does not contain a scheme named "iOS"` as a
problem with their Xcode install or their checkout.

**There is a second, larger problem underneath it.** The `iosApp` scheme is not
tracked by git. It exists only at
`apps/ios/iosApp.xcodeproj/xcuserdata/<user>.xcuserdatad/xcschemes/iosApp.xcscheme`,
and `.gitignore:5` ignores `xcuserdata`. So a fresh clone has no shared scheme,
and every `-scheme iosApp` invocation depends on Xcode or xcodebuild
autocreating one from the target. That works today, including in CI, which is
exactly why it has gone unnoticed. It is not something to rely on: autocreation
is an Xcode behaviour, not a contract, and it produces a scheme nobody has
reviewed.

The repo is already set up for the fix. `.gitignore:15` un-ignores
`!*.xcodeproj/xcshareddata/`, so marking the scheme Shared in Xcode moves it to
`xcshareddata/xcschemes/` where it will be tracked. Do that, commit it, and the
build command becomes true for everyone rather than true by luck.

**Provenance.** Reported by a template consumer, 2026-09-19. Both facts above
were verified in this repo, not taken on report.

---

## One machine-local store for setup secrets, so the second project is not as manual as the first

**What it is.** Spinning up a project from this template asks for the same
values every time, and answers them from scratch every time. `setup_sentry.main.kts`
reads no environment variables and has no shared store at all, so project five
is exactly as tedious as project one. The same is true of every other
credential the template wants.

Generalise it. One machine-local config, written once, read by every setup
script. Sentry is the first consumer, not the only one.

**Precedence, highest first.** Environment variable, then the machine-local
store, then prompt. CI already sets these as environment variables, so putting
env first means CI keeps working untouched and a one off override stays
possible.

**Where it lives.** Outside every repo, so it is never committed and never
copied into a generated project. Something like `~/.config/kmptemplate/`, mode
`0600`. It holds values that are genuinely per person and per machine, not per
project.

**What belongs in it.** The values that are identical across every project you
create:

- Sentry: the user auth token used during setup, the org slug, and the `sntrys_`
  org token that becomes each repo's `SENTRY_AUTH_TOKEN` secret
- Fly.io: `FLY_API_TOKEN_DEV` and `FLY_API_TOKEN_PROD`
- Apple: team ID, App Store Connect key id, issuer id, and the key file path
- Supabase: whatever a new project needs to point at an instance
- GitHub: whether `gh` is already authenticated, so the scripts can skip asking

What does not belong in it: anything per project. The Sentry project slug
derives from the `applicationId`. The Fly app name derives from the project
name. Deriving beats asking.

**The constraint that shapes the whole design: the store will often be absent.**
A different laptop, a fresh machine, a CI runner, a contributor who is not you.
So the store is an accelerator, never a requirement. Every script has to run
correctly with none of it present, which means:

- Missing values fall back to prompting, exactly as today. Nothing becomes
  unreachable because a file is not there.
- **The script says what it could not find and what that means.** Not a silent
  fallback. A short summary at the start listing which values came from the
  environment, which from the store, and which it is about to ask for. Then a
  summary at the end listing what is still not configured and the consequence of
  each, in the form "crash reporting is off until you run X".
- A non-interactive run with a missing required value fails loudly with the name
  of the value and how to supply it, rather than proceeding with a blank.
- A way to populate the store on a new machine in one step, so the recovery path
  is short. Consider making the store exportable, but note that it contains live
  credentials, so a copy instruction has to say that out loud rather than
  suggesting people sync it somewhere convenient.

**Why the "say what is missing" part is the important half.** The failure this
template has already shipped once is a blank value that is also a supported
value. A blank Sentry DSN means "reporting off", so a project with no DSN looks
configured and reports nothing, silently, for months. Any store that quietly
fills some values and leaves others blank reproduces that bug across every
credential it touches. The summary is what stops it.

**Provenance.** Requested 2026-09-19, generalising the Sentry setup work landed
2026-09-14. That script is the first consumer and the place to start.
