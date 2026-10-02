package com.profconq.app.ui

import com.profconq.app.api.AccountInfo
import com.profconq.app.api.ProfconqApiException
import com.profconq.app.billing.PremiumStatus
import com.profconq.app.data.WordLimitPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class WordLimitProjectionTest {
    private fun account(isPremium: Boolean, wordLimit: Int) = AccountInfo(
        uid = "uid-1",
        email = "reader@example.com",
        displayName = null,
        isPremium = isPremium,
        wordCount = 4,
        wordLimit = wordLimit,
    )

    @Test
    fun premiumActiveWithNullAccountShowsUnlimited() {
        val state = WordLimitProjectionState()

        state.publishBillingStatus(PremiumStatus.Active)

        assertEquals(WordLimitProjection.Unlimited, state.value.value)
    }

    @Test
    fun premiumActiveOverridesStaleFreeAccountLimit() {
        val state = WordLimitProjectionState()
        state.publish(
            Result.success(
                account(isPremium = false, wordLimit = WordLimitPolicy.FREE_LIMIT)
            )
        )

        state.publishBillingStatus(PremiumStatus.Active)

        assertEquals(WordLimitProjection.Unlimited, state.value.value)
    }

    @Test
    fun unauthorizedMeDoesNotReplaceUnlimited() {
        val state = WordLimitProjectionState()
        state.publishBillingStatus(PremiumStatus.Active)

        state.publish(Result.failure(ProfconqApiException.Unauthorized()))

        assertEquals(WordLimitProjection.Unlimited, state.value.value)
    }

    @Test
    fun transientMeFailureDoesNotReplaceUnlimited() {
        val state = WordLimitProjectionState()
        state.publishBillingStatus(PremiumStatus.Active)

        state.publish(Result.failure(ProfconqApiException.HttpError(503, null)))

        assertEquals(WordLimitProjection.Unlimited, state.value.value)
    }

    @Test
    fun authoritativeFreeAccountShowsTen() {
        val state = WordLimitProjectionState()
        state.publishBillingStatus(PremiumStatus.Free)
        state.publish(
            Result.success(
                account(isPremium = false, wordLimit = WordLimitPolicy.FREE_LIMIT)
            )
        )

        assertEquals(
            WordLimitProjection.Limited(WordLimitPolicy.FREE_LIMIT),
            state.value.value,
        )
    }

    @Test
    fun premiumStatusAndDisplayedLimitCannotDisagree() {
        val state = WordLimitProjectionState()
        state.publish(
            Result.success(
                account(isPremium = false, wordLimit = WordLimitPolicy.FREE_LIMIT)
            )
        )

        state.publishBillingStatus(PremiumStatus.Active)

        assertNotEquals(
            WordLimitProjection.Limited(WordLimitPolicy.FREE_LIMIT),
            state.value.value,
        )
    }

    @Test
    fun successfulPremiumMePublishesUnlimited() {
        val state = WordLimitProjectionState()
        state.publish(
            Result.success(
                account(isPremium = true, wordLimit = WordLimitPolicy.UNLIMITED)
            )
        )

        assertEquals(WordLimitProjection.Unlimited, state.value.value)
    }

    @Test
    fun premiumAccountIsNotCappedByFreeBillingAnswer() {
        val state = WordLimitProjectionState()
        state.publishBillingStatus(PremiumStatus.Free)

        state.publish(
            Result.success(
                account(isPremium = true, wordLimit = WordLimitPolicy.UNLIMITED)
            )
        )

        assertEquals(WordLimitProjection.Unlimited, state.value.value)
    }

    @Test
    fun processRecreationUsesPersistedEntitlement() {
        val recreated = WordLimitProjectionState()

        recreated.restorePersistedPremium(true)

        assertEquals(WordLimitProjection.Unlimited, recreated.value.value)
    }

    @Test
    fun unknownStateDoesNotPretendFree() {
        val state = WordLimitProjectionState()

        assertEquals(WordLimitProjection.Unknown, state.value.value)
        assertNotEquals(
            WordLimitProjection.Limited(WordLimitPolicy.FREE_LIMIT),
            state.value.value,
        )
    }

    @Test
    fun confirmedSignOutClearsPremiumProjection() {
        val state = WordLimitProjectionState()
        state.publishBillingStatus(PremiumStatus.Active)

        state.clear()

        assertEquals(WordLimitProjection.Unknown, state.value.value)
    }
}
