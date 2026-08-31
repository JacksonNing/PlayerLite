package com.wxy.playerlite.designsystem.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class PlayerLiteSkinCatalogTest {
    @Test
    fun defaultSkin_shouldResolveWithPairedPalettes() {
        val skin = PlayerLiteSkinCatalog.resolve(SkinId.Default)

        assertEquals(SkinId.Default, skin.id)
        assertSame(PlayerLiteThemeContract.DefaultBrandPalettes, skin.palettes)
    }

    @Test
    fun unknownSkin_shouldFallBackToDefaultSkin() {
        val skin = PlayerLiteSkinCatalog.resolve(SkinId("future-skin"))

        assertEquals(SkinId.Default, skin.id)
        assertSame(PlayerLiteThemeContract.DefaultBrandPalettes, skin.palettes)
    }
}
