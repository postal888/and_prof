package com.proficon.app.ui.components



import androidx.compose.foundation.background
import androidx.compose.foundation.border

import androidx.compose.foundation.clickable

import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.Row

import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.foundation.layout.navigationBarsPadding

import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.layout.size

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.automirrored.filled.MenuBook

import androidx.compose.material.icons.filled.AccountCircle

import androidx.compose.material.icons.filled.AutoStories

import androidx.compose.material.icons.filled.FitnessCenter

import androidx.compose.material.icons.filled.Home

import androidx.compose.material.icons.filled.LibraryBooks

import androidx.compose.material.icons.outlined.AccountCircle

import androidx.compose.material.icons.outlined.LibraryBooks

import androidx.compose.material.icons.outlined.AutoStories

import androidx.compose.material.icons.outlined.FitnessCenter

import androidx.compose.material.icons.outlined.Home

import androidx.compose.material.icons.outlined.MenuBook

import androidx.compose.material3.HorizontalDivider

import androidx.compose.material3.Icon

import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.Text

import androidx.compose.runtime.Composable

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip

import androidx.compose.ui.graphics.vector.ImageVector

import androidx.compose.ui.unit.dp

import com.proficon.app.ui.i18n.LocalUiStrings

import com.proficon.app.ui.navigation.MainTab

import com.proficon.app.ui.theme.PpBgElevated
import com.proficon.app.ui.theme.PpBorder
import com.proficon.app.ui.theme.PpDivider
import com.proficon.app.ui.theme.PpSecondary
import com.proficon.app.ui.theme.PpTextMuted



private data class TabItem(

    val tab: MainTab,

    val label: String,

    val activeIcon: ImageVector,

    val inactiveIcon: ImageVector,

)



@Composable

fun PortBottomNav(

    activeTab: MainTab,

    onTabSelected: (MainTab) -> Unit,

    modifier: Modifier = Modifier,

) {

    val strings = LocalUiStrings.current

    val tabs = listOf(

        TabItem(MainTab.Home, strings.tabHome, Icons.Filled.Home, Icons.Outlined.Home),

        TabItem(MainTab.Study, strings.tabStudy, Icons.Filled.AutoStories, Icons.Outlined.AutoStories),

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



    Column(

        modifier = modifier

            .fillMaxWidth()

            .background(PpBgElevated)
            .border(width = 0.5.dp, color = PpBorder),

    ) {

        HorizontalDivider(color = PpDivider, thickness = 0.5.dp)

        Row(

            modifier = Modifier

                .fillMaxWidth()

                .navigationBarsPadding()

                .padding(horizontal = 4.dp, vertical = 6.dp),

            horizontalArrangement = Arrangement.SpaceAround,

            verticalAlignment = Alignment.CenterVertically,

        ) {

            tabs.forEach { item ->

                val selected = item.tab == activeTab

                Column(

                    horizontalAlignment = Alignment.CenterHorizontally,

                    modifier = Modifier

                        .clip(RoundedCornerShape(8.dp))

                        .clickable { onTabSelected(item.tab) }

                        .padding(horizontal = 8.dp, vertical = 4.dp),

                ) {

                    Icon(

                        imageVector = if (selected) item.activeIcon else item.inactiveIcon,

                        contentDescription = item.label,

                        tint = if (selected) PpSecondary else PpTextMuted,

                        modifier = Modifier.size(20.dp),

                    )

                    Text(

                        text = item.label,

                        style = MaterialTheme.typography.labelMedium,

                        color = if (selected) PpSecondary else PpTextMuted,

                    )

                }

            }

        }

    }

}

