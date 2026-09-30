package com.profconq.app.ui.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumPlanPresentationTest {
    private fun activeMonthly() = planOptionPresentation(
        name = "Monthly",
        priceLine = "$9.99 / month",
        selected = true,
        enabled = false,
        premiumActive = true,
    )

    private fun unavailableAnnual() = planOptionPresentation(
        name = "Annual",
        priceLine = "$99.99 / year",
        selected = false,
        enabled = false,
        premiumActive = true,
    )

    @Test
    fun activeMonthlyRemainsDisabled() {
        assertFalse(activeMonthly().enabled)
    }

    @Test
    fun activeMonthlyLabelRemainsVisible() {
        val monthly = activeMonthly()

        assertTrue(monthly.name.isNotBlank())
        assertTrue(monthly.priceLine.isNotBlank())
        assertEquals(1f, monthly.contentAlpha)
    }

    @Test
    fun otherPlanRemainsDisabledUntilReplacementFlow() {
        assertFalse(unavailableAnnual().enabled)
    }

    @Test
    fun premiumPlanButtonsDoNotLaunchBillingFlow() {
        var launches = 0

        activeMonthly().select { launches++ }
        unavailableAnnual().select { launches++ }

        assertEquals(0, launches)
    }

    @Test
    fun activeAndUnavailablePlansHaveDistinctSemantics() {
        val active = activeMonthly()
        val unavailable = unavailableAnnual()

        assertEquals(PlanOptionSemantics.Current, active.semantics)
        assertEquals(PlanOptionSemantics.Unavailable, unavailable.semantics)
        assertNotEquals(active.semantics, unavailable.semantics)
    }
}
