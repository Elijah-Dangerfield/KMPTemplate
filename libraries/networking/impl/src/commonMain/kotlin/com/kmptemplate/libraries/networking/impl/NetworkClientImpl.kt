package com.kmptemplate.libraries.networking.impl

import com.kmptemplate.libraries.core.BuildInfo
import com.kmptemplate.libraries.core.logging.KLog
import com.kmptemplate.libraries.networking.AuthTokenProvider
import com.kmptemplate.libraries.networking.NetworkClient
import com.kmptemplate.libraries.networking.NetworkConfig
import com.kmptemplate.libraries.networking.NetworkJson
import com.kmptemplate.libraries.networking.NetworkReachability
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class NetworkClientImpl(
    private val config: NetworkConfig,
    private val authTokenProvider: AuthTokenProvider,
    private val reachability: NetworkReachability,
) : NetworkClient {

    override val client: HttpClient by lazy {
        HttpClient {
            applyCommonConfig(config, reachability)
        }
    }

    override val authenticatedClient: HttpClient by lazy {
        HttpClient {
            applyCommonConfig(config, reachability)
            install(Auth) {
                bearer {
                    loadTokens {
                        val token = authTokenProvider.getAccessToken() ?: return@loadTokens null
                        BearerTokens(accessToken = token, refreshToken = "")
                    }
                    refreshTokens {
                        val token = authTokenProvider.refreshAccessToken() ?: return@refreshTokens null
                        BearerTokens(accessToken = token, refreshToken = "")
                    }
                    sendWithoutRequest { true }
                }
            }
        }
    }
}

private fun HttpClientConfig<*>.applyCommonConfig(
    config: NetworkConfig,
    reachability: NetworkReachability,
) {
    install(ContentNegotiation) {
        json(NetworkJson)
    }
    // Witnessed reachability: a response (any status, even 4xx/5xx) means the
    // round-trip worked; a failure *without* a response (timeout / IO / DNS /
    // captive portal) means it didn't. This is what lets the offline banner
    // reflect "actually online" rather than just the OS's "there's a path."
    HttpResponseValidator {
        validateResponse { reachability.reportReachable() }
        handleResponseExceptionWithRequest { cause, _ ->
            // A ResponseException means the server answered (a 4xx/5xx) — the
            // network is fine. Anything else never reached the server.
            if (cause !is ResponseException) {
                reachability.reportUnreachable()
            }
        }
    }
    install(HttpTimeout) {
        requestTimeoutMillis = config.requestTimeoutMillis
        connectTimeoutMillis = config.requestTimeoutMillis
        socketTimeoutMillis = config.requestTimeoutMillis
    }
    install(DefaultRequest) {
        if (config.baseUrl.isNotBlank()) url(config.baseUrl)
        headers.append(HttpHeaders.Accept, "application/json")
        headers.append(HttpHeaders.ContentType, "application/json")
    }
    if (BuildInfo.isDebug) {
        install(Logging) {
            level = LogLevel.INFO
            logger = object : Logger {
                private val log = KLog.withTag("Network")
                override fun log(message: String) {
                    log.d { message }
                }
            }
        }
    }
    expectSuccess = true
}
