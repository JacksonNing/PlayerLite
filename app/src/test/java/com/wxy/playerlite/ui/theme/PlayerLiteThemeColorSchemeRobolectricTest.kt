package com.wxy.playerlite.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import com.wxy.playerlite.designsystem.theme.PlayerLiteResolvedTheme
import com.wxy.playerlite.designsystem.theme.ThemeMode
import com.wxy.playerlite.designsystem.theme.ThemeSelection
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlayerLiteThemeColorSchemeRobolectricTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun lightTheme_shouldExposeNewCorePalette() {
        var colorScheme: ColorScheme? = null

        composeRule.setContent {
            PlayerLiteTheme(darkTheme = false) {
                colorScheme = MaterialTheme.colorScheme
            }
        }

        composeRule.runOnIdle {
            val scheme = requireNotNull(colorScheme)
            assertEquals(Color(0xFFE53935), scheme.primary)
            assertEquals(Color(0xFF616161), scheme.secondary)
            assertEquals(Color(0xFF0087A0), scheme.tertiary)
            assertEquals(Color(0xFFF9F9FB), scheme.background)
        }
    }

    @Test
    fun appTheme_darkSelection_shouldOverrideLightSystemTheme() {
        val selectionFlow = MutableStateFlow(
            ThemeSelection(mode = ThemeMode.DARK)
        )
        var colorScheme: ColorScheme? = null

        composeRule.setContent {
            PlayerLiteAppTheme(
                themeSelectionFlow = selectionFlow,
                systemDarkTheme = false
            ) {
                colorScheme = MaterialTheme.colorScheme
            }
        }

        composeRule.runOnIdle {
            assertEquals(Color(0xFF111315), requireNotNull(colorScheme).background)
        }
    }

    @Test
    fun appTheme_systemSelection_shouldFollowSystemChanges() {
        val selectionFlow = MutableStateFlow(
            ThemeSelection(mode = ThemeMode.SYSTEM)
        )
        var systemDarkTheme by mutableStateOf(false)
        var resolvedDarkTheme: Boolean? = null

        composeRule.setContent {
            PlayerLiteAppTheme(
                themeSelectionFlow = selectionFlow,
                systemDarkTheme = systemDarkTheme
            ) {
                resolvedDarkTheme = PlayerLiteResolvedTheme.darkTheme
            }
        }

        composeRule.runOnIdle {
            assertEquals(false, resolvedDarkTheme)
            systemDarkTheme = true
        }
        composeRule.runOnIdle {
            assertEquals(true, resolvedDarkTheme)
        }
    }

    @Test
    fun appTheme_explicitSelection_shouldIgnoreSystemChanges() {
        val selectionFlow = MutableStateFlow(
            ThemeSelection(mode = ThemeMode.LIGHT)
        )
        var systemDarkTheme by mutableStateOf(false)
        var resolvedDarkTheme: Boolean? = null

        composeRule.setContent {
            PlayerLiteAppTheme(
                themeSelectionFlow = selectionFlow,
                systemDarkTheme = systemDarkTheme
            ) {
                resolvedDarkTheme = PlayerLiteResolvedTheme.darkTheme
            }
        }

        composeRule.runOnIdle {
            assertEquals(false, resolvedDarkTheme)
            systemDarkTheme = true
        }
        composeRule.runOnIdle {
            assertEquals(false, resolvedDarkTheme)
        }
    }

    @Test
    fun appTheme_selectionFlowUpdate_shouldRecomposeWithoutActivityRecreation() {
        val selectionFlow = MutableStateFlow(
            ThemeSelection(mode = ThemeMode.LIGHT)
        )
        var colorScheme: ColorScheme? = null

        composeRule.setContent {
            PlayerLiteAppTheme(
                themeSelectionFlow = selectionFlow,
                systemDarkTheme = true
            ) {
                colorScheme = MaterialTheme.colorScheme
            }
        }

        composeRule.runOnIdle {
            assertEquals(Color(0xFFF9F9FB), requireNotNull(colorScheme).background)
            selectionFlow.value = ThemeSelection(mode = ThemeMode.DARK)
        }
        composeRule.runOnIdle {
            assertEquals(Color(0xFF111315), requireNotNull(colorScheme).background)
        }
    }
}
