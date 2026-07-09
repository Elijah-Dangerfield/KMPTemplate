package com.kmptemplate.server.routes

import com.kmptemplate.server.domain.ProfileRepository
import com.kmptemplate.server.domain.UpdateProfileOutcome
import com.kmptemplate.server.plugins.PROFILE_WRITE_LIMIT
import com.kmptemplate.server.plugins.SUPABASE_JWT_AUTH
import com.kmptemplate.server.plugins.isAnonymousUser
import com.kmptemplate.server.plugins.problem
import com.kmptemplate.server.plugins.userId
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch

/**
 * `/v1/me` — the reference authenticated resource.
 *
 *  - `GET`   returns the caller's profile, creating it on first contact.
 *  - `PATCH` updates the display name (rate-limited — a write with a uniqueness
 *    constraint is a name-squatting surface).
 *
 * The caller's id is the JWT `sub` (`call.userId()`) — the client never sends it
 * in the body. Domain outcomes map to HTTP statuses via an exhaustive `when`;
 * the route never lets a domain failure escape as a 500.
 */
fun Route.meRoutes(repository: ProfileRepository) {
    authenticate(SUPABASE_JWT_AUTH) {
        get("/v1/me") {
            val userId = call.userId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
            val profile = repository.findOrCreate(userId)
            call.respond(profile.toMeResponse(isAnonymous = call.isAnonymousUser()))
        }

        rateLimit(RateLimitName(PROFILE_WRITE_LIMIT)) {
            patch("/v1/me") {
                val userId = call.userId() ?: return@patch call.respond(HttpStatusCode.Unauthorized)
                val displayName = call.receive<UpdateMeRequest>().displayName?.trim()
                if (displayName.isNullOrEmpty()) {
                    return@patch call.respond(
                        HttpStatusCode.BadRequest,
                        problem("invalid_display_name", "displayName must not be blank"),
                    )
                }
                when (val outcome = repository.updateDisplayName(userId, displayName)) {
                    is UpdateProfileOutcome.Success ->
                        call.respond(outcome.profile.toMeResponse(isAnonymous = call.isAnonymousUser()))

                    UpdateProfileOutcome.DisplayNameTaken ->
                        call.respond(HttpStatusCode.Conflict, problem("display_name_taken", "That display name is taken"))

                    UpdateProfileOutcome.NotFound ->
                        call.respond(HttpStatusCode.NotFound, problem("not_found", "Profile not found"))
                }
            }
        }
    }
}
