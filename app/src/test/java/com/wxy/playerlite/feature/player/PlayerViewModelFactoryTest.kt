package com.wxy.playerlite.feature.player

import androidx.lifecycle.ViewModelProvider
import com.wxy.playerlite.PlayerLiteApplication
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class PlayerViewModelFactoryTest {
    @Test
    fun applicationViewModelStore_shouldReturnSinglePlayerViewModel() {
        val application = RuntimeEnvironment.getApplication() as PlayerLiteApplication
        val provider = ViewModelProvider(
            owner = application,
            factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )

        val first = provider[PlayerViewModel::class.java]
        val second = ViewModelProvider(
            owner = application,
            factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[PlayerViewModel::class.java]

        assertSame(first, second)
        application.viewModelStore.clear()
    }
}
