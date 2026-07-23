package com.kmptemplate.server.config

/**
 * Root container for everything the server reads from the environment.
 *
 * Parsed once at startup from [Env] and then passed around as plain values.
 * Code that depends on config takes the specific sub-config it needs, not the
 * whole tree — keeps test wiring honest.
 *
 * Convention for adding a field (the whole point of this file):
 *  - boot-critical (server can't run without it) → `env.require("KEY")` — fail fast.
 *  - optional with a sensible default            → `env.int("KEY", default)` / `env["KEY"] ?: default`.
 *  - optional, feature degrades when absent      → nullable `env["KEY"]`, branch at the call site.
 *
 * [database] is nullable on purpose: with no `DATABASE_URL` the server still
 * boots (health + the example resource) so you can clone-and-run with zero
 * config. See apps/server/README.md → Quick start.
 */
data class ServerConfig(
    val http: HttpConfig,
    val database: DatabaseConfig?,
    val supabase: SupabaseConfig?,
    val sentry: SentryConfig,
    val observability: ObservabilityConfig,
    val accessControl: AccessControlConfig,
) {
    companion object {
        fun fromEnv(env: Env = Env()): ServerConfig = ServerConfig(
            http = HttpConfig.fromEnv(env),
            database = DatabaseConfig.fromEnv(env),
            supabase = SupabaseConfig.fromEnv(env),
            sentry = SentryConfig.fromEnv(env),
            observability = ObservabilityConfig.fromEnv(env),
            accessControl = AccessControlConfig.fromEnv(env),
        )
    }
}

/**
 * Settings for the account-access gate that blocks banned users on every
 * authenticated route.
 *
 * [appealUrl] is handed to a blocked client verbatim in the `403` body so the
 * app can offer an "appeal this" link. Null when unset — the client then shows
 * the block without an appeal affordance, which is fine for V1 (the gate still
 * works, the user just has no in-app appeal path). Set `APPEAL_URL` once a
 * support/appeal page exists.
 */
data class AccessControlConfig(
    val appealUrl: String?,
) {
    companion object {
        fun fromEnv(env: Env): AccessControlConfig = AccessControlConfig(
            appealUrl = env["APPEAL_URL"]?.takeIf { it.isNotBlank() },
        )
    }
}

/**
 * Server-side Sentry. No-op unless [dsn] is set, so the default build reports
 * nothing until you wire a project. [environment]/[release] tag every event.
 */
data class SentryConfig(
    val dsn: String?,
    val environment: String,
    val release: String?,
) {
    companion object {
        fun fromEnv(env: Env): SentryConfig = SentryConfig(
            dsn = env["SENTRY_DSN"],
            environment = env["SENTRY_ENVIRONMENT"] ?: "dev",
            release = env["SENTRY_RELEASE"],
        )
    }
}

/**
 * OpenTelemetry exporter wiring. With [otlpEndpoint] unset, the SDK still boots
 * but exports to stdout (spans/logs/metrics show in your logs). Set it (standard
 * env name, so vendor tooling drops in) to ship OTLP/HTTP instead.
 */
data class ObservabilityConfig(
    val otlpEndpoint: String?,
    val otlpHeaders: String?,
    val serviceName: String,
    val environment: String,
    val release: String?,
) {
    companion object {
        fun fromEnv(env: Env): ObservabilityConfig = ObservabilityConfig(
            otlpEndpoint = env["OTEL_EXPORTER_OTLP_ENDPOINT"],
            otlpHeaders = env["OTEL_EXPORTER_OTLP_HEADERS"],
            serviceName = env["OTEL_SERVICE_NAME"] ?: "kmptemplate-server",
            environment = env["OTEL_DEPLOYMENT_ENVIRONMENT"] ?: env["SENTRY_ENVIRONMENT"] ?: "dev",
            release = env["OTEL_SERVICE_VERSION"] ?: env["SENTRY_RELEASE"],
        )
    }
}

/**
 * Settings for verifying Supabase-issued JWTs.
 *
 * Supabase signs JWTs with ES256 (asymmetric). The server fetches the project's
 * public keys from `<projectUrl>/auth/v1/.well-known/jwks.json` at runtime and
 * verifies signatures against them — no shared secret lives on the server.
 *
 * Nullable: with no `SUPABASE_URL` the server boots without authenticated routes
 * (the `/v1/me` endpoint isn't mounted). Set it to enable auth.
 */
data class SupabaseConfig(
    /** e.g. `https://abcdefgh.supabase.co`. */
    val projectUrl: String,
) {
    /** Issuer the Auth service stamps on every JWT it issues. */
    val expectedIssuer: String get() = "$projectUrl/auth/v1"

    /** Discovery endpoint for the project's public signing keys. */
    val jwksUrl: String get() = "$projectUrl/auth/v1/.well-known/jwks.json"

    companion object {
        fun fromEnv(env: Env): SupabaseConfig? =
            env["SUPABASE_URL"]?.trimEnd('/')?.let { SupabaseConfig(projectUrl = it) }
    }
}

data class HttpConfig(
    val host: String,
    val port: Int,
) {
    companion object {
        fun fromEnv(env: Env): HttpConfig = HttpConfig(
            host = env["SERVER_HOST"] ?: "0.0.0.0",
            port = env.int("SERVER_PORT", default = 8080),
        )
    }
}

/**
 * Connection settings for the Postgres pool. We accept a single `DATABASE_URL`
 * (12-factor) and parse user + password out of it so Hikari can take them as
 * separate fields — some drivers / pools reject JDBC URLs with embedded
 * credentials.
 */
data class DatabaseConfig(
    val jdbcUrl: String,
    val username: String,
    val password: String,
    val poolMaxSize: Int,
    val poolMinIdle: Int,
) {
    companion object {
        /** Returns null when `DATABASE_URL` is unset — the server then runs in limited mode. */
        fun fromEnv(env: Env): DatabaseConfig? {
            val raw = env["DATABASE_URL"] ?: return null
            val parsed = ParsedPostgresUrl.parse(raw)
            return DatabaseConfig(
                jdbcUrl = parsed.jdbcUrl,
                username = parsed.username,
                password = parsed.password,
                poolMaxSize = env.int("DATABASE_POOL_MAX_SIZE", default = 10),
                poolMinIdle = env.int("DATABASE_POOL_MIN_IDLE", default = 2),
            )
        }
    }
}

/**
 * `postgresql://user:pass@host:port/db?params` →
 *   - jdbcUrl: `jdbc:postgresql://host:port/db?params` (no credentials)
 *   - username, password: extracted and URL-decoded
 *
 * Internal helper, public only for tests.
 */
internal data class ParsedPostgresUrl(
    val jdbcUrl: String,
    val username: String,
    val password: String,
) {
    companion object {
        fun parse(raw: String): ParsedPostgresUrl {
            require(raw.startsWith("postgresql://") || raw.startsWith("postgres://")) {
                "DATABASE_URL must start with postgresql:// (got: ${raw.take(20)}…)"
            }
            val noScheme = raw.substringAfter("://")
            val atIndex = noScheme.lastIndexOf('@')
            require(atIndex > 0) { "DATABASE_URL must include `user:password@host` credentials" }
            val credsPart = noScheme.substring(0, atIndex)
            val hostPart = noScheme.substring(atIndex + 1)
            val colon = credsPart.indexOf(':')
            require(colon > 0) { "DATABASE_URL credentials must be `user:password`" }
            val user = urlDecode(credsPart.substring(0, colon))
            val pass = urlDecode(credsPart.substring(colon + 1))
            return ParsedPostgresUrl(
                jdbcUrl = "jdbc:postgresql://$hostPart",
                username = user,
                password = pass,
            )
        }

        private fun urlDecode(s: String): String =
            java.net.URLDecoder.decode(s, Charsets.UTF_8)
    }
}
