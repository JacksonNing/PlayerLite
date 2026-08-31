package com.wxy.playerlite.feature.local

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wxy.playerlite.designsystem.theme.PlayerLiteVisualTheme
import com.wxy.playerlite.core.playback.AppPlaybackGraph
import com.wxy.playerlite.feature.song.SongDetailActivity
import com.wxy.playerlite.ui.theme.PlayerLiteAppTheme
import com.wxy.playerlite.ui.theme.applyInitialPlayerLiteSystemBars

class LocalSongsActivity : ComponentActivity() {
    private val viewModel: LocalSongsViewModel by viewModels {
        LocalSongsViewModel.factory(this)
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onPermissionStateChanged(granted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        applyInitialPlayerLiteSystemBars()
        viewModel.onPermissionStateChanged(hasLocalSongsPermission())
        setContent {
            PlayerLiteAppTheme {
                val state = viewModel.uiStateFlow.collectAsStateWithLifecycle().value
                BackHandler(onBack = ::finish)
                LaunchedEffect(viewModel) {
                    viewModel.uiEvents.collect { event ->
                        when (event) {
                            LocalSongsUiEvent.OpenPlayer -> {
                                setResult(
                                    RESULT_OK,
                                    createOpenPlayerResultIntent()
                                )
                                finish()
                            }

                            is LocalSongsUiEvent.ShowMessage -> {
                                Toast.makeText(
                                    this@LocalSongsActivity,
                                    event.message,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
                LocalSongsScreen(
                    state = state,
                    onBack = ::finish,
                    onRequestPermission = {
                        permissionLauncher.launch(requiredLocalSongsPermission())
                    },
                    onScan = {
                        if (hasLocalSongsPermission()) {
                            viewModel.onScanRequested()
                        } else {
                            permissionLauncher.launch(requiredLocalSongsPermission())
                        }
                    },
                    onPlayAll = viewModel::playAll,
                    onSongClick = viewModel::playSong,
                    onSongInsertNext = { song ->
                        val inserted = AppPlaybackGraph.runtime(this@LocalSongsActivity)
                            .insertPlaylistItemNext(song.toPlaylistItem())
                        Toast.makeText(
                            this@LocalSongsActivity,
                            if (inserted) {
                                "已加入下一首播放"
                            } else {
                                "当前没有可插入的播放上下文"
                            },
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onSongOpenDetail = { song ->
                        startActivity(
                            SongDetailActivity.createIntent(
                                context = this@LocalSongsActivity,
                                ref = song.toSongRef()
                            )
                        )
                    }
                )
            }
        }
    }

    companion object {
        private const val EXTRA_OPEN_PLAYER = "open_player"

        fun createIntent(context: Context): Intent {
            return Intent(context, LocalSongsActivity::class.java)
        }

        fun createOpenPlayerResultIntent(): Intent {
            return Intent().putExtra(EXTRA_OPEN_PLAYER, true)
        }

        fun shouldOpenPlayerFromResult(
            resultCode: Int,
            data: Intent?
        ): Boolean {
            return resultCode == RESULT_OK &&
                data?.getBooleanExtra(EXTRA_OPEN_PLAYER, false) == true
        }
    }

    private fun hasLocalSongsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            requiredLocalSongsPermission()
        ) == PackageManager.PERMISSION_GRANTED
    }
}

internal fun requiredLocalSongsPermission(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun LocalSongsScreen(
    state: LocalSongsUiState,
    onBack: () -> Unit,
    onRequestPermission: () -> Unit,
    onScan: () -> Unit,
    onPlayAll: () -> Unit,
    onSongClick: (Int) -> Unit,
    onSongInsertNext: (LocalSongEntry) -> Unit,
    onSongOpenDetail: (LocalSongEntry) -> Unit
) {
    val visualTokens = PlayerLiteVisualTheme.colors
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = visualTokens.canvas,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "本地歌曲",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = onScan,
                            modifier = Modifier.testTag("local_songs_scan_action")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "扫描本地歌曲"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = visualTokens.canvas
                    )
                )
                HorizontalDivider(
                    color = visualTokens.dividerSubtle,
                    thickness = 1.dp
                )
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isScanning,
            onRefresh = onScan,
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                state.requiresPermission -> {
                    LocalSongsPermissionState(
                        onRequestPermission = onRequestPermission,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }

                state.isLoading -> {
                    LocalSongsLoadingState(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }

                state.errorMessage != null && state.songs.isEmpty() -> {
                    LocalSongsStatusState(
                        title = "本地歌曲加载失败",
                        subtitle = state.errorMessage,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }

                state.songs.isEmpty() -> {
                    LocalSongsStatusState(
                        title = "还没有扫描到本地歌曲",
                        subtitle = "点击右上角“扫描”后，这里的结果会被缓存，下次打开可直接展示。",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .testTag("local_songs_list"),
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            bottom = 24.dp
                        )
                    ) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 64.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${state.songs.size} 首歌曲",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = visualTokens.textSecondary,
                                    modifier = Modifier.testTag("local_songs_song_count")
                                )
                                TextButton(
                                    onClick = onPlayAll,
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("local_songs_play_all"),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayCircleOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text(
                                        text = "播放全部",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            HorizontalDivider(color = visualTokens.dividerSubtle)
                        }
                        itemsIndexed(
                            items = state.songs,
                            key = { _, item -> item.id }
                        ) { index, item ->
                            LocalSongRow(
                                item = item,
                                onClick = { onSongClick(index) },
                                onInsertNext = { onSongInsertNext(item) },
                                onOpenDetail = { onSongOpenDetail(item) }
                            )
                            if (index < state.songs.lastIndex) {
                                HorizontalDivider(color = visualTokens.dividerSubtle)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalSongRow(
    item: LocalSongEntry,
    onClick: () -> Unit,
    onInsertNext: () -> Unit,
    onOpenDetail: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visualTokens = PlayerLiteVisualTheme.colors
    var menuExpanded by remember(item.id) { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 84.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
            .testTag("local_songs_item_${item.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier.size(52.dp),
            shape = RoundedCornerShape(14.dp),
            color = visualTokens.surfaceHighlight,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = visualTokens.accentStrong,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${item.artist} · ${item.album}",
                style = MaterialTheme.typography.bodyMedium,
                color = visualTokens.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = formatLocalSongDuration(item.durationMs),
            style = MaterialTheme.typography.bodyMedium,
            color = visualTokens.textSecondary,
            modifier = Modifier.testTag("local_songs_item_duration_${item.id}")
        )
        Box {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("local_songs_item_more_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "更多操作",
                    tint = visualTokens.textSecondary
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("下一首播放") },
                    onClick = {
                        menuExpanded = false
                        onInsertNext()
                    }
                )
                DropdownMenuItem(
                    text = { Text("查看歌曲详情") },
                    onClick = {
                        menuExpanded = false
                        onOpenDetail()
                    }
                )
            }
        }
    }
}

internal fun formatLocalSongDuration(durationMs: Long): String {
    val totalSeconds = durationMs.coerceAtLeast(0L) / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    val paddedSeconds = seconds.toString().padStart(2, '0')
    return if (hours > 0L) {
        "$hours:${minutes.toString().padStart(2, '0')}:$paddedSeconds"
    } else {
        "$minutes:$paddedSeconds"
    }
}

@Composable
private fun LocalSongsPermissionState(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "需要音频读取权限",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = "授权后才能扫描本机音频并缓存结果，下次打开可直接展示。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.size(16.dp))
        Button(onClick = onRequestPermission) {
            Text("去授权")
        }
    }
}

@Composable
private fun LocalSongsLoadingState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.size(12.dp))
        Text("正在扫描本地歌曲")
    }
}

@Composable
private fun LocalSongsStatusState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
