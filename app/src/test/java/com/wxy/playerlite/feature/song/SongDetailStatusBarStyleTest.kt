package com.wxy.playerlite.feature.song

import com.wxy.playerlite.designsystem.theme.PlayerLiteThemeContract
import com.wxy.playerlite.feature.detail.shouldUseLightStatusBarContent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SongDetailStatusBarStyleTest {
    @Test
    fun inverseSurface_shouldKeepSongDetailStatusBarContentReadable() {
        val lightTopColor = PlayerLiteThemeContract.colorScheme(
            darkTheme = false
        ).inverseSurface
        val darkTopColor = PlayerLiteThemeContract.colorScheme(
            darkTheme = true
        ).inverseSurface

        assertTrue(shouldUseLightStatusBarContent(lightTopColor))
        assertFalse(shouldUseLightStatusBarContent(darkTopColor))
    }
}
