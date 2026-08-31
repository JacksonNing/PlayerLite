package com.wxy.playerlite.ui.theme

import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wxy.playerlite.core.AppContainer
import com.wxy.playerlite.designsystem.theme.PlayerLiteBrandPalettes
import com.wxy.playerlite.designsystem.theme.PlayerLiteDesignTheme
import com.wxy.playerlite.designsystem.theme.PlayerLiteSkinCatalog
import com.wxy.playerlite.designsystem.theme.PlayerLiteSystemBarsEffect
import com.wxy.playerlite.designsystem.theme.PlayerLiteThemeContract
import com.wxy.playerlite.designsystem.theme.ThemeSelection
import com.wxy.playerlite.designsystem.theme.applyPlayerLiteResolvedThemeSystemBars
import kotlinx.coroutines.flow.StateFlow

@Composable
fun PlayerLiteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    brandPalettes: PlayerLiteBrandPalettes = PlayerLiteThemeContract.DefaultBrandPalettes,
    content: @Composable () -> Unit
) {
    PlayerLiteDesignTheme(
        darkTheme = darkTheme,
        brandPalettes = brandPalettes,
        content = content
    )
}

@Composable
internal fun PlayerLiteAppTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    PlayerLiteAppTheme(
        themeSelectionFlow = AppContainer.themePreferencesRepository(context).selectionFlow,
        content = content
    )
}

@Composable
internal fun PlayerLiteAppTheme(
    themeSelectionFlow: StateFlow<ThemeSelection>,
    systemDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val selection = themeSelectionFlow.collectAsStateWithLifecycle().value
    val resolvedDarkTheme = selection.mode.resolveDarkTheme(systemDarkTheme)
    val skin = PlayerLiteSkinCatalog.resolve(selection.skinId)

    PlayerLiteTheme(
        darkTheme = resolvedDarkTheme,
        brandPalettes = skin.palettes
    ) {
        PlayerLiteSystemBarsEffect(resolvedDarkTheme = resolvedDarkTheme)
        content()
    }
}

internal fun ComponentActivity.applyInitialPlayerLiteSystemBars() {
    val selection = AppContainer.themePreferencesRepository(this).currentSelection
    val systemDarkTheme = resources.configuration.isDarkTheme()
    applyPlayerLiteResolvedThemeSystemBars(
        window = window,
        resolvedDarkTheme = selection.mode.resolveDarkTheme(systemDarkTheme)
    )
}

private fun Configuration.isDarkTheme(): Boolean {
    return uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
}
