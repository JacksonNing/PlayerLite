package com.wxy.playerlite

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.wxy.playerlite.core.AppContainer
import com.wxy.playerlite.feature.home.HomeHostDependencies
import com.wxy.playerlite.feature.home.HomeHostDependenciesProvider
import com.wxy.playerlite.feature.search.SearchHostDependencies
import com.wxy.playerlite.feature.search.SearchHostDependenciesProvider
import com.wxy.playerlite.feature.search.SearchSongPlaybackHandler
import com.wxy.playerlite.feature.search.SearchRouteHandler
import com.wxy.playerlite.feature.search.playSearchSongs
import com.wxy.playerlite.feature.search.searchRouteIntent

class PlayerLiteApplication : Application(),
    ViewModelStoreOwner,
    SearchHostDependenciesProvider,
    HomeHostDependenciesProvider {
    override val viewModelStore = ViewModelStore()

    override fun homeHostDependencies(): HomeHostDependencies {
        return AppContainer.homeHostDependencies(this)
    }

    override fun searchHostDependencies(): SearchHostDependencies {
        return SearchHostDependencies(
            repository = AppContainer.searchRepository(this),
            themeSelectionFlow = AppContainer.themePreferencesRepository(this).selectionFlow,
            routeHandler = SearchRouteHandler { context, target ->
                searchRouteIntent(context, target)?.let(context::startActivity)
            },
            songPlaybackHandler = SearchSongPlaybackHandler { context, songs, activeSongId ->
                playSearchSongs(
                    context = context,
                    songs = songs,
                    activeSongId = activeSongId
                )
            }
        )
    }

    override fun onTerminate() {
        viewModelStore.clear()
        super.onTerminate()
    }
}
