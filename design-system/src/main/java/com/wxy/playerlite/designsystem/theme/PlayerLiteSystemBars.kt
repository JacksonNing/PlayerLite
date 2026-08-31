package com.wxy.playerlite.designsystem.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

fun shouldUseDarkSystemBarIcons(resolvedDarkTheme: Boolean): Boolean {
    return !resolvedDarkTheme
}

fun applyPlayerLiteResolvedThemeSystemBars(
    window: Window,
    resolvedDarkTheme: Boolean
) {
    applyPlayerLiteSystemBarIconAppearance(
        window = window,
        useDarkIcons = shouldUseDarkSystemBarIcons(resolvedDarkTheme)
    )
}

fun applyPlayerLiteSystemBarIconAppearance(
    window: Window,
    useDarkIcons: Boolean
) {
    WindowCompat.getInsetsController(window, window.decorView).apply {
        isAppearanceLightStatusBars = useDarkIcons
        isAppearanceLightNavigationBars = useDarkIcons
    }
}

@Composable
fun PlayerLiteSystemBarsEffect(
    resolvedDarkTheme: Boolean = PlayerLiteResolvedTheme.darkTheme
) {
    val window = LocalView.current.context.findActivity()?.window
    SideEffect {
        window?.let {
            applyPlayerLiteResolvedThemeSystemBars(
                window = it,
                resolvedDarkTheme = resolvedDarkTheme
            )
        }
    }
}

@Composable
fun PlayerLiteContentSystemBarsEffect(
    useLightContent: Boolean
) {
    val window = LocalView.current.context.findActivity()?.window
    val resolvedDarkTheme = PlayerLiteResolvedTheme.darkTheme

    SideEffect {
        window?.let {
            applyPlayerLiteSystemBarIconAppearance(
                window = it,
                useDarkIcons = !useLightContent
            )
        }
    }
    DisposableEffect(window, resolvedDarkTheme) {
        onDispose {
            window?.let {
                applyPlayerLiteResolvedThemeSystemBars(
                    window = it,
                    resolvedDarkTheme = resolvedDarkTheme
                )
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
