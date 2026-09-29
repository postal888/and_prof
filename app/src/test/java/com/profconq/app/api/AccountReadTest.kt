package com.profconq.app.api

import com.profconq.app.data.WordLimitPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Reading the cloud account, tested without a device: which endings of a `/me` call may change what
 * the profile shows, and which limit the profile line may show once an entitlement is applied.
 */
class AccountReadTest {

    private fun fields(
        plan: String?,
        wordCount: Int? = 4,
        wordLimit: Int? = WordLimitPolicy.FREE_LIMIT,
        email: String? = "reader@example.com",
    ) = MeFields(email = email, plan = plan, wordCount = wordCount, wordLimit = wordLimit)

    private fun account(isPremium: Boolean, wordLimit: Int, wordCount: Int = 4) = AccountInfo(
        uid = "uid-1",
        email = "reader@example.com",
        displayName = null,
        isPremium = isPremium,
        wordCount = wordCount,
        wordLimit = wordLimit,
    )

    @Test
    fun refusedSessionIsNotAFreeProfile() {
        // The old code read a 401 as "this account is free with 10 words"; it names a session, not a plan.
        assertEquals(MeRead.Rejected, meReadOf(401))
        assertEquals(MeRead.Rejected, meReadOf(403))
        assertNotEquals(MeRead.Answered, meReadOf(401))
    }

    @Test
    fun unreadableAnswerIsNeitherAGrantNorARevocation() {
        assertEquals(MeRead.Answered, meReadOf(200))
        assertEquals(MeRead.Unreachable, meReadOf(429))
        assertEquals(MeRead.Unreachable, meReadOf(500))
        assertEquals(MeRead.Unreachable, meReadOf(503))
        // 0 is the code of a call that never reached the server.
        assertEquals(MeRead.Unreachable, meReadOf(0))
    }

    @Test
    fun answeredReadKeepsThePlanAndTheLimitItNamed() {
        val premium = accountOf(fields("premium", wordLimit = null), "uid-1", null, null)
        assertTrue(premium.isPremium)
        // A premium account has no limit of its own: the server's JSON null means "no limit".
        assertEquals(WordLimitPolicy.UNLIMITED, premium.wordLimit)

        val free = accountOf(fields("free", wordLimit = 10), "uid-1", null, null)
        assertFalse(free.isPremium)
        assertEquals(10, free.wordLimit)
    }

    @Test
    fun onlyThePremiumPlanNameGrantsPremium() {
        assertFalse(accountOf(fields(null), "uid-1", null, null).isPremium)
        assertFalse(accountOf(fields(""), "uid-1", null, null).isPremium)
        assertFalse(accountOf(fields("Premium"), "uid-1", null, null).isPremium)
    }

    @Test
    fun aMissingCountIsZeroWordsButNeverAMissingPlan() {
        val read = accountOf(fields("free", wordCount = null, wordLimit = null), "uid-1", null, null)
        assertEquals(0, read.wordCount)
        assertEquals(WordLimitPolicy.FREE_LIMIT, read.wordLimit)
        assertFalse(read.isPremium)
    }

    @Test
    fun theReadOwnsTheEmailOnlyUntilAuthKnowsBetter() {
        val fromAuth = accountOf(fields("free", email = "old@example.com"), "uid-1", "new@example.com", "Ann")
        assertEquals("new@example.com", fromAuth.email)
        assertEquals("Ann", fromAuth.displayName)
        val fromRead = accountOf(fields("free", email = "old@example.com"), "uid-1", null, null)
        assertEquals("old@example.com", fromRead.email)
    }

    @Test
    fun appliedPremiumOutweighsAFreeRead() {
        val profile = profileOf(true, account(isPremium = false, wordLimit = WordLimitPolicy.FREE_LIMIT))
        assertTrue(profile.isPremium)
        assertEquals(WordLimitPolicy.UNLIMITED, profile.wordLimit)
    }

    @Test
    fun aPremiumReadThatWasNeverAppliedDoesNotClaimPremium() {
        val profile = profileOf(false, account(isPremium = true, wordLimit = WordLimitPolicy.UNLIMITED))
        assertFalse(profile.isPremium)
        assertEquals(WordLimitPolicy.FREE_LIMIT, profile.wordLimit)
    }

    @Test
    fun anAgreeingReadIsPassedThroughUntouched() {
        // Untouched means no field moves, not the same object: a agreeing premium read may be copied.
        val free = account(isPremium = false, wordLimit = WordLimitPolicy.FREE_LIMIT)
        assertEquals(free, profileOf(false, free))
        val premium = account(isPremium = true, wordLimit = WordLimitPolicy.UNLIMITED)
        assertEquals(premium, profileOf(true, premium))
    }

    @Test
    fun theDisplayedLimitFollowsTheAppliedEntitlement() {
        // The profile line shows "∞" from this threshold; both readings must land on the right side.
        val premium = profileOf(true, account(isPremium = false, wordLimit = WordLimitPolicy.FREE_LIMIT))
        assertTrue(premium.wordLimit >= WordLimitPolicy.UNLIMITED / 2)
        val free = profileOf(false, account(isPremium = true, wordLimit = WordLimitPolicy.UNLIMITED))
        assertFalse(free.wordLimit >= WordLimitPolicy.UNLIMITED / 2)
        assertEquals(WordLimitPolicy.FREE_LIMIT, free.wordLimit)
    }

    @Test
    fun oneAccountReadAtATime() {
        val guard = AccountReadGuard()
        assertTrue(guard.begin())
        // A second caller while the first is in flight is served by that read instead of stacking.
        assertFalse(guard.begin())
        assertFalse(guard.begin())
        guard.end()
        assertTrue(guard.begin())
    }

    @Test
    fun aReadThatThrewStillReleasesTheGuard() {
        val guard = AccountReadGuard()
        assertTrue(guard.begin())
        runCatching { error("refused session") }.also { guard.end() }
        assertTrue(guard.begin())
    }

    @Test
    fun concurrentCallersStartExactlyOneRead() {
        val guard = AccountReadGuard()
        val callers = 8
        val start = CountDownLatch(1)
        val done = CountDownLatch(callers)
        val started = AtomicInteger()
        repeat(callers) {
            Thread {
                start.await()
                if (guard.begin()) started.incrementAndGet()
                done.countDown()
            }.start()
        }
        start.countDown()
        assertTrue(done.await(5, TimeUnit.SECONDS))
        assertEquals(1, started.get())
    }
}
