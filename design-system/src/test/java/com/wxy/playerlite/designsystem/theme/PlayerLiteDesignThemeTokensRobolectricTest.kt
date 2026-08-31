package com.wxy.playerlite.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlayerLiteDesignThemeTokensRobolectricTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun lightTheme_shouldExposeSharedVisualTokens() {
        var colorScheme: ColorScheme? = null
        var tokens: PlayerLiteVisualTokens? = null
        var resolvedDarkTheme: Boolean? = null

        composeRule.setContent {
            PlayerLiteDesignTheme(darkTheme = false) {
                colorScheme = MaterialTheme.colorScheme
                tokens = PlayerLiteVisualTheme.colors
                resolvedDarkTheme = PlayerLiteResolvedTheme.darkTheme
            }
        }

        composeRule.runOnIdle {
            val currentScheme = requireNotNull(colorScheme)
            val current = requireNotNull(tokens)
            assertEquals(Color.White, currentScheme.surface)
            assertEquals(Color.White, currentScheme.onPrimary)
            assertEquals(Color(0xFFF9F9FB), current.canvas)
            assertEquals(Color.White, current.surfaceRaised)
            assertEquals(Color(0xFFE53935), current.accentStrong)
            assertEquals(Color(0xFF0087A0), current.accentSupport)
            assertEquals(
                Color(0xFF616161),
                current.miniPlayerProgressTrack
            )
            assertEquals(false, resolvedDarkTheme)
        }
    }

    @Test
    fun darkTheme_shouldPreserveDefaultPaletteRendering() {
        var colorScheme: ColorScheme? = null
        var tokens: PlayerLiteVisualTokens? = null
        var resolvedDarkTheme: Boolean? = null

        composeRule.setContent {
            PlayerLiteDesignTheme(darkTheme = true) {
                colorScheme = MaterialTheme.colorScheme
                tokens = PlayerLiteVisualTheme.colors
                resolvedDarkTheme = PlayerLiteResolvedTheme.darkTheme
            }
        }

        composeRule.runOnIdle {
            val currentScheme = requireNotNull(colorScheme)
            val current = requireNotNull(tokens)
            assertEquals(Color(0xFF191C1F), currentScheme.surface)
            assertEquals(Color(0xFF310909), currentScheme.onPrimary)
            assertEquals(Color(0xFF111315), current.canvas)
            assertEquals(Color(0xFF1D2024), current.surfaceRaised)
            assertEquals(Color(0xFFFF6E67), current.accentStrong)
            assertEquals(true, resolvedDarkTheme)
        }
    }

    @Test
    fun lightTheme_shouldUseEveryCustomPaletteRole() {
        assertCustomPaletteIsApplied(
            darkTheme = false,
            expectedPalette = CustomBrandPalettes.light
        )
    }

    @Test
    fun darkTheme_shouldUseEveryCustomPaletteRole() {
        assertCustomPaletteIsApplied(
            darkTheme = true,
            expectedPalette = CustomBrandPalettes.dark
        )
    }

    private fun assertCustomPaletteIsApplied(
        darkTheme: Boolean,
        expectedPalette: PlayerLiteBrandPalette
    ) {
        var colorScheme: ColorScheme? = null
        var tokens: PlayerLiteVisualTokens? = null

        composeRule.setContent {
            PlayerLiteDesignTheme(
                darkTheme = darkTheme,
                brandPalettes = CustomBrandPalettes
            ) {
                colorScheme = MaterialTheme.colorScheme
                tokens = PlayerLiteVisualTheme.colors
            }
        }

        composeRule.runOnIdle {
            val currentScheme = requireNotNull(colorScheme)
            val currentTokens = requireNotNull(tokens)
            val expectedSurfaceVariant = if (darkTheme) {
                expectedPalette.neutralStrong
            } else {
                expectedPalette.neutralVariant
            }
            val expectedHighlightAlpha = if (darkTheme) 0.14f else 0.08f
            val expectedDividerAlpha = if (darkTheme) 0.52f else 0.32f
            val expectedHandleAlpha = if (darkTheme) 0.48f else 0.24f

            assertEquals(expectedPalette.primary, currentScheme.primary)
            assertEquals(expectedPalette.onPrimary, currentScheme.onPrimary)
            assertEquals(expectedPalette.secondary, currentScheme.secondary)
            assertEquals(expectedPalette.onSecondary, currentScheme.onSecondary)
            assertEquals(expectedPalette.tertiary, currentScheme.tertiary)
            assertEquals(expectedPalette.onTertiary, currentScheme.onTertiary)
            assertEquals(expectedPalette.neutral, currentScheme.background)
            assertEquals(expectedPalette.onSurface, currentScheme.onBackground)
            assertEquals(expectedPalette.surface, currentScheme.surface)
            assertEquals(expectedPalette.onSurface, currentScheme.onSurface)
            assertEquals(expectedSurfaceVariant, currentScheme.surfaceVariant)
            assertEquals(expectedPalette.onSurfaceVariant, currentScheme.onSurfaceVariant)
            assertEquals(expectedPalette.outline, currentScheme.outline)
            assertEquals(expectedPalette.error, currentScheme.error)
            assertEquals(expectedPalette.onError, currentScheme.onError)

            assertEquals(expectedPalette.neutral, currentTokens.canvas)
            assertEquals(expectedPalette.surface, currentTokens.surfacePrimary)
            assertEquals(expectedSurfaceVariant, currentTokens.surfaceMuted)
            assertEquals(expectedPalette.surfaceRaised, currentTokens.surfaceRaised)
            assertEquals(
                expectedPalette.primary.copy(alpha = expectedHighlightAlpha),
                currentTokens.surfaceHighlight
            )
            assertEquals(expectedPalette.onSurfaceVariant, currentTokens.textSecondary)
            assertEquals(expectedPalette.secondary, currentTokens.textMuted)
            assertEquals(
                expectedPalette.outline.copy(alpha = expectedDividerAlpha),
                currentTokens.dividerSubtle
            )
            assertEquals(
                expectedPalette.onSurfaceVariant.copy(alpha = expectedHandleAlpha),
                currentTokens.handleMuted
            )
            assertEquals(expectedPalette.primary, currentTokens.accentStrong)
            assertEquals(expectedPalette.tertiary, currentTokens.accentSupport)
            assertEquals(expectedPalette.secondary, currentTokens.miniPlayerProgressTrack)
            assertEquals(expectedPalette.primary, currentTokens.miniPlayerProgressFill)
        }
    }

    private companion object {
        val CustomBrandPalettes = PlayerLiteBrandPalettes(
            light = customPalette(colorOffset = 0x00),
            dark = customPalette(colorOffset = 0x20)
        )

        fun customPalette(colorOffset: Int): PlayerLiteBrandPalette {
            fun color(step: Int): Color {
                val channel = colorOffset + step
                return Color(
                    red = channel,
                    green = channel,
                    blue = channel,
                    alpha = 0xFF
                )
            }

            return PlayerLiteBrandPalette(
                primary = color(1),
                secondary = color(2),
                tertiary = color(3),
                neutral = color(4),
                neutralVariant = color(5),
                neutralStrong = color(6),
                surface = color(7),
                surfaceRaised = color(8),
                onPrimary = color(9),
                onSecondary = color(10),
                onTertiary = color(11),
                onSurface = color(12),
                onSurfaceVariant = color(13),
                outline = color(14),
                error = color(15),
                onError = color(16)
            )
        }
    }
}
