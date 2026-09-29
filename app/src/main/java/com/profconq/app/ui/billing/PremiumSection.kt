package com.profconq.app.ui.billing

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.profconq.app.billing.BillingNotice
import com.profconq.app.billing.BillingUiState
import com.profconq.app.billing.BuyLock
import com.profconq.app.billing.EntitlementGate
import com.profconq.app.billing.NoticeSeverity
import com.profconq.app.billing.PlanView
import com.profconq.app.billing.PlansDiagnostics
import com.profconq.app.billing.PlansFailure
import com.profconq.app.billing.PremiumStatus
import com.profconq.app.billing.buyLockOf
import com.profconq.app.billing.noticeSeverityOf
import com.profconq.app.ui.components.GlassOutlineButton
import com.profconq.app.ui.components.GradientPrimaryButton
import com.profconq.app.ui.components.MutedText
import com.profconq.app.ui.components.PortCard
import com.profconq.app.ui.components.PortChipShape
import com.profconq.app.ui.components.SectionTitle
import com.profconq.app.ui.components.portClickable
import com.profconq.app.ui.i18n.AppLanguage
import com.profconq.app.ui.i18n.LocalUiStrings
import com.profconq.app.ui.i18n.UiStrings
import com.profconq.app.ui.theme.PpAccent
import com.profconq.app.ui.theme.PpBgElevated
import com.profconq.app.ui.theme.PpBrandNavy
import com.profconq.app.ui.theme.PpDanger
import com.profconq.app.ui.theme.PpHeading
import com.profconq.app.ui.theme.PpNeonCyan
import com.profconq.app.ui.theme.PpNeonGreen
import com.profconq.app.ui.theme.PpSurfaceInput
import com.profconq.app.ui.theme.PpTextMuted
import com.profconq.app.ui.theme.PortTheme
import com.profconq.app.ui.theme.rememberAccentGradientBrush
import com.profconq.app.ui.theme.rememberGlassBorderBrush

/**
 * Profile's Premium block. Plan names and prices are the ones Play reported; the status line is
 * the server snapshot. Nothing here grants access by itself.
 */
@Composable
fun PremiumSection(
    state: BillingUiState,
    signedIn: Boolean,
    onPurchase: (Activity) -> Unit,
    onRestore: () -> Unit,
    onSelectPlan: (String) -> Unit,
    onClearNotice: () -> Unit,
    onPurchaseScreenGone: () -> Unit,
    onRetryPlans: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalUiStrings.current
    val activity = LocalContext.current as? Activity
    // The gate the last server answer supports: an entitlement that waits for a re-check is not a
    // shelf to buy from, and the plan rows say so instead of looking free to pick.
    val gate = state.entitlementGate
    val plansEnabled = !state.busy && !state.plansLoading &&
        state.status != PremiumStatus.Active &&
        gate != EntitlementGate.Active &&
        gate != EntitlementGate.NeedsReverify &&
        gate != EntitlementGate.ReverifyFailed
    // One rule for the button and for the ViewModel's press: a sheet opens only from a list both
    // the server and Play confirmed during this load.
    val lock = buyLockOf(state, signedIn, gate)
    val canBuy = lock == BuyLock.None

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionTitle(title = strings.premiumSectionTitle)
        PortCard {
            // Stacked, never side by side: in a Row each of these two texts only gets the other's
            // leftover width, which squeezed the description into a narrow column against the title.
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = strings.premiumStatusText(state.status),
                    color = statusColor(state.status),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = strings.premiumHint,
                    color = PpTextMuted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            state.expiresAt?.let { expires ->
                Text(
                    text = strings.premiumExpiresAt(expires),
                    color = PpTextMuted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            state.autoRenews?.let { renews ->
                Text(
                    text = if (renews) strings.premiumAutoRenewsOn else strings.premiumAutoRenewsOff,
                    color = PpTextMuted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (state.plans.isNotEmpty()) {
                // One full-width row per plan: Play gives both base plans the same product title,
                // so the localized period is what actually tells them apart.
                state.plans.forEach { plan ->
                    PlanOption(
                        name = strings.premiumPlanName(plan.basePlanId, plan.title),
                        priceLine = strings.premiumPlanPriceLine(plan.basePlanId, plan.price),
                        selected = plan.basePlanId == state.selectedBasePlanId,
                        enabled = plansEnabled,
                        onSelect = { onSelectPlan(plan.basePlanId) },
                    )
                }
            } else if (state.plansFailure == null && !state.plansLoading && state.status == PremiumStatus.Free) {
                MutedText(strings.premiumNoPlans)
            }
            // The reason is shown whether or not rows are still on screen: a button that only
            // disappears tells the tester nothing about which side of the load failed.
            state.plansFailure?.let { failure ->
                MutedText(strings.plansFailureText(failure, state.plansDiagnostics))
                state.plansDiagnostics?.let { MutedText(it.diagnosticCode(failure)) }
            }
            // A disabled button never reads as broken: the reason it waits is named above it.
            when (lock) {
                BuyLock.SignedOut -> MutedText(strings.premiumSignInFirst)
                BuyLock.Loading -> MutedText(strings.plansLoadingWait)
                else -> Unit
            }
            state.notice?.let { notice ->
                Text(
                    text = strings.premiumNoticeText(notice),
                    color = noticeColor(notice),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClearNotice() },
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            GradientPrimaryButton(
                text = strings.premiumBuy,
                // A sheet has to be owned by a live Activity: without one the button says so
                // rather than doing nothing at all.
                onClick = { activity?.let(onPurchase) ?: onPurchaseScreenGone() },
                enabled = canBuy,
                loading = state.busy,
                modifier = Modifier.fillMaxWidth(),
            )
            // Visible whenever the list is empty or the last load did not fully succeed, so the
            // user has a way back that is not "kill the app".
            if (state.plans.isEmpty() || !state.plansFresh) {
                GlassOutlineButton(
                    text = strings.plansRetry,
                    onClick = onRetryPlans,
                    enabled = !state.plansLoading && !state.busy,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            GlassOutlineButton(
                text = strings.premiumRestore,
                onClick = onRestore,
                enabled = signedIn && !state.busy,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * A plan row: its own localized period name over Google's own formatted price. The content sets
 * the height, which may only ever grow, and the selected row keeps the accent fill the chips use.
 */
@Composable
private fun PlanOption(
    name: String,
    priceLine: String,
    selected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
) {
    val shape = PortChipShape
    val fillColor = if (!enabled) PpTextMuted.copy(alpha = 0.4f) else if (selected) PpBrandNavy else PpHeading
    val priceColor = if (!enabled) {
        PpTextMuted.copy(alpha = 0.4f)
    } else if (selected) {
        PpBrandNavy.copy(alpha = 0.75f)
    } else {
        PpTextMuted
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .shadow(
                elevation = if (selected) 10.dp else 0.dp,
                shape = shape,
                ambientColor = PpNeonCyan.copy(alpha = 0.45f),
                spotColor = PpNeonGreen.copy(alpha = 0.4f),
            )
            .clip(shape)
            .background(
                if (selected) {
                    rememberAccentGradientBrush()
                } else {
                    Brush.verticalGradient(listOf(PpSurfaceInput, PpBgElevated))
                },
                shape,
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                brush = if (selected) {
                    Brush.horizontalGradient(
                        listOf(Color.White.copy(alpha = 0.38f), PpNeonCyan.copy(alpha = 0.45f)),
                    )
                } else {
                    rememberGlassBorderBrush()
                },
                shape = shape,
            )
            .portClickable(enabled = enabled, onClick = onSelect)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = name,
            color = fillColor,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = priceLine,
            color = priceColor,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun statusColor(status: PremiumStatus): Color = when (status) {
    PremiumStatus.Active, PremiumStatus.Verifying -> PpAccent
    PremiumStatus.Stale, PremiumStatus.Pending, PremiumStatus.Restoring -> PpHeading
    PremiumStatus.Unavailable -> PpDanger
    PremiumStatus.Free -> PpTextMuted
    PremiumStatus.Loading -> PpTextMuted
}

/** Only a real failure is shown as an error; a cancellation or a restore is not one. */
@Composable
private fun noticeColor(notice: BillingNotice): Color = when (noticeSeverityOf(notice)) {
    NoticeSeverity.Success -> PpAccent
    NoticeSeverity.Info -> PpHeading
    NoticeSeverity.Neutral -> PpTextMuted
    NoticeSeverity.Danger -> PpDanger
}

/**
 * Previews of the block at the narrowest supported width. The real tokens and prices of a device
 * are never reproduced here; the price strings only stand in for whatever shape Google returns.
 */
@Composable
private fun PremiumPreview(
    language: AppLanguage,
    status: PremiumStatus,
    monthlyPrice: String,
    annualPrice: String,
    fontScale: Float,
    widthDp: Float,
    failure: PlansFailure? = null,
    diagnostics: PlansDiagnostics? = null,
    rowsKept: Boolean = false,
) {
    val density = LocalDensity.current
    val state = BillingUiState(
        status = status,
        plans = if (failure == null || rowsKept) {
            listOf(
                PlanView("monthly", "ProfConq Premium", monthlyPrice),
                PlanView("annual", "ProfConq Premium", annualPrice),
            )
        } else {
            emptyList()
        },
        selectedBasePlanId = "monthly",
        plansFresh = failure == null,
        plansFailure = failure,
        plansDiagnostics = diagnostics,
    )
    CompositionLocalProvider(
        LocalUiStrings provides UiStrings.forLanguage(language),
        LocalDensity provides Density(density.density, fontScale),
    ) {
        PortTheme {
            Column(modifier = Modifier.width(widthDp.dp).padding(8.dp)) {
                PremiumSection(
                    state = state,
                    signedIn = true,
                    onPurchase = { },
                    onRestore = { },
                    onSelectPlan = { },
                    onClearNotice = { },
                    onPurchaseScreenGone = { },
                    onRetryPlans = { },
                )
            }
        }
    }
}

@Preview(name = "PT 320dp", showBackground = true, backgroundColor = 0xFF0B1220)
@Composable
private fun PremiumPreviewPtNarrow() = PremiumPreview(
    language = AppLanguage.PT,
    status = PremiumStatus.Free,
    monthlyPrice = "10,99 €",
    annualPrice = "94,99 €",
    fontScale = 1f,
    widthDp = 320f,
)

@Preview(name = "PT 320dp fontScale 1.3", showBackground = true, backgroundColor = 0xFF0B1220)
@Composable
private fun PremiumPreviewPtNarrowLargeText() = PremiumPreview(
    language = AppLanguage.PT,
    status = PremiumStatus.Free,
    monthlyPrice = "10,99 €",
    annualPrice = "94,99 €",
    fontScale = 1.3f,
    widthDp = 320f,
)

@Preview(name = "RU 320dp long price", showBackground = true, backgroundColor = 0xFF0B1220)
@Composable
private fun PremiumPreviewRuLongPrice() = PremiumPreview(
    language = AppLanguage.RU,
    status = PremiumStatus.Free,
    monthlyPrice = "1 234,56 ₽ за месяц",
    annualPrice = "14 999,00 ₽ за год",
    fontScale = 1.3f,
    widthDp = 320f,
)

@Preview(name = "EN 320dp active", showBackground = true, backgroundColor = 0xFF0B1220)
@Composable
private fun PremiumPreviewEnActive() = PremiumPreview(
    language = AppLanguage.EN,
    status = PremiumStatus.Active,
    monthlyPrice = "US$10.99",
    annualPrice = "US$94.99",
    fontScale = 1f,
    widthDp = 320f,
)

/** The empty, blocked state: a reason, the compact code and a working Retry, at 320 dp. */
@Preview(name = "PT 320dp load failure", showBackground = true, backgroundColor = 0xFF0B1220)
@Composable
private fun PremiumPreviewPtLoadFailure() = PremiumPreview(
    language = AppLanguage.PT,
    status = PremiumStatus.Free,
    monthlyPrice = "10,99 €",
    annualPrice = "94,99 €",
    fontScale = 1f,
    widthDp = 320f,
    failure = PlansFailure.PlayQuery,
    diagnostics = PlansDiagnostics(
        responseCode = 2,
        subResponseCode = null,
        productsReturned = 0,
        unfetchedStatusCodes = emptyList(),
        offersSeen = 0,
        basePlanIdsSeen = emptyList(),
        acceptedOffers = 0,
        serverPlanIds = listOf("monthly", "annual"),
    ),
)

/**
 * The outage state this screen exists for: the rows stay, buying is off, the reason and a Retry are
 * both visible. Nothing here is Play data from a real device - the prices only stand in for shape.
 */
@Preview(name = "RU 320dp rows kept, buying off", showBackground = true, backgroundColor = 0xFF0B1220)
@Composable
private fun PremiumPreviewRuRowsKept() = PremiumPreview(
    language = AppLanguage.RU,
    status = PremiumStatus.Free,
    monthlyPrice = "129 ₽",
    annualPrice = "990 ₽",
    fontScale = 1.3f,
    widthDp = 320f,
    failure = PlansFailure.PlayConnection,
    diagnostics = PlansDiagnostics(
        responseCode = -1,
        subResponseCode = null,
        productsReturned = 0,
        unfetchedStatusCodes = emptyList(),
        offersSeen = 0,
        basePlanIdsSeen = emptyList(),
        acceptedOffers = 0,
        serverPlanIds = listOf("monthly", "annual"),
    ),
    rowsKept = true,
)
