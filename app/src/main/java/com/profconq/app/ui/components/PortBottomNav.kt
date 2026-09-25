package com.profconq.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.profconq.app.ui.i18n.LocalUiStrings
import com.profconq.app.ui.navigation.MainTab
import com.profconq.app.ui.theme.PpDivider
import com.profconq.app.ui.theme.PpGlassSurface
import com.profconq.app.ui.theme.PpNeonCyan
import com.profconq.app.ui.theme.PpNeonGreen
import com.profconq.app.ui.theme.PpTextMuted
import com.profconq.app.ui.theme.rememberGlassBorderBrush

private data class TabItem(
    val tab: MainTab,
    val label: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector,
)

private val TabItemWidth = 76.dp

@Composable
fun PortBottomNav(
    activeTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalUiStrings.current
    val tabs = remember(strings) {
        listOf(
            TabItem(MainTab.Home, strings.tabHome, Icons.Filled.Home, Icons.Outlined.Home),
            TabItem(MainTab.Study, strings.tabStudy, Icons.Filled.AutoStories, Icons.Outlined.AutoStories),
            TabItem(MainTab.Studio, strings.tabStudio, Icons.Filled.GraphicEq, Icons.Outlined.GraphicEq),
            TabItem(MainTab.Reader, strings.tabRead, Icons.AutoMirrored.Filled.MenuBook, Icons.Outlined.MenuBook),
            TabItem(MainTab.Practice, strings.tabVideo, Icons.Filled.FitnessCenter, Icons.Outlined.FitnessCenter),
            TabItem(
                MainTab.Dictionary,
                strings.tabDictionary,
                Icons.Filled.LibraryBooks,
                Icons.Outlined.LibraryBooks,
            ),
            TabItem(MainTab.Profile, strings.tabProfile, Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle),
        )
    }
    val listState = rememberLazyListState()
    LaunchedEffect(activeTab, tabs) {
        val index = tabs.indexOfFirst { it.tab == activeTab }
        if (index >= 0) listState.animateScrollToItem(index)
    }

    val navShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    val borderBrush = rememberGlassBorderBrush()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(navShape)
            .background(PpGlassSurface)
            .border(width = 1.dp, brush = borderBrush, shape = navShape),
    ) {
        HorizontalDivider(color = PpDivider, thickness = 0.5.dp)
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            itemsIndexed(tabs, key = { _, item -> item.tab }) { _, item ->
                val selected = item.tab == activeTab
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(TabItemWidth)
                        .clip(RoundedCornerShape(12.dp))
                        .then(
                            if (selected) {
                                Modifier.background(
                                    brush = Brush.horizontalGradient(
                                        listOf(PpNeonGreen.copy(alpha = 0.18f), PpNeonCyan.copy(alpha = 0.14f)),
                                    ),
                                )
                            } else {
                                Modifier
                            },
                        )
                        .portClickable(onClick = { onTabSelected(item.tab) })
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                ) {
                    Icon(
                        imageVector = if (selected) item.activeIcon else item.inactiveIcon,
                        contentDescription = item.label,
                        tint = if (selected) PpNeonCyan else PpTextMuted,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) PpNeonGreen else PpTextMuted,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
