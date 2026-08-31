package com.wxy.playerlite.core

import androidx.test.core.app.ApplicationProvider
import com.wxy.playerlite.PlayerLiteApplication
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppContainerThemePreferencesTest {
    @Test
    fun appContainerAndSearchHost_shouldShareOneThemeSelectionFlow() {
        val application = ApplicationProvider.getApplicationContext<PlayerLiteApplication>()
        val firstRepository = AppContainer.themePreferencesRepository(application)
        val secondRepository = AppContainer.themePreferencesRepository(application)
        val searchDependencies = application.searchHostDependencies()

        assertSame(firstRepository, secondRepository)
        assertSame(firstRepository.selectionFlow, searchDependencies.themeSelectionFlow)
    }
}
