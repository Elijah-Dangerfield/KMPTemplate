package com.kmptemplate.server.routes

import com.kmptemplate.server.domain.Profile
import kotlinx.serialization.Serializable

/**
 * Wire DTOs for `/v1/me`. Kept separate from the [Profile] domain type so the
 * JSON shape can evolve without touching the repository. `isAnonymous` mirrors
 * the JWT claim back so the client can prompt the user to claim a guest account.
 * `isNewAccount` is true only on the response that CREATED the profile — the
 * client's auth-outcome classifier uses it to distinguish SignedUp from
 * SignedIn.
 */
@Serializable
data class MeResponse(
    val userId: String,
    val displayName: String,
    val isAnonymous: Boolean,
    val isNewAccount: Boolean = false,
)

@Serializable
data class UpdateMeRequest(
    val displayName: String? = null,
)

fun Profile.toMeResponse(isAnonymous: Boolean, isNewAccount: Boolean = false): MeResponse = MeResponse(
    userId = userId.toString(),
    displayName = displayName,
    isAnonymous = isAnonymous,
    isNewAccount = isNewAccount,
)
