package com.wxy.playerlite.core.theme

import android.content.Context
import android.content.SharedPreferences
import com.wxy.playerlite.designsystem.theme.SkinId
import com.wxy.playerlite.designsystem.theme.ThemeMode
import com.wxy.playerlite.designsystem.theme.ThemeSelection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal interface ThemePreferencesRepository {
    val currentSelection: ThemeSelection
    val selectionFlow: StateFlow<ThemeSelection>

    fun setThemeMode(mode: ThemeMode)
}

internal class SharedPreferencesThemePreferencesRepository(
    private val preferences: SharedPreferences
) : ThemePreferencesRepository {
    constructor(context: Context) : this(
        preferences = context.applicationContext.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )
    )

    private val mutableSelectionFlow = MutableStateFlow(readSelection())

    override val currentSelection: ThemeSelection
        get() = mutableSelectionFlow.value

    override val selectionFlow: StateFlow<ThemeSelection> = mutableSelectionFlow.asStateFlow()

    override fun setThemeMode(mode: ThemeMode) {
        val current = mutableSelectionFlow.value
        if (current.mode == mode) {
            return
        }
        preferences.edit()
            .putString(KEY_THEME_MODE, mode.wireValue)
            .apply()
        mutableSelectionFlow.value = current.copy(mode = mode)
    }

    private fun readSelection(): ThemeSelection {
        return ThemeSelection(
            mode = ThemeMode.fromWireValue(preferences.getString(KEY_THEME_MODE, null)),
            skinId = SkinId.Default
        )
    }

    companion object {
        const val PREFERENCES_NAME = "theme_preferences"
        const val KEY_THEME_MODE = "theme_mode"
    }
}
