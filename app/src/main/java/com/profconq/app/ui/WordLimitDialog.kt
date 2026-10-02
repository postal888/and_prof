package com.profconq.app.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.profconq.app.ui.i18n.LocalUiStrings
import com.profconq.app.ui.theme.PpAccent
import com.profconq.app.ui.theme.PpHeading
import com.profconq.app.ui.theme.PpSurface
import com.profconq.app.ui.theme.PpText
import com.profconq.app.ui.theme.PpTextMuted

/** Shown when a free account tries to save a word past its limit. */
@Composable
fun WordLimitDialog(
    limit: Int,
    signedIn: Boolean,
    onUpgrade: () -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalUiStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.wordLimitDialogTitle, color = PpHeading) },
        text = { Text(strings.wordLimitDialogMessage(limit, signedIn), color = PpText) },
        confirmButton = {
            TextButton(onClick = onUpgrade) {
                Text(strings.wordLimitDialogUpgrade, color = PpAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.wordLimitDialogLater, color = PpTextMuted)
            }
        },
        containerColor = PpSurface,
    )
}
