package com.kmptemplate.server.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.JWTVerifier
import com.kmptemplate.server.domain.Profile
import com.kmptemplate.server.domain.ProfileRepository
import com.kmptemplate.server.domain.UpdateProfileOutcome
import com.kmptemplate.server.domain.UserId
import com.kmptemplate.server.plugins.JwtVerification
import com.kmptemplate.server.plugins.installAuthentication
import com.kmptemplate.server.plugins.installRateLimits
import com.kmptemplate.server.plugins.installSerialization
import com.kmptemplate.server.plugins.installStatusPages
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.util.Date
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Route-level tests for `/v1/me`. The repository is faked; this exercises the
 * HTTP + JWT-validation layer. Production verifies via JWKS (ES256); tests mint
 * HS256 tokens they control and pass a matching verifier through the
 * [JwtVerification.Static] seam — fast and hermetic, no JWKS fetch.
 *
 * Copy this shape for a new authenticated route.
 */
@OptIn(ExperimentalTime::class)
class MeRoutesTest {

    private val testIssuer = "https://test-project.supabase.co/auth/v1"
    private val testSecret = "0123456789abcdef0123456789abcdef0123456789abcdef"
    private val userId = UserId(UUID.fromString("11111111-1111-1111-1111-111111111111"))

    private val testVerifier: JWTVerifier = JWT.require(Algorithm.HMAC256(testSecret))
        .withIssuer(testIssuer)
        .withAudience("authenticated")
        .build()

    @Test
    fun me_returnsProfile_whenJwtValid() = runTest {
        withMeApp(FakeProfileRepository(fakeProfile())) { client ->
            val resp = client.get("/v1/me") {
                header(HttpHeaders.Authorization, "Bearer ${validJwt()}")
            }
            assertEquals(HttpStatusCode.OK, resp.status)
            val body = resp.body<MeResponse>()
            assertEquals(userId.toString(), body.userId)
            assertEquals("FakeName", body.displayName)
            assertEquals(false, body.isAnonymous)
        }
    }

    @Test
    fun me_marksAnonymous_whenClaimPresent() = runTest {
        withMeApp(FakeProfileRepository(fakeProfile())) { client ->
            val resp = client.get("/v1/me") {
                header(HttpHeaders.Authorization, "Bearer ${validJwt(isAnonymous = true)}")
            }
            assertEquals(HttpStatusCode.OK, resp.status)
            assertTrue(resp.body<MeResponse>().isAnonymous)
        }
    }

    @Test
    fun me_createsProfile_onFirstContact() = runTest {
        val repo = FakeProfileRepository(existing = null)
        withMeApp(repo) { client ->
            val resp = client.get("/v1/me") {
                header(HttpHeaders.Authorization, "Bearer ${validJwt()}")
            }
            assertEquals(HttpStatusCode.OK, resp.status)
            assertEquals(1, repo.findOrCreateCalls)
        }
    }

    @Test
    fun me_returns401_whenAuthHeaderMissing() = runTest {
        withMeApp(FakeProfileRepository(fakeProfile())) { client ->
            assertEquals(HttpStatusCode.Unauthorized, client.get("/v1/me").status)
        }
    }

    @Test
    fun me_returns401_whenSignatureWrong() = runTest {
        withMeApp(FakeProfileRepository(fakeProfile())) { client ->
            val resp = client.get("/v1/me") {
                header(HttpHeaders.Authorization, "Bearer ${validJwt(secret = "wrong-secret-wrong-secret-wrong!")}")
            }
            assertEquals(HttpStatusCode.Unauthorized, resp.status)
        }
    }

    @Test
    fun patchMe_updatesDisplayName() = runTest {
        withMeApp(FakeProfileRepository(fakeProfile())) { client ->
            val resp = client.patch("/v1/me") {
                header(HttpHeaders.Authorization, "Bearer ${validJwt()}")
                contentType(ContentType.Application.Json)
                setBody(UpdateMeRequest(displayName = "Ada"))
            }
            assertEquals(HttpStatusCode.OK, resp.status)
            assertEquals("Ada", resp.body<MeResponse>().displayName)
        }
    }

    @Test
    fun patchMe_returns409_whenDisplayNameTaken() = runTest {
        withMeApp(FakeProfileRepository(fakeProfile())) { client ->
            val resp = client.patch("/v1/me") {
                header(HttpHeaders.Authorization, "Bearer ${validJwt()}")
                contentType(ContentType.Application.Json)
                setBody(UpdateMeRequest(displayName = "taken"))
            }
            assertEquals(HttpStatusCode.Conflict, resp.status)
        }
    }

    private suspend fun withMeApp(repo: ProfileRepository, block: suspend (HttpClient) -> Unit) {
        testApplication {
            application {
                installSerialization()
                installRateLimits()
                installStatusPages()
                installAuthentication(JwtVerification.Static(testVerifier))
                routing { meRoutes(repo) }
            }
            val client = createClient {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            }
            block(client)
        }
    }

    private fun validJwt(isAnonymous: Boolean = false, secret: String = testSecret): String =
        JWT.create()
            .withIssuer(testIssuer)
            .withAudience("authenticated")
            .withSubject(userId.value.toString())
            .withExpiresAt(Date(System.currentTimeMillis() + 60_000))
            .apply { if (isAnonymous) withClaim("is_anonymous", true) }
            .sign(Algorithm.HMAC256(secret))

    private fun fakeProfile(): Profile = Profile(
        userId = userId,
        displayName = "FakeName",
        createdAt = Instant.fromEpochSeconds(0),
        updatedAt = Instant.fromEpochSeconds(0),
    )

    private class FakeProfileRepository(private var existing: Profile?) : ProfileRepository {
        var findOrCreateCalls = 0

        override suspend fun findOrCreate(userId: UserId): Profile {
            findOrCreateCalls++
            return existing ?: Profile(
                userId = userId,
                displayName = "FakeName",
                createdAt = Instant.fromEpochSeconds(0),
                updatedAt = Instant.fromEpochSeconds(0),
            ).also { existing = it }
        }

        override suspend fun updateDisplayName(userId: UserId, displayName: String): UpdateProfileOutcome {
            if (displayName == "taken") return UpdateProfileOutcome.DisplayNameTaken
            val base = existing ?: Profile(
                userId, "FakeName", Instant.fromEpochSeconds(0), Instant.fromEpochSeconds(0),
            )
            val updated = base.copy(displayName = displayName)
            existing = updated
            return UpdateProfileOutcome.Success(updated)
        }
    }
}
