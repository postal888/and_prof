package com.profconq.app.data

import com.profconq.app.auth.AuthUser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AccountDataOwnerTest {
    @Test
    fun googleAndWebsiteSignInOfOneEmailOwnTheSameData() {
        val google = AuthUser(uid = "firebase-uid", displayName = null, email = "Reader@Example.com")
        val site = AuthUser(uid = "site-uuid", displayName = null, email = " reader@example.com ")

        assertEquals(accountDataOwnerOf(google), accountDataOwnerOf(site))
    }

    @Test
    fun differentEmailsAreDifferentOwners() {
        val a = AuthUser(uid = "u", displayName = null, email = "a@example.com")
        val b = AuthUser(uid = "u", displayName = null, email = "b@example.com")

        assertNotEquals(accountDataOwnerOf(a), accountDataOwnerOf(b))
    }

    @Test
    fun accountWithoutEmailFallsBackToUid() {
        val a = AuthUser(uid = "uid-a", displayName = null, email = null)
        val b = AuthUser(uid = "uid-b", displayName = null, email = "")

        assertNotEquals(accountDataOwnerOf(a), accountDataOwnerOf(b))
    }

    @Test
    fun firstSignInAdoptsExistingData() {
        assertEquals(DataOwnership.Adopt, dataOwnershipOf(null, 42L))
    }

    @Test
    fun sameOwnerKeepsData() {
        assertEquals(DataOwnership.Same, dataOwnershipOf(42L, 42L))
    }

    @Test
    fun otherOwnerIsForeign() {
        assertEquals(DataOwnership.Foreign, dataOwnershipOf(41L, 42L))
    }
}
