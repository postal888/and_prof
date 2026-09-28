package com.profconq.app.ui.billing

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.profconq.app.billing.BillingNotice
import com.profconq.app.billing.BillingUiState
import com.profconq.app.billing.NoticeSeverity
import com.profconq.app.billing.PremiumStatus
import com.profconq.app.billing.noticeSeverityOf
import com.profconq.app.ui.components.GlassOutlineButton
import com.profconq.app.ui.components.GradientPrimaryButton
import com.profconq.app.ui.components.MutedText
import com.profconq.app.ui.components.PortCard
import com.profconq.app.ui.components.PortSegmentedControl
import com.profconq.app.ui.components.SectionTitle
import com.profconq.app.ui.i18n.LocalUiStrings
import com.profconq.app.ui.theme.PpAccent
import com.profconq.app.ui.theme.PpDanger
import com.profconq.app.ui.theme.PpHeading
import com.profconq.app.ui.theme.PpTextMuted

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
    modifier: Modifier = Modifier,
) {
    val strings = LocalUiStrings.current
    val activity = LocalContext.current as? Activity
    val labels = state.plans.map { plan -> "${plan.title} · ${plan.price}" }
    val selectedIndex = state.plans.indexOfFirst { it.basePlanId == state.selectedBasePlanId }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionTitle(title = strings.premiumSectionTitle)
        PortCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = strings.premiumStatusText(state.status),
                    color = statusColor(state.status),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = strings.premiumHint,
                    color = PpTextMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            state.expiresAt?.let { expires ->
                Text(
                    text = strings.premiumExpiresAt(expires),
                    color = PpTextMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            state.autoRenews?.let { renews ->
                Text(
                    text = if (renews) strings.premiumAutoRenewsOn else strings.premiumAutoRenewsOff,
                    color = PpTextMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (labels.isNotEmpty()) {
                PortSegmentedControl(
                    items = labels,
                    selectedIndex = selectedIndex.coerceAtLeast(0),
                    onSelect = { index -> state.plans.getOrNull(index)?.let { onSelectPlan(it.basePlanId) } },
                    enabled = !state.busy && state.status != PremiumStatus.Active,
                )
            } else if (state.status == PremiumStatus.Free) {
                MutedText(strings.premiumNoPlans)
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
                enabled = signedIn && !state.busy && state.plans.isNotEmpty() &&
                    state.status != PremiumStatus.Active,
                loading = state.busy,
                modifier = Modifier.fillMaxWidth(),
            )
            GlassOutlineButton(
                text = strings.premiumRestore,
                onClick = onRestore,
                enabled = signedIn && !state.busy,
                compact = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (!signedIn) MutedText(strings.premiumSignInFirst)
        }
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
