package com.kmptemplate.server.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS

/**
 * Permissive CORS for the template default. Tighten [anyHost] to your real web
 * origins before shipping a browser client.
 */
fun Application.installCors() {
    install(CORS) {
        anyHost()
        allowHeader("Content-Type")
        allowHeader("Authorization")
        allowHeader("X-Request-Id")
    }
}
