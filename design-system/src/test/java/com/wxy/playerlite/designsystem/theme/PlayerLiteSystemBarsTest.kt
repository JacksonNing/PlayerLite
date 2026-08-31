package com.wxy.playerlite.designsystem.theme

import android.app.Activity
import androidx.core.view.WindowCompat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlayerLiteSystemBarsTest {
    @Test
    fun resolvedTheme_shouldMapToOppositeSystemBarIconBrightness() {
        assertTrue(shouldUseDarkSystemBarIcons(resolvedDarkTheme = false))
        assertFalse(shouldUseDarkSystemBarIcons(resolvedDarkTheme = true))
    }

    @Test
    fun applyResolvedTheme_shouldUpdateStatusAndNavigationBarsTogether() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val controller = WindowCompat.getInsetsController(
            activity.window,
            activity.window.decorView
        )

        applyPlayerLiteResolvedThemeSystemBars(
            window = activity.window,
            resolvedDarkTheme = false
        )
        assertTrue(controller.isAppearanceLightStatusBars)
        assertTrue(controller.isAppearanceLightNavigationBars)

        applyPlayerLiteResolvedThemeSystemBars(
            window = activity.window,
            resolvedDarkTheme = true
        )
        assertFalse(controller.isAppearanceLightStatusBars)
        assertFalse(controller.isAppearanceLightNavigationBars)
    }
}
