package com.wxy.playerlite.feature.detail

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class DetailStatusBarStyleTest {
    @Test
    fun detailHeroContentColor_shouldUseWhiteForDarkBackdrop() {
        assertEquals(
            Color.White,
            detailHeroContentColor(Color(0xFF202124))
        )
    }

    @Test
    fun detailHeroContentColor_shouldUseBlackForLightBackdrop() {
        assertEquals(
            Color.Black,
            detailHeroContentColor(Color(0xFFF3F4F7))
        )
    }
}
