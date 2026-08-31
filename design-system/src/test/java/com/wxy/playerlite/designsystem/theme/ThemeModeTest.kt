package com.wxy.playerlite.designsystem.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeModeTest {
    @Test
    fun wireValues_shouldRemainStable() {
        assertEquals("system", ThemeMode.SYSTEM.wireValue)
        assertEquals("light", ThemeMode.LIGHT.wireValue)
        assertEquals("dark", ThemeMode.DARK.wireValue)
    }

    @Test
    fun resolveDarkTheme_shouldRespectModeAndSystemValue() {
        assertFalse(ThemeMode.SYSTEM.resolveDarkTheme(systemDark = false))
        assertTrue(ThemeMode.SYSTEM.resolveDarkTheme(systemDark = true))
        assertFalse(ThemeMode.LIGHT.resolveDarkTheme(systemDark = false))
        assertFalse(ThemeMode.LIGHT.resolveDarkTheme(systemDark = true))
        assertTrue(ThemeMode.DARK.resolveDarkTheme(systemDark = false))
        assertTrue(ThemeMode.DARK.resolveDarkTheme(systemDark = true))
    }

    @Test
    fun fromWireValue_shouldFallBackToSystemForMissingOrUnknownValues() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromWireValue(null))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromWireValue(""))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromWireValue("future-mode"))
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromWireValue("light"))
        assertEquals(ThemeMode.DARK, ThemeMode.fromWireValue("dark"))
    }
}
