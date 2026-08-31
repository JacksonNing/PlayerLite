package com.wxy.playerlite.ui.theme

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.WindowCompat
import com.wxy.playerlite.core.AppContainer
import com.wxy.playerlite.designsystem.theme.PlayerLiteContentSystemBarsEffect
import com.wxy.playerlite.designsystem.theme.PlayerLiteSystemBarsEffect
import com.wxy.playerlite.designsystem.theme.ThemeMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController

@RunWith(RobolectricTestRunner::class)
class PlayerLiteSystemBarsRobolectricTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun contentOverrideLeavingComposition_shouldRestoreResolvedThemeOnBothBars() {
        var showContentOverride by mutableStateOf(true)

        composeRule.setContent {
            PlayerLiteTheme(darkTheme = false) {
                PlayerLiteSystemBarsEffect()
                if (showContentOverride) {
                    PlayerLiteContentSystemBarsEffect(useLightContent = true)
                }
            }
        }

        composeRule.runOnIdle {
            assertSystemBarIconAppearance(useDarkIcons = false)
            showContentOverride = false
        }
        composeRule.runOnIdle {
            assertSystemBarIconAppearance(useDarkIcons = true)
        }
    }

    @Test
    fun initialSystemBars_shouldReadCurrentRepositorySnapshot() {
        val repository = AppContainer.themePreferencesRepository(composeRule.activity)
        val originalMode = repository.currentSelection.mode

        try {
            repository.setThemeMode(ThemeMode.DARK)
            composeRule.activity.applyInitialPlayerLiteSystemBars()
            assertSystemBarIconAppearance(useDarkIcons = false)

            repository.setThemeMode(ThemeMode.LIGHT)
            composeRule.activity.applyInitialPlayerLiteSystemBars()
            assertSystemBarIconAppearance(useDarkIcons = true)
        } finally {
            repository.setThemeMode(originalMode)
        }
    }

    @Test
    fun recreatedAndNewActivities_shouldUseLatestStoredSelection() {
        val repository = AppContainer.themePreferencesRepository(composeRule.activity)
        val originalMode = repository.currentSelection.mode
        var firstController: ActivityController<ThemeLifecycleTestActivity>? = null
        var secondController: ActivityController<ThemeLifecycleTestActivity>? = null

        try {
            repository.setThemeMode(ThemeMode.DARK)
            firstController = Robolectric.buildActivity(ThemeLifecycleTestActivity::class.java)
                .setup()
            assertSystemBarIconAppearance(
                activity = requireNotNull(firstController).get(),
                useDarkIcons = false
            )

            repository.setThemeMode(ThemeMode.LIGHT)
            firstController = requireNotNull(firstController).recreate()
            assertSystemBarIconAppearance(
                activity = requireNotNull(firstController).get(),
                useDarkIcons = true
            )

            repository.setThemeMode(ThemeMode.DARK)
            secondController = Robolectric.buildActivity(ThemeLifecycleTestActivity::class.java)
                .setup()
            assertSystemBarIconAppearance(
                activity = requireNotNull(secondController).get(),
                useDarkIcons = false
            )
        } finally {
            firstController?.pause()?.stop()?.destroy()
            secondController?.pause()?.stop()?.destroy()
            repository.setThemeMode(originalMode)
        }
    }

    private fun assertSystemBarIconAppearance(useDarkIcons: Boolean) {
        assertSystemBarIconAppearance(
            activity = composeRule.activity,
            useDarkIcons = useDarkIcons
        )
    }

    private fun assertSystemBarIconAppearance(
        activity: ComponentActivity,
        useDarkIcons: Boolean
    ) {
        val controller = WindowCompat.getInsetsController(
            activity.window,
            activity.window.decorView
        )
        if (useDarkIcons) {
            assertTrue(controller.isAppearanceLightStatusBars)
            assertTrue(controller.isAppearanceLightNavigationBars)
        } else {
            assertFalse(controller.isAppearanceLightStatusBars)
            assertFalse(controller.isAppearanceLightNavigationBars)
        }
    }
}

class ThemeLifecycleTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        applyInitialPlayerLiteSystemBars()
    }
}
