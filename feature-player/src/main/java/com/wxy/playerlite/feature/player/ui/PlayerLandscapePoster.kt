package com.wxy.playerlite.feature.player.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.wxy.playerlite.designsystem.theme.PlayerLiteVisualTheme

@Composable
internal fun PlayerLandscapeHeroPanel(
    trackTitle: String,
    trackArtist: String,
    currentArtistId: String?,
    lyricSummaryText: String,
    lyricSummaryColor: Color,
    lyricSummaryTag: String,
    titleTextStyle: TextStyle,
    artistTextStyle: TextStyle,
    lyricTextStyle: TextStyle,
    layoutMetrics: PlayerScreenLayoutMetrics,
    onArtistClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasLyricSummary = lyricSummaryText.isNotBlank()
    val heroTitleStyle = titleTextStyle.copy(
        // Keep the landscape title compact enough to leave room for the artist and lyric
        // context. The base title token is tuned for portrait, so clamp it explicitly here.
        fontSize = titleTextStyle.fontSize.value.coerceIn(28f, 34f).sp,
        lineHeight = (titleTextStyle.fontSize.value * 1.16f).coerceIn(32f, 38f).sp,
        letterSpacing = 0.sp
    )
    val heroArtistStyle = artistTextStyle.copy(
        fontSize = (artistTextStyle.fontSize.value * 1.06f).coerceIn(18f, 22f).sp,
        lineHeight = (artistTextStyle.fontSize.value * 1.16f).coerceIn(22f, 26f).sp
    )
    val heroLyricStyle = lyricTextStyle.copy(
        fontSize = lyricTextStyle.fontSize.value.coerceIn(14f, 16f).sp,
        lineHeight = (lyricTextStyle.fontSize.value * 1.18f).coerceIn(18f, 21f).sp
    )

    Column(
        modifier = modifier
            .testTag("player_screen_landscape_hero_panel")
            .padding(
                top = layoutMetrics.sectionSpacing * 0.28f,
                end = layoutMetrics.sectionSpacing * 0.25f,
                bottom = layoutMetrics.sectionSpacing * 0.10f
            ),
        verticalArrangement = Arrangement.spacedBy(layoutMetrics.sectionSpacing * 0.34f)
    ) {
        Text(
            text = trackTitle,
            style = heroTitleStyle,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag("player_screen_title")
        )
        Text(
            text = trackArtist,
            style = heroArtistStyle,
            color = Color.White.copy(alpha = if (currentArtistId.isNullOrBlank()) 0.9f else 0.96f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .then(
                    if (currentArtistId.isNullOrBlank()) {
                        Modifier
                    } else {
                        Modifier.clickable(
                            onClickLabel = "打开歌手详情",
                            onClick = onArtistClick
                        )
                    }
                )
                .testTag("player_screen_artist")
        )
        if (hasLyricSummary) {
            Text(
                text = lyricSummaryText,
                style = heroLyricStyle,
                color = lyricSummaryColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(lyricSummaryTag)
            )
        }
    }
}

@Composable
internal fun PlayerLandscapeStageFrame(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .aspectRatio(1f)
                .testTag("player_screen_landscape_cover_frame")
        ) {
            content()
        }
    }
}
