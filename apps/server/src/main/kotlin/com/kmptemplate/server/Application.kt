package com.kmptemplate.server

import com.kmptemplate.server.config.ServerConfig
import com.kmptemplate.server.data.InMemoryExampleSource
import com.kmptemplate.server.db.Database
import com.kmptemplate.server.di.ServerComponent
import com.kmptemplate.server.di.create
import com.kmptemplate.server.plugins.BanGate
import com.kmptemplate.server.plugins.JwtVerification
import com.kmptemplate.server.plugins.installAuthentication
import com.kmptemplate.server.plugins.installCors
import com.kmptemplate.server.plugins.installHttpServerTracing
import com.kmptemplate.server.plugins.installObservability
import com.kmptemplate.server.plugins.installOpenTelemetry
import com.kmptemplate.server.plugins.installRateLimits
import com.kmptemplate.server.plugins.installSentry
import com.kmptemplate.server.plugins.installSerialization
import com.kmptemplate.server.plugins.installStatusPages
import com.kmptemplate.server.routes.exampleRoutes
import com.kmptemplate.server.routes.healthRoutes
import com.kmptemplate.server.routes.meRoutes
import com.kmptemplate.server.routes.playerReportRoutes
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import org.slf4j.LoggerFactory

/**
 * Single source of truth for how the app boots. Stays small on purpose — the
 * plugins/ and routes/ packages own their concerns and this wires them together
 * in the right order.
 *
 *  - [module] does production-only setup (observability, the DB connection),
 *    builds the DI graph, and picks the JWT verification strategy, then
 *    delegates to [installApp].
 *  - [installApp] installs the functional plugins + mounts every route. It is
 *    the seam reused by full-stack tests: a test builds a [ServerComponent]
 *    against a Testcontainers database and passes a [JwtVerification.Static]
 *    verifier to exercise the real plugins + routes + DB.
 *
 * Graceful degradation: with no `DATABASE_URL` the server runs in limited mode
 * (health + example); with no `SUPABASE_URL` the authenticated `/v1/me` route
 * isn't mounted. Either way it boots — so you can clone and run with zero config.
 *
 * Order matters: serialization before status pages (so error envelopes encode),
 * auth after serialization (the 401 challenge writes a JSON body), CORS early.
 */
fun Application.module(config: ServerConfig) {
    val logger = LoggerFactory.getLogger("Bootstrap")
    logger.info("Booting server on ${config.http.host}:${config.http.port}")

    // Production-only observability, kept out of [installApp] so tests don't pay
    // for it. Sentry first (so later boot failures are captured), then OTel + HTTP
    // tracing (so subsequent plugins' spans export), then request logging.
    installSentry(config.sentry)
    val openTelemetry = installOpenTelemetry(config.observability)
    installHttpServerTracing(openTelemetry)
    installObservability()

    val database = config.database?.let {
        Database.connect(it).also { logger.info("Database connected and migrations applied") }
    }
    if (database == null) {
        logger.warn("DATABASE_URL not set — limited mode (no DB-backed routes). See apps/server/README.md.")
    }

    val verification = config.supabase?.let { JwtVerification.Jwks(it.jwksUrl, it.expectedIssuer) }
    if (verification == null) {
        logger.warn("SUPABASE_URL not set — authenticated routes (/v1/me) are disabled.")
    }

    val component = database?.let { ServerComponent::class.create(it) }
    // Ban gate needs the DB (moderation reads live in auth.users), so limited
    // mode simply runs without it — same null-safe degradation as everything
    // else here.
    val banGate = component?.let { BanGate(it.moderationRepository, config.accessControl.appealUrl) }
    installApp(component, verification, banGate)
}

/**
 * Installs the functional plugins + every route. Shared by production [module]
 * and full-stack tests (which pass a real [component] + a [JwtVerification.Static]).
 *
 * [component] is null only in limited mode (no `DATABASE_URL`); [verification] is
 * null only when Supabase isn't configured. Health + the example resource are
 * always served.
 */
fun Application.installApp(
    component: ServerComponent?,
    verification: JwtVerification?,
    banGate: BanGate? = null,
) {
    installSerialization()
    installCors()
    installRateLimits()
    installStatusPages()
    if (verification != null) installAuthentication(verification, banGate)

    routing {
        healthRoutes()
        exampleRoutes(component?.exampleSource ?: InMemoryExampleSource())
        if (component != null && verification != null) {
            meRoutes(component.profileRepository)
            playerReportRoutes(component.playerReportRepository)
        }
    }
}
