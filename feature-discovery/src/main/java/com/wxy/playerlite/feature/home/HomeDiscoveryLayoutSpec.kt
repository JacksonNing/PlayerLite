package com.wxy.playerlite.feature.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object HomeDiscoveryLayoutSpec {
    val rowContentPadding = PaddingValues(horizontal = 0.dp)
    val bannerContentPadding = PaddingValues(end = 64.dp)

    val pageTopPadding = 20.dp
    val headerAvatarSize = 48.dp
    val bannerHeight = 190.dp
    val discoveryCardWidth = 112.dp
    val discoveryCardHeight = 112.dp
    val compactCardWidth = 112.dp
    val compactCardHeight = 104.dp
    val compactImageSize = 48.dp
    val itemSpacing = 12.dp
    val sectionSpacing = 28.dp
    val songCardHeight = 72.dp
    const val songColumnItemCount = 3
    val songColumnItemSpacing = 2.dp
    val songSectionContentPadding = PaddingValues(0.dp)
    val searchBoxCornerRadius = 18.dp
    val searchBoxHeight = 52.dp
    val searchBoxShadowElevation = 0.dp
    val bannerCardCornerRadius = 18.dp
    val standardCardCornerRadius = 14.dp
    val compactCardCornerRadius = 18.dp
    val songCardCoverSize = 48.dp
    val songCardCoverCornerRadius = 12.dp
    val songCardMenuButtonSize = 36.dp
    val songDividerStartPadding = 112.dp
    val songDividerVerticalPadding = 0.dp
    const val songDividerAlpha = 0.46f
    val sectionTitleFontSize = 22.sp
    val sectionTitleLineHeight = 28.sp

    const val sectionTitleMaxLines = 1
    const val titleMaxLines = 1
    const val subtitleMaxLines = 1
    const val bannerBadgeUsesTextOnlyStyle = true
    const val bannerImageFillsCard = true
    const val bannerUsesInfiniteLoop = true
    const val dailyShortcutUsesCompactIconStyle = true
    const val discoveryImageUsesFullBleed = true
    const val discoveryImageAspectRatio = 1f
    const val virtualBannerPageCount = 4_000

    private val dailyShortcutPalette = listOf(
        Color(0xFFFFF1E6),
        Color(0xFFFFE8EE),
        Color(0xFFEAF4FF),
        Color(0xFFF1EDFF),
        Color(0xFFE8FAF1),
        Color(0xFFFFF4DA)
    )

    fun usesCarousel(layout: HomeSectionLayout): Boolean {
        return layout == HomeSectionLayout.BANNER
    }

    fun initialBannerPage(itemCount: Int): Int {
        if (itemCount <= 1) {
            return 0
        }
        val midpoint = virtualBannerPageCount / 2
        return midpoint - (midpoint % itemCount)
    }

    fun recenterBannerPage(currentPage: Int, itemCount: Int): Int {
        if (itemCount <= 1 || !bannerUsesInfiniteLoop) {
            return 0
        }
        val nearStart = currentPage < itemCount
        val nearEnd = currentPage > virtualBannerPageCount - itemCount - 1
        if (!nearStart && !nearEnd) {
            return currentPage
        }
        return initialBannerPage(itemCount) + positiveMod(currentPage, itemCount)
    }

    fun dailyShortcutBackgroundColor(seed: String): Color {
        val stableSeed = seed.ifBlank { "home-shortcut" }
        return dailyShortcutPalette[positiveMod(stableSeed.hashCode(), dailyShortcutPalette.size)]
    }

    private fun positiveMod(value: Int, divisor: Int): Int {
        return ((value % divisor) + divisor) % divisor
    }
}
