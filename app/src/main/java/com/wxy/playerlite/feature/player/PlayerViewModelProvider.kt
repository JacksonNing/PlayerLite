package com.wxy.playerlite.feature.player

import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import com.wxy.playerlite.PlayerLiteApplication

internal fun ComponentActivity.sharedPlayerViewModels(): Lazy<PlayerViewModel> {
    return lazy(LazyThreadSafetyMode.NONE) {
        val owner = application as? PlayerLiteApplication
            ?: error("PlayerLiteApplication must own the shared PlayerViewModel")
        ViewModelProvider(
            owner = owner,
            factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[PlayerViewModel::class.java]
    }
}
