package com.kmptemplate.server.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.request.path
import org.slf4j.event.Level
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Wire request IDs and structured call logging. Every request gets a unique
 * `X-Request-Id` header surfaced in CallLogging's MDC so logs from one request
 * can be grepped together. Health checks are skipped to keep dev logs readable.
 */
@OptIn(ExperimentalUuidApi::class)
fun Application.installObservability() {
    install(CallId) {
        retrieveFromHeader("X-Request-Id")
        generate { Uuid.random().toString() }
        verify { it.isNotBlank() }
    }
    install(CallLogging) {
        level = Level.INFO
        filter { call -> !call.request.path().startsWith("/_health") }
    }
}
