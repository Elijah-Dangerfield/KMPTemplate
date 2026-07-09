package com.kmptemplate.server.plugins

import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.interfaces.JWTVerifier
import com.kmptemplate.server.domain.UserId
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTAuthenticationProvider
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import java.net.URI
import java.util.concurrent.TimeUnit

/** Authentication-realm name; used by `authenticate(SUPABASE_JWT_AUTH)` in routes. */
const val SUPABASE_JWT_AUTH = "supabase-jwt"

/**
 * How inbound JWTs are verified — the seam that makes auth testable.
 *
 *  - [Jwks] is production: fetch Supabase's public ES256 keys from the JWKS
 *    endpoint and verify signatures against them.
 *  - [Static] is for tests: verify against a caller-supplied verifier (e.g.
 *    HS256 + a known secret), so a test can mint matching tokens with no
 *    network.
 *
 * Both run the identical `validate` + `challenge` behaviour (UUID `sub`, JSON
 * 401) — only the signature check differs.
 */
sealed interface JwtVerification {
    data class Jwks(val jwksUrl: String, val issuer: String) : JwtVerification
    data class Static(val verifier: JWTVerifier) : JwtVerification
}

/**
 * Installs JWT auth under [SUPABASE_JWT_AUTH] with the given [verification]
 * strategy. The single seam shared by production [com.kmptemplate.server.module]
 * and full-stack tests.
 */
fun Application.installAuthentication(verification: JwtVerification) {
    install(Authentication) {
        jwt(SUPABASE_JWT_AUTH) {
            when (verification) {
                is JwtVerification.Jwks -> {
                    val jwkProvider = JwkProviderBuilder(URI(verification.jwksUrl).toURL())
                        .cached(10, 24, TimeUnit.HOURS)
                        .rateLimited(10, 1, TimeUnit.MINUTES)
                        .build()
                    verifier(jwkProvider, verification.issuer) {
                        withAudience("authenticated")
                    }
                }

                is JwtVerification.Static -> verifier(verification.verifier)
            }
            validateAndChallengeForUserId()
        }
    }
}

private fun JWTAuthenticationProvider.Config.validateAndChallengeForUserId() {
    validate { credential ->
        // `sub` must be a UUID. Anything else is a malformed token → 401.
        val sub = credential.payload.subject
        if (sub.isNullOrBlank() || UserId.parse(sub) == null) null
        else JWTPrincipal(credential.payload)
    }
    challenge { _, _ ->
        call.respond(
            HttpStatusCode.Unauthorized,
            mapOf("error" to mapOf("code" to "unauthorized", "message" to "Missing or invalid access token")),
        )
    }
}

/**
 * The caller's `auth.users.id` from the validated JWT. Only meaningful inside an
 * `authenticate(SUPABASE_JWT_AUTH) { … }` block — null outside it.
 */
fun ApplicationCall.userId(): UserId? {
    val principal = principal<JWTPrincipal>() ?: return null
    val sub = principal.payload.subject ?: return null
    return UserId.parse(sub)
}

/**
 * Whether the caller's JWT was issued for an anonymous user. Supabase marks
 * anonymous users with the `is_anonymous: true` claim.
 */
fun ApplicationCall.isAnonymousUser(): Boolean {
    val principal = principal<JWTPrincipal>() ?: return false
    return principal.payload.getClaim("is_anonymous").asBoolean() ?: false
}
