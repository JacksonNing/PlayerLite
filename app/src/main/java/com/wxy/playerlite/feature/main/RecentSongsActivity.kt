package com.wxy.playerlite.feature.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.wxy.playerlite.core.playback.AppPlaybackGraph
import com.wxy.playerlite.designsystem.theme.PlayerLiteVisualTheme
import com.wxy.playerlite.feature.album.AlbumDetailActivity
import com.wxy.playerlite.feature.artist.ArtistDetailActivity
import com.wxy.playerlite.feature.player.PlayerActivity
import com.wxy.playerlite.feature.player.runtime.DetailPlaybackRequest
import com.wxy.playerlite.feature.search.SearchRouteTarget
import com.wxy.playerlite.feature.song.SongDetailActivity
import com.wxy.playerlite.feature.user.LoginActivity
import com.wxy.playerlite.ui.theme.PlayerLiteTheme

class RecentSongsActivity : ComponentActivity() {
    private val viewModel: RecentSongsViewModel by viewModels()
    private val songDetailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && SongDetailActivity.wasRemovedFromRecent(result.data)) {
            viewModel.retry()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PlayerLiteTheme {
                val state = viewModel.uiStateFlow.collectAsStateWithLifecycle().value
                BackHandler(onBack = ::finish)
                RecentSongsScreen(
                    state = state,
                    onBack = ::finish,
                    onLoginClick = {
                        startActivity(LoginActivity.createIntent(this@RecentSongsActivity))
                    },
                    onRetry = viewModel::retry,
                    onSelectTab = viewModel::selectTab,
                    onItemClick = { item ->
                        val songs = (state.contentState as? RecentPlaybackContentState.SongContent)
                            ?.items
                            .orEmpty()
                        playRecentSongs(items = songs, target = item)
                    },
                    onItemInsertNext = ::insertSongNext,
                    onItemOpenDetail = ::openSongDetail,
                    onItemOpenArtist = ::openArtistDetail,
                    onItemOpenAlbum = ::openAlbumDetail,
                    onLocalItemClick = { item ->
                        val localItems = (state.contentState as? RecentPlaybackContentState.LocalContent)
                            ?.items
                            .orEmpty()
                        playLocalRecent(localItems = localItems, target = item)
                    },
                    onLocalItemInsertNext = ::insertLocalNext,
                    onLocalItemOpenDetail = ::openLocalDetail,
                    onLocalItemOpenArtist = ::openArtistDetail,
                    onLocalItemOpenAlbum = ::openAlbumDetail
                )
            }
        }
    }

    private fun insertSongNext(item: RecentSongItemUiModel) {
        val inserted = AppPlaybackGraph.runtime(this)
            .insertPlaylistItemNext(item.toPlaylistItem(queueIndex = 0))
        showMessage(
            if (inserted) {
                "已加入下一首播放"
            } else {
                "当前没有可插入的播放上下文"
            }
        )
    }

    private fun openSongDetail(item: RecentSongItemUiModel) {
        val songId = (item.detailAction as? ContentEntryAction.OpenDetail)
            ?.target
            ?.let { it as? SearchRouteTarget.Song }
            ?.songId
            ?.takeIf { it.isNotBlank() }
            ?: run {
                showMessage("当前歌曲详情暂时无法打开")
                return
            }
        songDetailLauncher.launch(
            SongDetailActivity.createOnlineIntent(
                context = this,
                songId = songId
            )
        )
    }

    private fun playRecentSongs(
        items: List<RecentSongItemUiModel>,
        target: RecentSongItemUiModel
    ) {
        val activeIndex = items.indexOfFirst { it.id == target.id }
        if (activeIndex < 0) {
            showMessage("当前歌曲暂时无法播放")
            return
        }
        val started = AppPlaybackGraph.detailPlaybackGateway(this)
            .play(
                DetailPlaybackRequest(
                    items = items.mapIndexed { index, item -> item.toPlaylistItem(index) },
                    activeIndex = activeIndex
                )
            )
        if (!started) {
            showMessage("播放启动失败，请稍后重试")
            return
        }
        startActivity(createRecentPlaybackPlayerIntent(this))
    }

    private fun openArtistDetail(artistId: String) {
        startActivity(ArtistDetailActivity.createIntent(this, artistId))
    }

    private fun openAlbumDetail(albumId: String) {
        startActivity(AlbumDetailActivity.createIntent(this, albumId))
    }

    private fun playLocalRecent(
        localItems: List<RecentLocalPlaybackItemUiModel>,
        target: RecentLocalPlaybackItemUiModel
    ) {
        val activeIndex = localItems.indexOfFirst { it.recordKey == target.recordKey }
        if (activeIndex < 0) {
            showMessage("当前歌曲暂时无法播放")
            return
        }
        val started = AppPlaybackGraph.detailPlaybackGateway(this)
            .play(
                DetailPlaybackRequest(
                    items = localItems.mapIndexed { index, item -> item.toPlaylistItem(index) },
                    activeIndex = activeIndex
                )
            )
        if (!started) {
            showMessage("播放启动失败，请稍后重试")
            return
        }
        startActivity(createRecentPlaybackPlayerIntent(this))
    }

    private fun insertLocalNext(item: RecentLocalPlaybackItemUiModel) {
        val inserted = AppPlaybackGraph.runtime(this)
            .insertPlaylistItemNext(item.toPlaylistItem(queueIndex = 0))
        showMessage(
            if (inserted) {
                "已加入下一首播放"
            } else {
                "当前没有可插入的播放上下文"
            }
        )
    }

    private fun openLocalDetail(item: RecentLocalPlaybackItemUiModel) {
        val intent = if (!item.songId.isNullOrBlank()) {
            SongDetailActivity.createOnlineIntent(
                context = this,
                songId = item.songId,
                recentRecordKey = item.recordKey,
                fallbackTitle = item.title,
                fallbackArtistText = item.artistText,
                fallbackAlbumTitle = item.albumTitle,
                fallbackDurationMs = item.durationMs,
                fallbackCoverUrl = item.imageUrl,
                fallbackPrimaryArtistId = item.primaryArtistId,
                fallbackAlbumId = item.albumId
            )
        } else {
            SongDetailActivity.createLocalIntent(
                context = this,
                playbackUri = item.playbackUri,
                title = item.title,
                artistText = item.artistText,
                albumTitle = item.albumTitle.orEmpty(),
                durationMs = item.durationMs,
                coverUrl = item.imageUrl,
                recentRecordKey = item.recordKey
            )
        }
        songDetailLauncher.launch(intent)
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, RecentSongsActivity::class.java)
        }
    }
}

internal fun createRecentPlaybackPlayerIntent(context: Context): Intent {
    return PlayerActivity.createIntent(
        context = context,
        startPlayback = true
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecentSongsScreen(
    state: RecentSongsUiState,
    onBack: () -> Unit,
    onLoginClick: () -> Unit,
    onRetry: () -> Unit,
    onSelectTab: (RecentPlaybackTab) -> Unit,
    onItemClick: (RecentSongItemUiModel) -> Unit,
    onItemInsertNext: (RecentSongItemUiModel) -> Unit,
    onItemOpenDetail: (RecentSongItemUiModel) -> Unit,
    onItemOpenArtist: (String) -> Unit,
    onItemOpenAlbum: (String) -> Unit,
    onLocalItemClick: (RecentLocalPlaybackItemUiModel) -> Unit,
    onLocalItemInsertNext: (RecentLocalPlaybackItemUiModel) -> Unit,
    onLocalItemOpenDetail: (RecentLocalPlaybackItemUiModel) -> Unit,
    onLocalItemOpenArtist: (String) -> Unit,
    onLocalItemOpenAlbum: (String) -> Unit
) {
    val visualTokens = PlayerLiteVisualTheme.colors
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = visualTokens.canvas,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "最近播放",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            lineHeight = 28.sp
                        ),
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = visualTokens.canvas
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "返回",
                            modifier = Modifier.size(22.dp),
                            tint = visualTokens.textSecondary
                        )
                    }
                },
                actions = {
                    if (state.isLoggedIn || state.selectedTab == RecentPlaybackTab.LOCAL) {
                        IconButton(
                            onClick = onRetry,
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(44.dp)
                                .testTag("recent_songs_retry_action")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "刷新",
                                modifier = Modifier.size(22.dp),
                                tint = visualTokens.textSecondary
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            RecentPlaybackTabStrip(
                selectedTab = state.selectedTab,
                onSelectTab = onSelectTab,
                modifier = Modifier.fillMaxWidth()
            )
            if (!state.isLoggedIn && state.selectedTab != RecentPlaybackTab.LOCAL) {
                RecentSongsLoginState(
                    onLoginClick = onLoginClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("recent_songs_login_state")
                )
            } else {
                RecentPlaybackContent(
                    selectedTab = state.selectedTab,
                    contentState = state.contentState,
                    onRetry = onRetry,
                    onSongClick = onItemClick,
                    onSongInsertNext = onItemInsertNext,
                    onSongOpenDetail = onItemOpenDetail,
                    onSongOpenArtist = onItemOpenArtist,
                    onSongOpenAlbum = onItemOpenAlbum,
                    onLocalItemClick = onLocalItemClick,
                    onLocalItemInsertNext = onLocalItemInsertNext,
                    onLocalItemOpenDetail = onLocalItemOpenDetail,
                    onLocalItemOpenArtist = onLocalItemOpenArtist,
                    onLocalItemOpenAlbum = onLocalItemOpenAlbum,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun RecentPlaybackTabStrip(
    selectedTab: RecentPlaybackTab,
    onSelectTab: (RecentPlaybackTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val visualTokens = PlayerLiteVisualTheme.colors
    val emphasisColor = recentPlaybackEmphasisColor()
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 8.dp, bottom = 3.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        RecentPlaybackTab.entries.forEach { tab ->
            val selected = tab == selectedTab
            Box(
                modifier = Modifier
                    .width(52.dp)
                    .height(48.dp)
                    .selectable(
                        selected = selected,
                        role = Role.Tab,
                        onClick = { onSelectTab(tab) }
                    )
                    .testTag("recent_playback_tab_${tab.testTag}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab.label,
                    modifier = Modifier.padding(horizontal = 6.dp),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = if (selected) {
                        emphasisColor
                    } else {
                        visualTokens.textSecondary
                    },
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                )
                if (selected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 1.dp)
                            .size(width = 28.dp, height = 2.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(emphasisColor)
                            .testTag("recent_playback_tab_${tab.testTag}_indicator")
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentPlaybackContent(
    selectedTab: RecentPlaybackTab,
    contentState: RecentPlaybackContentState,
    onRetry: () -> Unit,
    onSongClick: (RecentSongItemUiModel) -> Unit,
    onSongInsertNext: (RecentSongItemUiModel) -> Unit,
    onSongOpenDetail: (RecentSongItemUiModel) -> Unit,
    onSongOpenArtist: (String) -> Unit,
    onSongOpenAlbum: (String) -> Unit,
    onLocalItemClick: (RecentLocalPlaybackItemUiModel) -> Unit,
    onLocalItemInsertNext: (RecentLocalPlaybackItemUiModel) -> Unit,
    onLocalItemOpenDetail: (RecentLocalPlaybackItemUiModel) -> Unit,
    onLocalItemOpenArtist: (String) -> Unit,
    onLocalItemOpenAlbum: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    when (contentState) {
        RecentPlaybackContentState.Idle,
        RecentPlaybackContentState.Loading -> {
            RecentSongsLoadingState(
                modifier = modifier.testTag("recent_playback_loading_${selectedTab.testTag}")
            )
        }

        RecentPlaybackContentState.Empty -> {
            RecentSongsStatusState(
                title = "${selectedTab.label}最近播放为空",
                subtitle = "还没有可展示的最近播放${selectedTab.label}。",
                modifier = modifier.testTag("recent_playback_empty_${selectedTab.testTag}")
            )
        }

        is RecentPlaybackContentState.Error -> {
            RecentSongsStatusState(
                title = "${selectedTab.label}最近播放加载失败",
                subtitle = contentState.message,
                modifier = modifier.testTag("recent_playback_error_${selectedTab.testTag}")
            ) {
                Button(
                    onClick = onRetry,
                    modifier = Modifier.testTag("recent_playback_retry_${selectedTab.testTag}")
                ) {
                    Text("重试")
                }
            }
        }

        is RecentPlaybackContentState.LocalContent -> {
            LazyColumn(
                modifier = modifier.testTag("recent_playback_list_${selectedTab.testTag}"),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    bottom = 24.dp
                )
            ) {
                itemsIndexed(
                    items = contentState.items,
                    key = { _, item -> item.id }
                ) { index, item ->
                    RecentTrackRow(
                        position = index + 1,
                        title = item.title,
                        metadata = recentTrackMetadata(item.artistText, item.albumTitle),
                        imageUrl = item.imageUrl,
                        durationMs = item.durationMs,
                        positionTestTag = "recent_local_item_index_${item.id}",
                        detailTestTag = "recent_local_item_more_${item.id}",
                        onClick = { onLocalItemClick(item) },
                        onOpenDetail = { onLocalItemOpenDetail(item) },
                        showDivider = index < contentState.items.lastIndex,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("recent_local_item_${item.id}")
                    )
                }
            }
        }

        is RecentPlaybackContentState.SongContent -> {
            LazyColumn(
                modifier = modifier.testTag("recent_playback_list_${selectedTab.testTag}"),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    bottom = 24.dp
                )
            ) {
                itemsIndexed(
                    items = contentState.items,
                    key = { _, item -> item.id }
                ) { index, item ->
                    RecentTrackRow(
                        position = index + 1,
                        title = item.title,
                        metadata = recentTrackMetadata(item.artistText, item.albumTitle),
                        imageUrl = item.imageUrl,
                        durationMs = item.durationMs,
                        positionTestTag = "recent_songs_item_index_${item.id}",
                        detailTestTag = "recent_songs_item_more_${item.id}",
                        onClick = { onSongClick(item) },
                        onOpenDetail = { onSongOpenDetail(item) },
                        showDivider = index < contentState.items.lastIndex,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("recent_songs_item_${item.id}")
                    )
                }
            }
        }

        is RecentPlaybackContentState.GenericContent -> {
            LazyColumn(
                modifier = modifier.testTag("recent_playback_list_${selectedTab.testTag}"),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    bottom = 24.dp
                )
            ) {
                itemsIndexed(
                    items = contentState.items,
                    key = { _, item -> item.id }
                ) { index, item ->
                    RecentPlaybackGenericRow(
                        position = index + 1,
                        item = item,
                        showDivider = index != contentState.items.lastIndex,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("recent_playback_item_${selectedTab.testTag}_${item.id}")
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentTrackRow(
    position: Int,
    title: String,
    metadata: String,
    imageUrl: String?,
    durationMs: Long,
    positionTestTag: String,
    detailTestTag: String,
    onClick: () -> Unit,
    onOpenDetail: () -> Unit,
    showDivider: Boolean,
    modifier: Modifier = Modifier
) {
    val visualTokens = PlayerLiteVisualTheme.colors
    val emphasisColor = recentPlaybackEmphasisColor()
    Column {
        Row(
            modifier = modifier
                .clickable(onClick = onClick)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatRecentPosition(position),
                style = MaterialTheme.typography.bodyMedium,
                color = if (position <= 3) {
                    emphasisColor
                } else {
                    visualTokens.textMuted
                },
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .width(34.dp)
                    .testTag(positionTestTag)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(12.dp),
                color = visualTokens.surfaceHighlight,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (!imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = visualTokens.accentStrong,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = metadata,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = visualTokens.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier.width(44.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (durationMs > 0L) {
                    Text(
                        text = formatDurationLabel(durationMs),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = visualTokens.textSecondary,
                        modifier = Modifier.testTag("${positionTestTag}_duration")
                    )
                }
            }
            IconButton(
                onClick = onOpenDetail,
                modifier = Modifier
                    .size(48.dp)
                    .testTag(detailTestTag)
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreHoriz,
                    contentDescription = "查看歌曲详情",
                    modifier = Modifier.size(20.dp),
                    tint = visualTokens.textSecondary
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 110.dp, end = 20.dp),
                color = visualTokens.dividerSubtle,
                thickness = 0.5.dp
            )
        }
    }
}

private fun formatRecentPosition(position: Int): String {
    return position.coerceAtLeast(0).toString().padStart(2, '0')
}

@Composable
private fun recentPlaybackEmphasisColor(): Color {
    return if (isSystemInDarkTheme()) {
        PlayerLiteVisualTheme.colors.accentStrong
    } else {
        Color(0xFFD32F2F)
    }
}

private fun recentTrackMetadata(
    artistText: String,
    albumTitle: String?
): String {
    return listOfNotNull(
        artistText.takeIf { it.isNotBlank() },
        albumTitle?.takeIf { it.isNotBlank() }
    ).joinToString(" · ")
}

private fun formatDurationLabel(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%d:%02d".format(minutes, seconds)
}

@Composable
private fun RecentPlaybackGenericRow(
    position: Int,
    item: RecentPlaybackListItemUiModel,
    showDivider: Boolean,
    modifier: Modifier = Modifier
) {
    val visualTokens = PlayerLiteVisualTheme.colors
    val emphasisColor = recentPlaybackEmphasisColor()
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatRecentPosition(position),
                style = MaterialTheme.typography.bodyMedium,
                color = if (position <= 3) {
                    emphasisColor
                } else {
                    visualTokens.textMuted
                },
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .width(34.dp)
                    .testTag("recent_playback_item_index_${item.id}")
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(12.dp),
                color = visualTokens.surfaceHighlight,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.imageUrl.isNullOrBlank()) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = visualTokens.accentStrong,
                            modifier = Modifier.size(26.dp)
                        )
                    } else {
                        AsyncImage(
                            model = item.imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("recent_playback_cover_${item.id}"),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val supportingText = listOfNotNull(
                    item.subtitle.takeIf { it.isNotBlank() },
                    item.badge?.takeIf { it.isNotBlank() },
                    item.meta?.takeIf { it.isNotBlank() }
                ).joinToString(" · ")
                if (supportingText.isNotBlank()) {
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = visualTokens.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .testTag("recent_playback_more_placeholder_${item.id}"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreHoriz,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = visualTokens.textMuted
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 110.dp, end = 20.dp),
                thickness = 0.5.dp,
                color = visualTokens.dividerSubtle
            )
        }
    }
}

@Composable
private fun RecentSongsLoginState(
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "登录后才能查看最近播放",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "当前为游客浏览模式，登录后会同步你的在线播放数据。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(18.dp))
        Button(onClick = onLoginClick) {
            Text("去登录")
        }
    }
}

@Composable
private fun RecentSongsLoadingState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun RecentSongsStatusState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier.padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        action?.let {
            Spacer(modifier = Modifier.height(18.dp))
            it()
        }
    }
}
