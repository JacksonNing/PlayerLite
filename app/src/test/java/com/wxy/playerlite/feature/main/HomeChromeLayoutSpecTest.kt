package com.wxy.playerlite.feature.main

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeChromeLayoutSpecTest {
    @Test
    fun miniPlayerAndBottomBar_shouldUseMatchingCornerRhythm() {
        assertEquals(14.dp, HomeChromeLayoutSpec.bottomBarCornerRadius)
        assertEquals(14.dp, HomeChromeLayoutSpec.miniPlayerCornerRadius)
        assertEquals(
            HomeChromeLayoutSpec.miniPlayerCornerRadius,
            HomeChromeLayoutSpec.bottomBarCornerRadius
        )
    }

    @Test
    fun bottomBar_shouldMatchMiniPlayerHeight() {
        assertEquals(60.dp, HomeChromeLayoutSpec.bottomBarMinHeight)
        assertEquals(
            HomeChromeLayoutSpec.miniPlayerMinHeight,
            HomeChromeLayoutSpec.bottomBarMinHeight
        )
    }

    @Test
    fun bottomBar_shouldRemainACompactFloatingBar() {
        assertEquals(0.70f, HomeChromeLayoutSpec.bottomBarWidthFraction)
        assertEquals(276.dp, HomeChromeLayoutSpec.bottomBarMaxWidth)
        assertEquals(26.dp, HomeChromeLayoutSpec.bottomBarOuterHorizontalPadding)
    }

    @Test
    fun bottomBar_shouldUseTheSameFloatingShadowAsMiniPlayer() {
        assertEquals(
            HomeChromeLayoutSpec.miniPlayerShadowElevation,
            HomeChromeLayoutSpec.bottomBarShadowElevation
        )
    }

    @Test
    fun bottomBar_shouldReserveIndependentBottomClearanceForGestureHandle() {
        assertEquals(4.dp, HomeChromeLayoutSpec.bottomBarBottomClearance)
    }

    @Test
    fun miniPlayerProgressTrack_shouldStaySubtleAndTranslucent() {
        assertTrue(
            "Expected progress track background alpha to stay readable without becoming heavy, but was ${HomeChromeLayoutSpec.miniPlayerProgressTrackAlpha}",
            HomeChromeLayoutSpec.miniPlayerProgressTrackAlpha in 0.09f..0.12f
        )
    }

    @Test
    fun miniPlayerProgressTrack_shouldStaySlimButReadable() {
        assertEquals(4.dp, HomeChromeLayoutSpec.miniPlayerProgressTrackHeight)
        assertEquals(0.dp, HomeChromeLayoutSpec.miniPlayerProgressTrackOverlap)
    }

    @Test
    fun overviewScrollContent_shouldReserveExtraBottomSpaceForMiniPlayerAndTabBar() {
        assertEquals(
            HomeChromeLayoutSpec.bottomBarOverlayHeight + 4.dp,
            HomeChromeLayoutSpec.homeMiniPlayerBottomSpacing
        )
        assertTrue(
            HomeChromeLayoutSpec.homeOverviewScrollBottomPadding >
                HomeChromeLayoutSpec.homeMiniPlayerBottomSpacing
        )
    }

    @Test
    fun userCenterScrollContent_shouldReserveBottomSpaceForFloatingTabBar() {
        assertTrue(
            HomeChromeLayoutSpec.userCenterScrollBottomPadding >
                HomeChromeLayoutSpec.bottomBarMinHeight
        )
    }
}
