package com.wxy.playerlite.feature.user

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import com.wxy.playerlite.designsystem.theme.PlayerLiteThemeContract
import com.wxy.playerlite.ui.theme.PlayerLiteTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AccountVisualThemeRobolectricTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun accountVisualTheme_shouldDeriveFromCurrentSkinPalette() {
        val customPalettes = PlayerLiteThemeContract.DefaultBrandPalettes.copy(
            light = PlayerLiteThemeContract.DefaultBrandPalettes.light.copy(
                primary = Color(0xFF123456)
            )
        )
        var accent: Color? = null
        var accentSoft: Color? = null
        var accentText: Color? = null

        composeRule.setContent {
            PlayerLiteTheme(
                darkTheme = false,
                brandPalettes = customPalettes
            ) {
                accent = AccountVisualTheme.accent
                accentSoft = AccountVisualTheme.accentSoft
                accentText = AccountVisualTheme.accentText
            }
        }

        composeRule.runOnIdle {
            assertEquals(Color(0xFF123456), accent)
            assertEquals(Color(0xFF123456).copy(alpha = 0.08f), accentSoft)
            assertEquals(Color(0xFF123456), accentText)
        }
    }
}
