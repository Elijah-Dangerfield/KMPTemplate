# Deploying the server

The server ships as a Docker image (multi-stage, `installDist`) and is set up to
deploy to [Fly.io](https://fly.io). Postgres + auth live on Supabase, not Fly.

## Prerequisites

- [`flyctl`](https://fly.io/docs/flyctl/install/) installed and `fly auth login`
- A Supabase project (for `DATABASE_URL` + `SUPABASE_URL`)

## One-time setup

```bash
# 1. Pick an app name and update `app = '…'` in apps/server/fly.toml.
fly apps create your-server-name

# 2. Set secrets (injected as env at runtime — never baked into the image).
fly secrets set \
  DATABASE_URL='postgresql://postgres:<url-encoded-pw>@db.<ref>.supabase.co:5432/postgres' \
  SUPABASE_URL='https://<ref>.supabase.co' \
  -a your-server-name

# 3. Deploy from the repo root (the Dockerfile COPYs repo-root paths).
fly deploy --config apps/server/fly.toml --remote-only

# 4. Verify.
curl https://your-server-name.fly.dev/_health   # {"ok":true}
```

`--remote-only` builds on Fly's builders, so you don't need local Docker. To
enable observability later, `fly secrets set SENTRY_DSN=… OTEL_EXPORTER_OTLP_ENDPOINT=…`.

## Steady state

Re-deploy with `fly deploy --config apps/server/fly.toml`. Wire this into CI
(deploy on merge to `main` when `apps/server/**` changes) using a deploy token:
`fly tokens create deploy -a your-server-name`.

Day-to-day: `fly logs`, `fly status`, `fly ssh console`, `fly releases` (and
`fly releases rollback` to revert).

## Server build slimming (`serverOnly`)

This is a KMP monorepo; the server is one Gradle module among Android/iOS
clients. The Docker build passes `-DserverOnly=true`, which makes
[`settings.gradle.kts`](../../settings.gradle.kts) include **only** `:apps:server`
— so the image build needs no Android SDK or Kotlin/Native toolchain. (The flag
name is project-agnostic on purpose, so renaming the project can't desync it from
the Dockerfile.)

The server currently has no `:libraries:*` dependencies. If you add one, the
Docker build fails at configuration with "project not found" until you (a) add an
always-included `include(":libraries:foo")` in `settings.gradle.kts` and (b) add
a `COPY libraries/foo/ libraries/foo/` line to the [Dockerfile](Dockerfile).

## Build the image locally (optional)

```bash
# From the repo root:
docker build -f apps/server/Dockerfile -t kmptemplate-server .
docker run --rm -p 8080:8080 -e DATABASE_URL=... -e SUPABASE_URL=... kmptemplate-server
```

## Environment & secrets

All config is env vars (see [`.env.example`](.env.example) and
[`README.md`](README.md#environment-variables)). In prod, set them via
`fly secrets set`; OS env always wins over the local `.env` file. `DATABASE_URL`
and `SUPABASE_URL` are the two that unlock the full feature set — the server
boots without them (limited mode) so a misconfigured deploy still answers
`/_health`.

## Notes

- **Memory**: `JAVA_OPTS` in `fly.toml` is tuned for a 512MB machine. Bump both
  the heap and the VM `memory` together if you add heavy dependencies.
- **Region**: set `primary_region` close to your Supabase database.
- **Warm baseline**: `min_machines_running = 1` avoids a slow JVM cold-start on
  the first request; set it to `0` to scale to zero if cold latency is fine.
