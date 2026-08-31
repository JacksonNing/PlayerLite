package com.wxy.playerlite.feature.search

import android.content.Context
import com.wxy.playerlite.designsystem.theme.ThemeSelection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface SearchHostDependenciesProvider {
    fun searchHostDependencies(): SearchHostDependencies
}

data class SearchHostDependencies(
    val repository: SearchRepository,
    val routeHandler: SearchRouteHandler = SearchRouteHandler { _, _ -> Unit },
    val songPlaybackHandler: SearchSongPlaybackHandler = SearchSongPlaybackHandler { _, _, _ ->
        false
    },
    val themeSelectionFlow: StateFlow<ThemeSelection> = MutableStateFlow(ThemeSelection())
)

fun interface SearchRouteHandler {
    fun open(context: Context, target: SearchRouteTarget)
}

fun interface SearchSongPlaybackHandler {
    fun play(
        context: Context,
        songs: List<SearchResultUiModel.Song>,
        activeSongId: String
    ): Boolean
}

internal fun Context.requireSearchHostDependencies(): SearchHostDependencies {
    val provider = applicationContext as? SearchHostDependenciesProvider
        ?: error("Application must implement SearchHostDependenciesProvider")
    return provider.searchHostDependencies()
}
