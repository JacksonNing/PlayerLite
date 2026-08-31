package com.wxy.playerlite.designsystem.theme

import androidx.compose.runtime.Immutable

enum class ThemeMode(val wireValue: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    fun resolveDarkTheme(systemDark: Boolean): Boolean {
        return when (this) {
            SYSTEM -> systemDark
            LIGHT -> false
            DARK -> true
        }
    }

    companion object {
        fun fromWireValue(value: String?): ThemeMode {
            return entries.firstOrNull { it.wireValue == value } ?: SYSTEM
        }
    }
}

@JvmInline
value class SkinId(val value: String) {
    companion object {
        val Default = SkinId("default")
    }
}

@Immutable
data class ThemeSelection(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val skinId: SkinId = SkinId.Default
)

@Immutable
data class PlayerLiteSkin(
    val id: SkinId,
    val palettes: PlayerLiteBrandPalettes
)

object PlayerLiteSkinCatalog {
    private val defaultSkin = PlayerLiteSkin(
        id = SkinId.Default,
        palettes = PlayerLiteThemeContract.DefaultBrandPalettes
    )

    private val skins = mapOf(defaultSkin.id to defaultSkin)

    fun resolve(skinId: SkinId): PlayerLiteSkin {
        return skins[skinId] ?: defaultSkin
    }
}
