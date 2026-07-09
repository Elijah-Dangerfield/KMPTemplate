package com.kmptemplate.server.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.extension.kotlin.asContextElement
import io.opentelemetry.instrumentation.ktor.v3_0.KtorServerTelemetry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext

/**
 * Installs automatic HTTP-server tracing: the OpenTelemetry Ktor plugin opens a
 * span per inbound request and closes it on response, so ordinary traffic
 * produces traces with no manual instrumentation. Pass the live SDK from
 * [installOpenTelemetry] (not [GlobalOpenTelemetry]) so the plugin binds to the
 * same instance even if the global slot was locked to noop first.
 */
fun Application.installHttpServerTracing(openTelemetry: OpenTelemetry) {
    install(KtorServerTelemetry) {
        setOpenTelemetry(openTelemetry)
    }
}

internal const val SERVER_TRACER_NAME = "com.kmptemplate.server"

/** One tracer for the whole server; the span name disambiguates. */
internal val serverTracer: Tracer
    get() = GlobalOpenTelemetry.getTracer(SERVER_TRACER_NAME)

/**
 * Run [block] inside a fresh span, parented to the current OTel context (so
 * nested calls form a tree), recorded as `ERROR` on throw, ended in `finally`.
 * Propagates across `suspend` boundaries via [asContextElement]. Cancellation is
 * not an error — the span ends cleanly and re-throws.
 *
 * Use for manual spans around notable work; HTTP requests are traced
 * automatically by [installHttpServerTracing].
 */
internal suspend inline fun <T> withSpan(
    name: String,
    crossinline configure: Span.() -> Unit = {},
    crossinline block: suspend () -> T,
): T {
    val span = serverTracer.spanBuilder(name).startSpan().apply(configure)
    return try {
        withContext(span.asContextElement()) { block() }
    } catch (e: CancellationException) {
        throw e
    } catch (t: Throwable) {
        span.setStatus(StatusCode.ERROR, t.message ?: t::class.java.simpleName)
        span.recordException(t)
        throw t
    } finally {
        span.end()
    }
}
