package com.profconq.app.data

import com.profconq.app.auth.AuthUser
import java.security.MessageDigest

/** How the account that just signed in relates to the learning data already on the device. */
enum class DataOwnership {
    /** No account has owned the data yet: the one signing in takes it over. */
    Adopt,

    /** The data is this account's own. */
    Same,

    /** The data belongs to another account and must not be mixed into this one. */
    Foreign,
}

/**
 * A stable, non-reversible key for an account. The email comes first: the server joins a Google
 * sign-in and a website sign-in of one person by email, while their uids differ.
 */
fun accountDataOwnerOf(user: AuthUser): Long {
    val identity = user.email?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        ?.let { "email:$it" }
        ?: "uid:${user.uid}"
    val digest = MessageDigest.getInstance("SHA-256").digest(identity.toByteArray(Charsets.UTF_8))
    var key = 0L
    for (i in 0 until 8) key = (key shl 8) or (digest[i].toLong() and 0xFF)
    // Zero is the stored "no owner" value.
    return if (key == 0L) 1L else key
}

fun dataOwnershipOf(storedOwner: Long?, signedIn: Long): DataOwnership = when (storedOwner) {
    null -> DataOwnership.Adopt
    signedIn -> DataOwnership.Same
    else -> DataOwnership.Foreign
}
