package com.profconq.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.profconq.app.ui.i18n.LocalUiStrings
import com.profconq.app.ui.theme.PpTextMuted

private val LampColors = listOf(
    Color(0xFFE36A5D),
    Color(0xFFE8A838),
    Color(0xFFF3B318),
    Color(0xFF83BF59),
    Color(0xFF6AB673),
)

@Composable
fun VocabLampsRow(
    level: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalUiStrings.current
    val labels = listOf(
        strings.dictionaryMastery1,
        strings.dictionaryMastery2,
        strings.dictionaryMastery3,
        strings.dictionaryMastery4,
        strings.dictionaryMastery5,
    )
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        LampColors.forEachIndexed { index, color ->
            val n = index + 1
            IconButton(onClick = { onSelect(n) }, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Filled.Lightbulb,
                    contentDescription = "$n/5 · ${labels[index]}",
                    tint = if (level >= n) color else PpTextMuted.copy(alpha = 0.38f),
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}
