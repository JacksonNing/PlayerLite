package com.wxy.playerlite.core.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.wxy.playerlite.designsystem.theme.SkinId
import com.wxy.playerlite.designsystem.theme.ThemeMode
import com.wxy.playerlite.designsystem.theme.ThemeSelection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ThemePreferencesRepositoryTest {
    private lateinit var preferences: SharedPreferences

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        preferences = context.getSharedPreferences(
            "theme_preferences_repository_test",
            Context.MODE_PRIVATE
        )
        preferences.edit().clear().commit()
    }

    @Test
    fun missingValue_shouldDefaultToSystemAndDefaultSkin() {
        val repository = SharedPreferencesThemePreferencesRepository(preferences)

        assertEquals(
            ThemeSelection(mode = ThemeMode.SYSTEM, skinId = SkinId.Default),
            repository.currentSelection
        )
        assertEquals(repository.currentSelection, repository.selectionFlow.value)
    }

    @Test
    fun setThemeMode_shouldRoundTripEveryStableWireValue() {
        val repository = SharedPreferencesThemePreferencesRepository(preferences)

        listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.SYSTEM).forEach { mode ->
            repository.setThemeMode(mode)

            assertEquals(mode, repository.currentSelection.mode)
            assertEquals(mode, repository.selectionFlow.value.mode)
            assertEquals(
                mode.wireValue,
                preferences.getString(
                    SharedPreferencesThemePreferencesRepository.KEY_THEME_MODE,
                    null
                )
            )
        }
    }

    @Test
    fun invalidPersistedValue_shouldFallBackToSystem() {
        preferences.edit()
            .putString(SharedPreferencesThemePreferencesRepository.KEY_THEME_MODE, "future-mode")
            .commit()

        val repository = SharedPreferencesThemePreferencesRepository(preferences)

        assertEquals(ThemeMode.SYSTEM, repository.currentSelection.mode)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun settingSameMode_shouldNotEmitDuplicateSelection() = runTest {
        val repository = SharedPreferencesThemePreferencesRepository(preferences)
        val observed = mutableListOf<ThemeSelection>()
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.selectionFlow.collect(observed::add)
        }

        repository.setThemeMode(ThemeMode.DARK)
        repository.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()

        assertEquals(
            listOf(
                ThemeSelection(),
                ThemeSelection(mode = ThemeMode.DARK)
            ),
            observed
        )
        collectJob.cancel()
    }

    @Test
    fun newInstance_shouldRestoreSavedSelection() {
        SharedPreferencesThemePreferencesRepository(preferences)
            .setThemeMode(ThemeMode.DARK)

        val restored = SharedPreferencesThemePreferencesRepository(preferences)

        assertEquals(ThemeSelection(mode = ThemeMode.DARK), restored.currentSelection)
    }
}
