package com.profconq.app.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogProperties
import com.profconq.app.ui.i18n.LocalUiStrings
import com.profconq.app.ui.theme.PpDanger
import com.profconq.app.ui.theme.PpHeading
import com.profconq.app.ui.theme.PpSurface
import com.profconq.app.ui.theme.PpText
import com.profconq.app.ui.theme.PpTextMuted

/**
 * Asked when the signed-in account is not the one the device's words belong to. It cannot be
 * dismissed: leaving it open-ended would let the next sync mix the two accounts.
 */
@Composable
fun AccountSwitchDialog(
    email: String?,
    busy: Boolean,
    onReplace: () -> Unit,
    onSignOut: () -> Unit,
) {
    val strings = LocalUiStrings.current
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text(strings.accountSwitchTitle, color = PpHeading) },
        text = { Text(strings.accountSwitchMessage(email), color = PpText) },
        confirmButton = {
            TextButton(onClick = onReplace, enabled = !busy) {
                Text(strings.accountSwitchReplace, color = PpDanger)
            }
        },
        dismissButton = {
            TextButton(onClick = onSignOut, enabled = !busy) {
                Text(strings.accountSwitchSignOut, color = PpTextMuted)
            }
        },
        containerColor = PpSurface,
    )
}
