package com.wxy.playerlite.feature.player.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.wxy.playerlite.designsystem.theme.PlayerLiteThemeContract
import com.wxy.playerlite.designsystem.theme.PlayerLiteVisualTheme
import com.wxy.playerlite.core.playlist.PlaylistItem
import com.wxy.playerlite.playback.model.PlaybackMode
import kotlinx.coroutines.flow.first

val PlaylistSheetFirstVisibleIndexKey =
    SemanticsPropertyKey<Int>("PlaylistSheetFirstVisibleIndex")

internal var SemanticsPropertyReceiver.playlistSheetFirstVisibleIndex by
    PlaylistSheetFirstVisibleIndexKey

data class PlaylistSheetLayoutSpec(
    val isLandscape: Boolean,
    val widthFraction: Float,
    val minWidthDp: Float? = null,
    val maxWidthDp: Float? = null,
    val heightFraction: Float,
    val dockToEnd: Boolean
)

fun resolvePlaylistSheetLayoutSpec(
    viewportWidthDp: Float,
    viewportHeightDp: Float
): PlaylistSheetLayoutSpec {
    val isLandscape = viewportWidthDp > viewportHeightDp
    return if (isLandscape) {
        PlaylistSheetLayoutSpec(
            isLandscape = true,
            widthFraction = 0.5f,
            minWidthDp = 360f,
            maxWidthDp = 560f,
            heightFraction = 0.84f,
            dockToEnd = true
        )
    } else {
        PlaylistSheetLayoutSpec(
            isLandscape = false,
            widthFraction = 1f,
            heightFraction = 0.74f,
            dockToEnd = false
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun PlaylistBottomSheet(
    visible: Boolean,
    items: List<PlaylistItem>,
    activeIndex: Int,
    playbackMode: PlaybackMode,
    showOriginalOrderInShuffle: Boolean,
    canReorder: Boolean,
    onDismiss: () -> Unit,
    onCyclePlaybackMode: () -> Unit = {},
    onShowOriginalOrderInShuffleChange: (Boolean) -> Unit,
    onSelect: (Int) -> Unit,
    onClearAll: () -> Unit = {},
    onRemove: (Int) -> Unit,
    onOpenSongDetail: (PlaylistItem) -> Unit = {},
    onOpenArtist: (String) -> Unit = {},
    onOpenAlbum: (String) -> Unit = {},
    onMove: (Int, Int) -> Unit,
    expandedMenuItemIdOverride: String? = null,
    modifier: Modifier = Modifier
) {
    val visualTokens = PlayerLiteVisualTheme.colors
    val brandPalette = PlayerLiteThemeContract.DefaultBrandPalettes.light
    val scrimInteraction = remember { MutableInteractionSource() }
    val reorderStepPx = with(LocalDensity.current) { 65.dp.toPx() }
    val navigationBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    var draggingIndex by remember { mutableIntStateOf(-1) }
    var draggingOffsetY by remember { mutableFloatStateOf(0f) }
    var expandedMenuItemId by remember { mutableStateOf<String?>(null) }
    var reorderModeEnabled by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val activeScrollTarget = items.getOrNull(activeIndex)?.id?.let { itemId ->
        itemId to activeIndex
    }
    var lastAutoScrolledTarget by remember { mutableStateOf<Pair<String, Int>?>(null) }

    LaunchedEffect(visible, canReorder) {
        if (!visible || !canReorder) {
            lastAutoScrolledTarget = null
            expandedMenuItemId = null
            reorderModeEnabled = false
            draggingIndex = -1
            draggingOffsetY = 0f
        }
    }

    LaunchedEffect(visible, activeScrollTarget) {
        val target = activeScrollTarget
        if (!visible || target == null || activeIndex !in items.indices) {
            return@LaunchedEffect
        }

        if (lastAutoScrolledTarget == target) {
            return@LaunchedEffect
        }

        val visibleIndexes = snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.map { itemInfo -> itemInfo.index }
        }.first { indexes -> indexes.isNotEmpty() }
        if (target.second !in visibleIndexes) {
            listState.scrollToItem(index = target.second)
        }
        lastAutoScrolledTarget = target
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(220)) + slideInVertically(
            animationSpec = tween(280),
            initialOffsetY = { it }
        ),
        exit = fadeOut(animationSpec = tween(180)) + slideOutVertically(
            animationSpec = tween(220),
            targetOffsetY = { it }
        ),
        modifier = modifier.fillMaxSize()
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val layoutSpec = resolvePlaylistSheetLayoutSpec(
                viewportWidthDp = maxWidth.value,
                viewportHeightDp = maxHeight.value
            )
            val stackHeaderActions = maxWidth < 380.dp
            val surfaceShape = if (layoutSpec.isLandscape) {
                RoundedCornerShape(22.dp)
            } else {
                RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            }
            val surfaceModifier = if (layoutSpec.isLandscape) {
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp, top = 16.dp, bottom = 16.dp)
                    .fillMaxWidth(layoutSpec.widthFraction)
                    .widthIn(
                        min = (layoutSpec.minWidthDp ?: 0f).dp,
                        max = (layoutSpec.maxWidthDp ?: Float.MAX_VALUE).dp
                    )
                    .fillMaxHeight(layoutSpec.heightFraction)
            } else {
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(layoutSpec.widthFraction)
                    .fillMaxHeight(layoutSpec.heightFraction)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.26f))
                    .clickable(
                        interactionSource = scrimInteraction,
                        indication = null,
                        onClick = onDismiss
                    )
            )

            Surface(
                modifier = surfaceModifier
                    .testTag("playlist_sheet_surface"),
                shape = surfaceShape,
                color = brandPalette.neutral,
                tonalElevation = 0.dp,
                shadowElevation = 16.dp,
                border = BorderStroke(
                    width = 1.dp,
                    color = visualTokens.dividerSubtle.copy(alpha = 0.42f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = 10.dp,
                            bottom = navigationBottomPadding
                        ),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(width = 40.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(visualTokens.handleMuted)
                    )

                    PlaylistSheetHeader(
                        stackActions = stackHeaderActions,
                        itemCount = items.size,
                        playbackMode = playbackMode,
                        canReorder = canReorder,
                        reorderModeEnabled = reorderModeEnabled,
                        visualTokens = visualTokens,
                        onCyclePlaybackMode = onCyclePlaybackMode,
                        onToggleReorder = {
                            reorderModeEnabled = !reorderModeEnabled
                            draggingIndex = -1
                            draggingOffsetY = 0f
                            expandedMenuItemId = null
                        },
                        onClearAll = onClearAll,
                        onDismiss = onDismiss
                    )

                    if (playbackMode == PlaybackMode.SHUFFLE) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "显示原始顺序",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Checkbox(
                                checked = showOriginalOrderInShuffle,
                                onCheckedChange = onShowOriginalOrderInShuffleChange
                            )
                        }
                    }

                    Text(
                        text = if (reorderModeEnabled) {
                            "长按歌曲并拖动调整顺序"
                        } else {
                            "点击排序后，长按歌曲调整顺序"
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, top = 10.dp, end = 20.dp, bottom = 12.dp)
                            .testTag("playlist_sheet_reorder_hint"),
                        style = MaterialTheme.typography.bodySmall,
                        color = visualTokens.textMuted
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(visualTokens.dividerSubtle.copy(alpha = 0.56f))
                    )

                    if (items.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "播放列表为空，点击右上角文件按钮添加音频",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("playlist_sheet_list")
                                .semantics {
                                    playlistSheetFirstVisibleIndex = listState.firstVisibleItemIndex
                            },
                            state = listState,
                            contentPadding = PaddingValues(bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                                val isActive = index == activeIndex
                                val isDragging = index == draggingIndex
                                val menuExpanded = expandedMenuItemIdOverride == item.id ||
                                    (expandedMenuItemIdOverride == null && expandedMenuItemId == item.id)
                                val itemVisuals = resolvePlaylistSheetItemVisuals(
                                    isActive = isActive,
                                    isDragging = isDragging,
                                    canReorder = canReorder && reorderModeEnabled,
                                    visualTokens = visualTokens
                                )
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("playlist_sheet_item_${item.id}")
                                        .animateItem()
                                        .zIndex(if (isDragging) 1f else 0f)
                                        .graphicsLayer {
                                            translationY = if (isDragging) draggingOffsetY else 0f
                                            scaleX = if (isDragging) 1.01f else 1f
                                            scaleY = if (isDragging) 1.01f else 1f
                                        }
                                        .let { baseModifier ->
                                            if (!canReorder || !reorderModeEnabled) {
                                                baseModifier
                                            } else {
                                                baseModifier.pointerInput(
                                                    items.size,
                                                    index,
                                                    reorderStepPx,
                                                    reorderModeEnabled
                                                ) {
                                                    detectDragGesturesAfterLongPress(
                                                        onDragStart = {
                                                            draggingIndex = index
                                                            draggingOffsetY = 0f
                                                        },
                                                        onDragEnd = {
                                                            draggingIndex = -1
                                                            draggingOffsetY = 0f
                                                        },
                                                        onDragCancel = {
                                                            draggingIndex = -1
                                                            draggingOffsetY = 0f
                                                        },
                                                        onDrag = { change, dragAmount ->
                                                            change.consume()

                                                            if (draggingIndex < 0) {
                                                                return@detectDragGesturesAfterLongPress
                                                            }

                                                            draggingOffsetY += dragAmount.y

                                                            if (draggingOffsetY > reorderStepPx && draggingIndex < items.lastIndex) {
                                                                val from = draggingIndex
                                                                val to = from + 1
                                                                onMove(from, to)
                                                                draggingIndex = to
                                                                draggingOffsetY -= reorderStepPx
                                                            }

                                                            if (draggingOffsetY < -reorderStepPx && draggingIndex > 0) {
                                                                val from = draggingIndex
                                                                val to = from - 1
                                                                onMove(from, to)
                                                                draggingIndex = to
                                                                draggingOffsetY += reorderStepPx
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                        .clickable(enabled = !isDragging) { onSelect(index) },
                                    shape = if (isDragging) RoundedCornerShape(12.dp) else RectangleShape,
                                    color = itemVisuals.containerColor,
                                    tonalElevation = if (itemVisuals.raised) 1.dp else 0.dp,
                                    shadowElevation = if (itemVisuals.raised) 3.dp else 0.dp,
                                    border = itemVisuals.border
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("playlist_sheet_item_row_${item.id}")
                                                .defaultMinSize(minHeight = 64.dp)
                                                .padding(horizontal = 20.dp),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (reorderModeEnabled) {
                                                Box(
                                                    modifier = Modifier.size(width = 28.dp, height = 44.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    PlaylistSheetDragHandle(
                                                        tint = itemVisuals.dragHandleTint,
                                                        enabled = canReorder,
                                                        modifier = Modifier
                                                            .testTag("playlist_sheet_drag_handle_${item.id}")
                                                    )
                                                }
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .testTag("playlist_sheet_artwork_${item.id}")
                                            ) {
                                                Surface(
                                                    modifier = Modifier.fillMaxSize(),
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = itemVisuals.artworkFallbackContainerColor
                                                ) {
                                                    if (!item.coverUrl.isNullOrBlank()) {
                                                        AsyncImage(
                                                            model = item.coverUrl,
                                                            contentDescription = null,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    } else {
                                                        Box(
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Rounded.LibraryMusic,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                    }
                                                }
                                                if (isActive) {
                                                    Surface(
                                                        modifier = Modifier
                                                            .align(Alignment.BottomEnd)
                                                            .padding(2.dp)
                                                            .size(18.dp)
                                                            .testTag("playlist_sheet_active_indicator_${item.id}"),
                                                        shape = CircleShape,
                                                        color = itemVisuals.titleColor.copy(alpha = 0.92f)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                imageVector = Icons.Rounded.GraphicEq,
                                                                contentDescription = "当前播放",
                                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                                modifier = Modifier.size(13.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.effectiveTitle,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                                                    color = itemVisuals.titleColor,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = resolvePlaylistSheetItemSubtitle(item),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = itemVisuals.subtitleColor,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Box {
                                                IconButton(
                                                    onClick = {
                                                            if (expandedMenuItemIdOverride != null) {
                                                                return@IconButton
                                                            }
                                                            expandedMenuItemId = if (menuExpanded) {
                                                                null
                                                            } else {
                                                                item.id
                                                            }
                                                    },
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .testTag("playlist_sheet_more_${item.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.MoreVert,
                                                        contentDescription = "更多操作",
                                                        tint = visualTokens.textSecondary,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }

                                                DropdownMenu(
                                                    expanded = menuExpanded,
                                                    onDismissRequest = {
                                                        expandedMenuItemId = null
                                                    },
                                                    modifier = Modifier
                                                        .widthIn(min = 180.dp)
                                                        .testTag("playlist_sheet_more_panel_${item.id}")
                                                ) {
                                                    if (item.songId?.isNotBlank() == true || item.uri.isNotBlank()) {
                                                        PlaylistSheetActionRow(
                                                            label = "查看歌曲详情",
                                                            tag = "playlist_sheet_action_detail_${item.id}",
                                                            onClick = {
                                                                expandedMenuItemId = null
                                                                onOpenSongDetail(item)
                                                            }
                                                        )
                                                    }
                                                    item.primaryArtistId?.takeIf { it.isNotBlank() }?.let { artistId ->
                                                        PlaylistSheetActionRow(
                                                            label = "查看歌手",
                                                            tag = "playlist_sheet_action_artist_${item.id}",
                                                            onClick = {
                                                                expandedMenuItemId = null
                                                                onOpenArtist(artistId)
                                                            }
                                                        )
                                                    }
                                                    item.albumId?.takeIf { it.isNotBlank() }?.let { albumId ->
                                                        PlaylistSheetActionRow(
                                                            label = "查看专辑",
                                                            tag = "playlist_sheet_action_album_${item.id}",
                                                            onClick = {
                                                                expandedMenuItemId = null
                                                                onOpenAlbum(albumId)
                                                            }
                                                        )
                                                    }
                                                    PlaylistSheetActionRow(
                                                        label = "移出播放队列",
                                                        tag = "playlist_sheet_action_remove_${item.id}",
                                                        destructive = true,
                                                        onClick = {
                                                            expandedMenuItemId = null
                                                            onRemove(index)
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 74.dp, end = 20.dp)
                                                .height(1.dp)
                                                .background(
                                                    visualTokens.dividerSubtle.copy(alpha = 0.50f)
                                                )
                                                .testTag("playlist_sheet_divider_${item.id}")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistSheetHeader(
    stackActions: Boolean,
    itemCount: Int,
    playbackMode: PlaybackMode,
    canReorder: Boolean,
    reorderModeEnabled: Boolean,
    visualTokens: com.wxy.playerlite.designsystem.theme.PlayerLiteVisualTokens,
    onCyclePlaybackMode: () -> Unit,
    onToggleReorder: () -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    if (stackActions) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 10.dp, end = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PlaylistSheetHeaderTitle(
                    itemCount = itemCount,
                    visualTokens = visualTokens
                )
                PlaylistSheetCloseButton(onDismiss = onDismiss)
            }
            PlaylistSheetHeaderActions(
                modifier = Modifier.fillMaxWidth(),
                itemCount = itemCount,
                playbackMode = playbackMode,
                canReorder = canReorder,
                reorderModeEnabled = reorderModeEnabled,
                visualTokens = visualTokens,
                includeClose = false,
                onCyclePlaybackMode = onCyclePlaybackMode,
                onToggleReorder = onToggleReorder,
                onClearAll = onClearAll,
                onDismiss = onDismiss
            )
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 10.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlaylistSheetHeaderTitle(
                itemCount = itemCount,
                visualTokens = visualTokens,
                modifier = Modifier.weight(1f)
            )
            PlaylistSheetHeaderActions(
                itemCount = itemCount,
                playbackMode = playbackMode,
                canReorder = canReorder,
                reorderModeEnabled = reorderModeEnabled,
                visualTokens = visualTokens,
                includeClose = true,
                onCyclePlaybackMode = onCyclePlaybackMode,
                onToggleReorder = onToggleReorder,
                onClearAll = onClearAll,
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
private fun PlaylistSheetHeaderTitle(
    itemCount: Int,
    visualTokens: com.wxy.playerlite.designsystem.theme.PlayerLiteVisualTokens,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "接下来播放",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "$itemCount 首歌曲",
            style = MaterialTheme.typography.bodySmall,
            color = visualTokens.textMuted
        )
    }
}

@Composable
private fun PlaylistSheetHeaderActions(
    itemCount: Int,
    playbackMode: PlaybackMode,
    canReorder: Boolean,
    reorderModeEnabled: Boolean,
    visualTokens: com.wxy.playerlite.designsystem.theme.PlayerLiteVisualTokens,
    includeClose: Boolean,
    onCyclePlaybackMode: () -> Unit,
    onToggleReorder: () -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onCyclePlaybackMode)
                .testTag("playlist_sheet_mode_button")
                .padding(horizontal = 6.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = playbackMode.icon(),
                contentDescription = null,
                tint = visualTokens.accentStrong,
                modifier = Modifier.size(17.dp)
            )
            Text(
                text = playbackMode.label(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = visualTokens.accentStrong
            )
        }
        if (canReorder && itemCount > 1) {
            TextButton(
                onClick = onToggleReorder,
                modifier = Modifier
                    .defaultMinSize(minWidth = 44.dp)
                    .testTag("playlist_sheet_reorder_toggle"),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text(
                    text = if (reorderModeEnabled) "完成" else "排序",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = visualTokens.accentStrong
                )
            }
        }
        if (itemCount > 0) {
            TextButton(
                onClick = onClearAll,
                modifier = Modifier
                    .defaultMinSize(minWidth = 44.dp)
                    .testTag("playlist_sheet_clear_all"),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text(
                    text = "清空",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = visualTokens.accentStrong
                )
            }
        }
        if (includeClose) {
            PlaylistSheetCloseButton(onDismiss = onDismiss)
        }
    }
}

@Composable
private fun PlaylistSheetCloseButton(onDismiss: () -> Unit) {
    IconButton(
        onClick = onDismiss,
        modifier = Modifier.size(40.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = "关闭播放列表",
            modifier = Modifier.size(24.dp)
        )
    }
}

private fun PlaybackMode.label(): String {
    return when (this) {
        PlaybackMode.LIST_LOOP -> "列表循环"
        PlaybackMode.SINGLE_LOOP -> "单曲循环"
        PlaybackMode.SHUFFLE -> "随机播放"
    }
}

@Composable
private fun PlaylistSheetActionRow(
    label: String,
    tag: String,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (destructive) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        },
        onClick = onClick,
        modifier = Modifier
            .testTag(tag)
    )
}

private fun PlaybackMode.icon() = when (this) {
    PlaybackMode.LIST_LOOP -> Icons.Rounded.Repeat
    PlaybackMode.SINGLE_LOOP -> Icons.Rounded.RepeatOne
    PlaybackMode.SHUFFLE -> Icons.Rounded.Shuffle
}

data class PlaylistSheetItemVisuals(
    val containerColor: Color,
    val titleColor: Color,
    val subtitleColor: Color,
    val dragHandleTint: Color,
    val artworkFallbackContainerColor: Color,
    val border: BorderStroke?,
    val raised: Boolean
)

fun resolvePlaylistSheetItemVisuals(
    isActive: Boolean,
    isDragging: Boolean,
    canReorder: Boolean,
    visualTokens: com.wxy.playerlite.designsystem.theme.PlayerLiteVisualTokens
): PlaylistSheetItemVisuals {
    val baseBorder = BorderStroke(
        width = 1.dp,
        color = visualTokens.dividerSubtle.copy(alpha = 0.48f)
    )
    return when {
        isDragging -> PlaylistSheetItemVisuals(
            containerColor = visualTokens.surfaceRaised,
            titleColor = PlayerLiteThemeContract.DefaultBrandPalettes.light.onSurface,
            subtitleColor = visualTokens.textMuted,
            dragHandleTint = visualTokens.accentStrong,
            artworkFallbackContainerColor = visualTokens.accentStrong.copy(alpha = 0.10f),
            border = baseBorder,
            raised = true
        )

        isActive -> PlaylistSheetItemVisuals(
            containerColor = visualTokens.accentStrong.copy(alpha = 0.055f),
            titleColor = visualTokens.accentStrong,
            subtitleColor = visualTokens.textMuted,
            dragHandleTint = visualTokens.accentStrong,
            artworkFallbackContainerColor = visualTokens.accentStrong.copy(alpha = 0.10f),
            border = null,
            raised = false
        )

        else -> PlaylistSheetItemVisuals(
            containerColor = Color.Transparent,
            titleColor = PlayerLiteThemeContract.DefaultBrandPalettes.light.onSurface,
            subtitleColor = visualTokens.textMuted,
            dragHandleTint = visualTokens.textSecondary,
            artworkFallbackContainerColor = visualTokens.surfaceMuted.copy(alpha = 0.85f),
            border = null,
            raised = false
        )
    }
}

fun resolvePlaylistSheetItemSubtitle(item: PlaylistItem): String {
    return item.artistText
        ?.takeIf { it.isNotBlank() }
        ?: item.albumTitle?.takeIf { it.isNotBlank() }
        ?: if (item.isOnline) "在线歌曲" else "本地音频"
}

@Composable
private fun PlaylistSheetDragHandle(
    tint: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.DragHandle,
            contentDescription = "拖动调整顺序",
            tint = if (enabled) tint else tint.copy(alpha = 0.35f),
            modifier = Modifier.size(24.dp)
        )
    }
}
