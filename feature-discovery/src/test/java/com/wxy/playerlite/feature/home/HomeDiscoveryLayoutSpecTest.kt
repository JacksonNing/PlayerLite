package com.wxy.playerlite.feature.home

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeDiscoveryLayoutSpecTest {
    @Test
    fun bannerSections_shouldUseCarouselPresentation() {
        assertTrue(HomeDiscoveryLayoutSpec.usesCarousel(HomeSectionLayout.BANNER))
    }

    @Test
    fun horizontalSections_shouldUseTighterEdgePaddingForDenserViewport() {
        assertEquals(
            0.dp,
            HomeDiscoveryLayoutSpec.rowContentPadding.calculateLeftPadding(LayoutDirection.Ltr)
        )
        assertEquals(
            0.dp,
            HomeDiscoveryLayoutSpec.rowContentPadding.calculateRightPadding(LayoutDirection.Ltr)
        )
    }

    @Test
    fun cards_shouldUseFixedHeightsToAvoidViewportJitter() {
        assertEquals(190.dp, HomeDiscoveryLayoutSpec.bannerHeight)
        assertEquals(112.dp, HomeDiscoveryLayoutSpec.discoveryCardHeight)
        assertEquals(104.dp, HomeDiscoveryLayoutSpec.compactCardHeight)
        assertEquals(72.dp, HomeDiscoveryLayoutSpec.songCardHeight)
    }

    @Test
    fun cardText_shouldClampToSingleLineForStableHeight() {
        assertEquals(1, HomeDiscoveryLayoutSpec.titleMaxLines)
        assertEquals(1, HomeDiscoveryLayoutSpec.subtitleMaxLines)
    }

    @Test
    fun sectionTitle_shouldUseCompactSingleLineTypography() {
        assertEquals(22.sp, HomeDiscoveryLayoutSpec.sectionTitleFontSize)
        assertEquals(28.sp, HomeDiscoveryLayoutSpec.sectionTitleLineHeight)
        assertEquals(1, HomeDiscoveryLayoutSpec.sectionTitleMaxLines)
    }

    @Test
    fun bannerBadge_shouldUsePlainTextStyle() {
        assertTrue(HomeDiscoveryLayoutSpec.bannerBadgeUsesTextOnlyStyle)
    }

    @Test
    fun bannerCarousel_shouldExposeTheNextEditorialCard() {
        assertTrue(
            HomeDiscoveryLayoutSpec.bannerContentPadding.calculateRightPadding(LayoutDirection.Ltr) >
                HomeDiscoveryLayoutSpec.rowContentPadding.calculateRightPadding(LayoutDirection.Ltr)
        )
    }

    @Test
    fun bannerImage_shouldFillTheWholeCard() {
        assertTrue(HomeDiscoveryLayoutSpec.bannerImageFillsCard)
    }

    @Test
    fun bannerCarousel_shouldUseVirtualLoopingPages() {
        assertTrue(HomeDiscoveryLayoutSpec.bannerUsesInfiniteLoop)
        assertTrue(HomeDiscoveryLayoutSpec.virtualBannerPageCount > 1000)
    }

    @Test
    fun dailyShortcutCards_shouldUseCompactIconLeadingStyle() {
        assertTrue(HomeDiscoveryLayoutSpec.dailyShortcutUsesCompactIconStyle)
        assertEquals(104.dp, HomeDiscoveryLayoutSpec.compactCardHeight)
    }

    @Test
    fun dailyShortcutCards_shouldDeriveStableAccentBackgrounds() {
        val expected = HomeDiscoveryLayoutSpec.dailyShortcutBackgroundColor("每日推荐")
        val actual = HomeDiscoveryLayoutSpec.dailyShortcutBackgroundColor("每日推荐")

        assertEquals(expected, actual)
        assertTrue(actual != Color.Unspecified)
    }

    @Test
    fun discoveryCards_shouldUseFullBleedSquareArtwork() {
        assertTrue(HomeDiscoveryLayoutSpec.discoveryImageUsesFullBleed)
        assertEquals(1f, HomeDiscoveryLayoutSpec.discoveryImageAspectRatio)
    }

    @Test
    fun songPages_shouldUseThreeFullWidthRows() {
        assertEquals(3, HomeDiscoveryLayoutSpec.songColumnItemCount)
        assertEquals(2.dp, HomeDiscoveryLayoutSpec.songColumnItemSpacing)
    }

    @Test
    fun homepageCards_shouldUseTighterCornerHierarchy() {
        assertEquals(18.dp, HomeDiscoveryLayoutSpec.bannerCardCornerRadius)
        assertEquals(14.dp, HomeDiscoveryLayoutSpec.standardCardCornerRadius)
        assertEquals(18.dp, HomeDiscoveryLayoutSpec.compactCardCornerRadius)
    }

    @Test
    fun songCards_shouldReserveArtworkAndOverflowActionSpace() {
        assertEquals(48.dp, HomeDiscoveryLayoutSpec.songCardCoverSize)
        assertEquals(12.dp, HomeDiscoveryLayoutSpec.songCardCoverCornerRadius)
        assertEquals(36.dp, HomeDiscoveryLayoutSpec.songCardMenuButtonSize)
    }

    @Test
    fun songRows_shouldUseCompactVisibleDividers() {
        assertEquals(112.dp, HomeDiscoveryLayoutSpec.songDividerStartPadding)
        assertEquals(0.dp, HomeDiscoveryLayoutSpec.songDividerVerticalPadding)
        assertEquals(0.46f, HomeDiscoveryLayoutSpec.songDividerAlpha)
    }

    @Test
    fun homeSearchBox_shouldUseCalmerChrome() {
        assertEquals(18.dp, HomeDiscoveryLayoutSpec.searchBoxCornerRadius)
        assertEquals(0.dp, HomeDiscoveryLayoutSpec.searchBoxShadowElevation)
    }

    @Test
    fun bannerCarousel_shouldRecentreNearEdgesWhileKeepingSameLogicalItem() {
        val itemCount = 5
        val edgePage = HomeDiscoveryLayoutSpec.virtualBannerPageCount - 1
        val recenteredPage = HomeDiscoveryLayoutSpec.recenterBannerPage(
            currentPage = edgePage,
            itemCount = itemCount
        )

        assertEquals(edgePage % itemCount, recenteredPage % itemCount)
        assertTrue(recenteredPage < edgePage)
        assertTrue(recenteredPage >= HomeDiscoveryLayoutSpec.initialBannerPage(itemCount))
    }
}
