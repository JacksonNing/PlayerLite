package com.wxy.playerlite.feature.player.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wxy.playerlite.core.playlist.PlaylistItem
import com.wxy.playerlite.designsystem.theme.PlayerLiteThemeContract
import com.wxy.playerlite.playback.model.PlaybackMode
import com.wxy.playerlite.ui.theme.PlayerLiteTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlaylistSheetVisualsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun resolvePlaylistSheetItemVisuals_shouldHighlightActiveItemAndKeepInactiveRowsLightweight() {
        val colorScheme = PlayerLiteThemeContract.colorScheme(darkTheme = false)
        val visualTokens = PlayerLiteThemeContract.visualTokens(
            darkTheme = false,
            colorScheme = colorScheme
        )

        val active = resolvePlaylistSheetItemVisuals(
            isActive = true,
            isDragging = false,
            canReorder = true,
            visualTokens = visualTokens,
            onSurfaceColor = colorScheme.onSurface
        )
        val inactive = resolvePlaylistSheetItemVisuals(
            isActive = false,
            isDragging = false,
            canReorder = true,
            visualTokens = visualTokens,
            onSurfaceColor = colorScheme.onSurface
        )

        assertEquals(visualTokens.accentStrong.copy(alpha = 0.055f), active.containerColor)
        assertEquals(visualTokens.accentStrong, active.titleColor)
        assertEquals(visualTokens.textMuted, active.subtitleColor)
        assertEquals(null, active.border)
        assertTrue(!active.raised)
        assertEquals(Color.Transparent, inactive.containerColor)
        assertEquals(colorScheme.onSurface, inactive.titleColor)
        assertEquals(visualTokens.textMuted, inactive.subtitleColor)
        assertEquals(null, inactive.border)
    }

    @Test
    fun resolvePlaylistSheetItemVisuals_shouldUseCurrentDarkThemeOnSurfaceColor() {
        val colorScheme = PlayerLiteThemeContract.colorScheme(darkTheme = true)
        val visualTokens = PlayerLiteThemeContract.visualTokens(
            darkTheme = true,
            colorScheme = colorScheme
        )

        val inactive = resolvePlaylistSheetItemVisuals(
            isActive = false,
            isDragging = false,
            canReorder = false,
            visualTokens = visualTokens,
            onSurfaceColor = colorScheme.onSurface
        )

        assertEquals(colorScheme.onSurface, inactive.titleColor)
    }

    @Test
    fun resolvePlaylistSheetItemVisuals_shouldUseCustomSkinTokensWithoutDefaultPaletteLeakage() {
        val customPalettes = PlayerLiteThemeContract.DefaultBrandPalettes.copy(
            dark = PlayerLiteThemeContract.DefaultBrandPalettes.dark.copy(
                primary = Color(0xFF123456),
                surfaceRaised = Color(0xFF234567),
                onSurface = Color(0xFF345678)
            )
        )
        val colorScheme = PlayerLiteThemeContract.colorScheme(
            darkTheme = true,
            brandPalettes = customPalettes
        )
        val visualTokens = PlayerLiteThemeContract.visualTokens(
            darkTheme = true,
            colorScheme = colorScheme,
            brandPalettes = customPalettes
        )

        val active = resolvePlaylistSheetItemVisuals(
            isActive = true,
            isDragging = false,
            canReorder = true,
            visualTokens = visualTokens,
            onSurfaceColor = colorScheme.onSurface
        )
        val dragging = resolvePlaylistSheetItemVisuals(
            isActive = false,
            isDragging = true,
            canReorder = true,
            visualTokens = visualTokens,
            onSurfaceColor = colorScheme.onSurface
        )

        assertEquals(Color(0xFF123456), active.titleColor)
        assertEquals(Color(0xFF234567), dragging.containerColor)
        assertEquals(Color(0xFF345678), dragging.titleColor)
    }

    @Test
    fun playlistBottomSheet_shouldHideReorderHintWhenReorderUnavailableOrSingleItem() {
        composeRule.setContent {
            PlayerLiteTheme {
                PlaylistBottomSheet(
                    visible = true,
                    items = listOf(
                        PlaylistItem(
                            id = "single",
                            uri = "file:///single.mp3",
                            displayName = "单曲"
                        )
                    ),
                    activeIndex = 0,
                    playbackMode = PlaybackMode.LIST_LOOP,
                    showOriginalOrderInShuffle = false,
                    canReorder = true,
                    onDismiss = {},
                    onShowOriginalOrderInShuffleChange = {},
                    onSelect = {},
                    onRemove = {},
                    onMove = { _, _ -> }
                )
            }
        }

        composeRule.onAllNodesWithTag("playlist_sheet_reorder_hint").assertCountEquals(0)
        composeRule.onNodeWithTag("playlist_sheet_more_single").assertWidthIsAtLeast(48.dp)
        composeRule.onNodeWithTag("playlist_sheet_more_single").assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithTag("playlist_sheet_mode_button").assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun playlistBottomSheet_shouldHideReorderControlsWhenReorderIsDisabled() {
        composeRule.setContent {
            PlayerLiteTheme {
                PlaylistBottomSheet(
                    visible = true,
                    items = listOf(
                        PlaylistItem(id = "first", uri = "file:///first.mp3", displayName = "第一首"),
                        PlaylistItem(id = "second", uri = "file:///second.mp3", displayName = "第二首")
                    ),
                    activeIndex = 0,
                    playbackMode = PlaybackMode.LIST_LOOP,
                    showOriginalOrderInShuffle = false,
                    canReorder = false,
                    onDismiss = {},
                    onShowOriginalOrderInShuffleChange = {},
                    onSelect = {},
                    onRemove = {},
                    onMove = { _, _ -> }
                )
            }
        }

        composeRule.onAllNodesWithTag("playlist_sheet_reorder_hint").assertCountEquals(0)
        composeRule.onAllNodesWithTag("playlist_sheet_reorder_toggle").assertCountEquals(0)
        composeRule.onAllNodesWithTag("playlist_sheet_drag_handle_first").assertCountEquals(0)
    }

    @Test
    fun playlistBottomSheet_emptyState_shouldUseThemeNeutralCopyWithoutMissingActionReference() {
        composeRule.setContent {
            PlayerLiteTheme {
                PlaylistBottomSheet(
                    visible = true,
                    items = emptyList(),
                    activeIndex = -1,
                    playbackMode = PlaybackMode.LIST_LOOP,
                    showOriginalOrderInShuffle = false,
                    canReorder = true,
                    onDismiss = {},
                    onShowOriginalOrderInShuffleChange = {},
                    onSelect = {},
                    onRemove = {},
                    onMove = { _, _ -> }
                )
            }
        }

        composeRule.onNodeWithText("播放列表为空，添加音频后会显示在这里").assertIsDisplayed()
        composeRule.onAllNodesWithText("播放列表为空，点击右上角文件按钮添加音频").assertCountEquals(0)
    }

    @Test
    fun resolvePlaylistSheetItemSubtitle_shouldPreferArtistOverInteractionHintText() {
        val item = PlaylistItem(
            id = "track-1",
            uri = "https://example.com/track-1.mp3",
            displayName = "悬日",
            title = "悬日",
            artistText = "田馥甄",
            albumTitle = "无人知晓"
        )

        assertEquals("田馥甄", resolvePlaylistSheetItemSubtitle(item))
    }

    @Test
    fun resolvePlaylistSheetLayoutSpec_shouldUseHalfWidthSidePanelInLandscape() {
        val spec = resolvePlaylistSheetLayoutSpec(
            viewportWidthDp = 960f,
            viewportHeightDp = 540f
        )

        assertTrue(spec.isLandscape)
        assertEquals(0.5f, spec.widthFraction)
        assertEquals(360f, spec.minWidthDp)
        assertEquals(560f, spec.maxWidthDp)
        assertEquals(0.84f, spec.heightFraction)
        assertTrue(spec.dockToEnd)
    }

    @Test
    fun resolvePlaylistSheetLayoutSpec_shouldKeepFullWidthBottomSheetInPortrait() {
        val spec = resolvePlaylistSheetLayoutSpec(
            viewportWidthDp = 360f,
            viewportHeightDp = 760f
        )

        assertTrue(!spec.isLandscape)
        assertEquals(1f, spec.widthFraction)
        assertEquals(null, spec.minWidthDp)
        assertEquals(null, spec.maxWidthDp)
        assertEquals(0.74f, spec.heightFraction)
        assertTrue(!spec.dockToEnd)
    }

    @Test
    fun playlistBottomSheet_shouldScrollActiveItemIntoViewportWhenOpened() {
        val items = buildPlaylistItems(prefix = "opened")

        composeRule.setContent {
            PlayerLiteTheme {
                PlaylistBottomSheet(
                    visible = true,
                    items = items,
                    activeIndex = 30,
                    playbackMode = PlaybackMode.LIST_LOOP,
                    showOriginalOrderInShuffle = false,
                    canReorder = true,
                    onDismiss = {},
                    onShowOriginalOrderInShuffleChange = {},
                    onSelect = {},
                    onRemove = {},
                    onMove = { _, _ -> }
                )
            }
        }

        composeRule.waitForIdle()
        waitUntilFirstVisibleIndex(expected = 30)

        composeRule
            .onNodeWithTag("playlist_sheet_artwork_opened-30", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun playlistBottomSheet_shouldNotClipFirstRowWhenActiveItemIsAlreadyVisible() {
        composeRule.setContent {
            PlayerLiteTheme {
                Box(modifier = Modifier.size(width = 360.dp, height = 760.dp)) {
                    PlaylistBottomSheet(
                        visible = true,
                        items = buildPlaylistItems(prefix = "visible").take(10),
                        activeIndex = 1,
                        playbackMode = PlaybackMode.LIST_LOOP,
                        showOriginalOrderInShuffle = false,
                        canReorder = true,
                        onDismiss = {},
                        onShowOriginalOrderInShuffleChange = {},
                        onSelect = {},
                        onRemove = {},
                        onMove = { _, _ -> }
                    )
                }
            }
        }

        waitUntilFirstVisibleIndex(expected = 0)
        composeRule
            .onNodeWithTag("playlist_sheet_artwork_visible-0", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun playlistBottomSheet_rows_shouldUseBalancedVerticalDensity() {
        val items = buildPlaylistItems(prefix = "density")

        composeRule.setContent {
            PlayerLiteTheme {
                Box(modifier = Modifier.size(width = 360.dp, height = 760.dp)) {
                    PlaylistBottomSheet(
                        visible = true,
                        items = items,
                        activeIndex = 0,
                        playbackMode = PlaybackMode.LIST_LOOP,
                        showOriginalOrderInShuffle = false,
                        canReorder = true,
                        onDismiss = {},
                        onShowOriginalOrderInShuffleChange = {},
                        onSelect = {},
                        onRemove = {},
                        onMove = { _, _ -> }
                    )
                }
            }
        }

        composeRule.waitForIdle()
        val activeRowBounds = composeRule
            .onNodeWithTag("playlist_sheet_item_row_density-0", useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot
        val nextRowBounds = composeRule
            .onNodeWithTag("playlist_sheet_item_row_density-1", useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot
        val activeHeight = with(composeRule.density) { activeRowBounds.height.toDp() }
        val rowGap = with(composeRule.density) { (nextRowBounds.top - activeRowBounds.bottom).toDp() }

        assertTrue(
            "Expected queue row to remain comfortably tappable, but height was $activeHeight",
            activeHeight >= 64.dp
        )
        assertTrue(
            "Expected queue row to stay compact, but height was $activeHeight",
            activeHeight <= 74.dp
        )
        assertTrue(
            "Expected continuous queue rows without card gaps, but gap was $rowGap",
            rowGap >= 0.dp
        )
        assertTrue(
            "Expected only a thin divider between queue rows, but gap was $rowGap",
            rowGap <= 2.dp
        )
    }

    @Test
    fun playlistBottomSheet_activeIndicator_shouldOverlayArtworkBottomEnd() {
        composeRule.setContent {
            PlayerLiteTheme {
                Box(modifier = Modifier.size(width = 360.dp, height = 760.dp)) {
                    PlaylistBottomSheet(
                        visible = true,
                        items = buildPlaylistItems(prefix = "overlay").take(2),
                        activeIndex = 0,
                        playbackMode = PlaybackMode.LIST_LOOP,
                        showOriginalOrderInShuffle = false,
                        canReorder = true,
                        onDismiss = {},
                        onShowOriginalOrderInShuffleChange = {},
                        onSelect = {},
                        onRemove = {},
                        onMove = { _, _ -> }
                    )
                }
            }
        }

        composeRule.waitForIdle()
        val artworkBounds = composeRule
            .onNodeWithTag("playlist_sheet_artwork_overlay-0", useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot
        val indicatorBounds = composeRule
            .onNodeWithTag("playlist_sheet_active_indicator_overlay-0", useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot

        assertTrue(indicatorBounds.left >= artworkBounds.left)
        assertTrue(indicatorBounds.top >= artworkBounds.top)
        assertTrue(indicatorBounds.right <= artworkBounds.right)
        assertTrue(indicatorBounds.bottom <= artworkBounds.bottom)
        assertTrue(indicatorBounds.center.x > artworkBounds.center.x)
        assertTrue(indicatorBounds.center.y > artworkBounds.center.y)
        composeRule
            .onAllNodesWithTag("playlist_sheet_active_indicator_overlay-1", useUnmergedTree = true)
            .assertCountEquals(0)
    }

    @Test
    fun playlistBottomSheet_shouldFollowActiveItemIdentityChangeWhileVisible() {
        var items by mutableStateOf(buildPlaylistItems(prefix = "before"))

        composeRule.setContent {
            PlayerLiteTheme {
                PlaylistBottomSheet(
                    visible = true,
                    items = items,
                    activeIndex = 30,
                    playbackMode = PlaybackMode.LIST_LOOP,
                    showOriginalOrderInShuffle = false,
                    canReorder = true,
                    onDismiss = {},
                    onShowOriginalOrderInShuffleChange = {},
                    onSelect = {},
                    onRemove = {},
                    onMove = { _, _ -> }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule
            .onNodeWithTag("playlist_sheet_artwork_before-30", useUnmergedTree = true)
            .assertIsDisplayed()

        scrollAwayFromActiveItem()

        composeRule.runOnIdle {
            items = buildPlaylistItems(prefix = "after")
        }
        composeRule.waitForIdle()
        waitUntilFirstVisibleIndex(expected = 30)

        composeRule
            .onNodeWithTag("playlist_sheet_artwork_after-30", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun playlistBottomSheet_shouldNotStealScrollOnPlainRecompose() {
        val items = buildPlaylistItems(prefix = "stable")
        var recomposeTick by mutableIntStateOf(0)

        composeRule.setContent {
            PlayerLiteTheme {
                recomposeTick
                PlaylistBottomSheet(
                    visible = true,
                    items = items,
                    activeIndex = 30,
                    playbackMode = PlaybackMode.LIST_LOOP,
                    showOriginalOrderInShuffle = false,
                    canReorder = true,
                    onDismiss = {},
                    onShowOriginalOrderInShuffleChange = {},
                    onSelect = {},
                    onRemove = {},
                    onMove = { _, _ -> }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule
            .onNodeWithTag("playlist_sheet_artwork_stable-30", useUnmergedTree = true)
            .assertIsDisplayed()

        scrollAwayFromActiveItem()

        composeRule.runOnIdle {
            recomposeTick += 1
        }
        composeRule.waitForIdle()

        waitUntilFirstVisibleIndex(expected = 0)
    }

    @Test
    fun playlistBottomSheet_shouldShowAndCyclePlaybackModeFromHeader() {
        var playbackMode by mutableStateOf(PlaybackMode.LIST_LOOP)
        var cycleCount = 0

        composeRule.setContent {
            PlayerLiteTheme {
                PlaylistBottomSheet(
                    visible = true,
                    items = buildPlaylistItems(prefix = "mode"),
                    activeIndex = 0,
                    playbackMode = playbackMode,
                    showOriginalOrderInShuffle = false,
                    canReorder = true,
                    onDismiss = {},
                    onCyclePlaybackMode = {
                        cycleCount += 1
                        playbackMode = when (playbackMode) {
                            PlaybackMode.LIST_LOOP -> PlaybackMode.SINGLE_LOOP
                            PlaybackMode.SINGLE_LOOP -> PlaybackMode.SHUFFLE
                            PlaybackMode.SHUFFLE -> PlaybackMode.LIST_LOOP
                        }
                    },
                    onShowOriginalOrderInShuffleChange = {},
                    onSelect = {},
                    onRemove = {},
                    onMove = { _, _ -> }
                )
            }
        }

        composeRule.onNodeWithText("列表循环").assertIsDisplayed()
        composeRule.onNodeWithTag("playlist_sheet_mode_button")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onNodeWithText("单曲循环").assertIsDisplayed()
        composeRule.onNodeWithTag("playlist_sheet_mode_button")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onNodeWithText("随机播放").assertIsDisplayed()

        composeRule.runOnIdle {
            assertEquals(2, cycleCount)
        }
    }

    @Test
    fun playlistBottomSheet_shouldOnlyShowDragHandlesWhileSorting() {
        composeRule.setContent {
            PlayerLiteTheme {
                PlaylistBottomSheet(
                    visible = true,
                    items = listOf(
                        PlaylistItem(
                            id = "sort-1",
                            uri = "file:///sort-1.mp3",
                            displayName = "歌曲 1"
                        ),
                        PlaylistItem(
                            id = "sort-2",
                            uri = "file:///sort-2.mp3",
                            displayName = "歌曲 2"
                        )
                    ),
                    activeIndex = 0,
                    playbackMode = PlaybackMode.LIST_LOOP,
                    showOriginalOrderInShuffle = false,
                    canReorder = true,
                    onDismiss = {},
                    onShowOriginalOrderInShuffleChange = {},
                    onSelect = {},
                    onRemove = {},
                    onMove = { _, _ -> }
                )
            }
        }

        composeRule
            .onNodeWithTag("playlist_sheet_active_indicator_sort-1", useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule
            .onAllNodesWithTag("playlist_sheet_drag_handle_sort-1", useUnmergedTree = true)
            .assertCountEquals(0)
        composeRule.onNodeWithTag("playlist_sheet_more_sort-1").assertIsDisplayed()
        composeRule.onNodeWithText("排序").assertIsDisplayed()

        composeRule.onNodeWithTag("playlist_sheet_reorder_toggle")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("playlist_sheet_reorder_toggle").assertIsDisplayed()
        composeRule
            .onAllNodesWithTag("playlist_sheet_drag_handle_sort-1", useUnmergedTree = true)
            .assertCountEquals(1)
        composeRule
            .onAllNodesWithTag("playlist_sheet_active_indicator_sort-1", useUnmergedTree = true)
            .assertCountEquals(1)
        composeRule.onNodeWithTag("playlist_sheet_more_sort-1").assertIsDisplayed()
    }

    @Test
    fun playlistBottomSheet_moreMenu_shouldFloatWithoutChangingQueueItemLayout() {
        composeRule.setContent {
            PlayerLiteTheme {
                PlaylistBottomSheet(
                    visible = true,
                    items = listOf(
                        PlaylistItem(
                            id = "queue-1",
                            uri = "https://example.com/queue-1.mp3",
                            displayName = "夜曲",
                            songId = "song-1",
                            title = "夜曲",
                            artistText = "周杰伦"
                        ),
                        PlaylistItem(
                            id = "queue-2",
                            uri = "https://example.com/queue-2.mp3",
                            displayName = "稻香",
                            songId = "song-2",
                            title = "稻香",
                            artistText = "周杰伦"
                        )
                    ),
                    activeIndex = 0,
                    playbackMode = PlaybackMode.LIST_LOOP,
                    showOriginalOrderInShuffle = false,
                    canReorder = true,
                    onDismiss = {},
                    onShowOriginalOrderInShuffleChange = {},
                    onSelect = {},
                    onRemove = {},
                    onMove = { _, _ -> }
                )
            }
        }

        val firstItemBoundsBefore = composeRule
            .onNodeWithTag("playlist_sheet_item_queue-1")
            .fetchSemanticsNode()
            .boundsInRoot
        val secondItemBoundsBefore = composeRule
            .onNodeWithTag("playlist_sheet_item_queue-2")
            .fetchSemanticsNode()
            .boundsInRoot

        composeRule.onNodeWithTag("playlist_sheet_more_queue-1")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag("playlist_sheet_action_detail_queue-1").assertCountEquals(1)

        val firstItemBoundsAfter = composeRule
            .onNodeWithTag("playlist_sheet_item_queue-1")
            .fetchSemanticsNode()
            .boundsInRoot
        val secondItemBoundsAfter = composeRule
            .onNodeWithTag("playlist_sheet_item_queue-2")
            .fetchSemanticsNode()
            .boundsInRoot

        assertTrue(
            "More menu should not change the current queue item height",
            kotlin.math.abs(firstItemBoundsBefore.height - firstItemBoundsAfter.height) < 1f
        )
        assertTrue(
            "More menu should float instead of pushing the next queue item",
            kotlin.math.abs(secondItemBoundsBefore.top - secondItemBoundsAfter.top) < 1f
        )
    }

    @Test
    fun playlistBottomSheet_shouldOpenQueueAwareMoreMenuAndDispatchActions() {
        var detailId: String? = null
        var artistId: String? = null
        var albumId: String? = null
        var removedIndex = -1

        composeRule.setContent {
            PlayerLiteTheme {
                PlaylistBottomSheet(
                    visible = true,
                    items = listOf(
                        PlaylistItem(
                            id = "queue-1",
                            uri = "https://example.com/queue-1.mp3",
                            displayName = "夜曲",
                            songId = "song-1",
                            title = "夜曲",
                            artistText = "周杰伦",
                            primaryArtistId = "artist-1",
                            albumId = "album-1",
                            albumTitle = "十一月的萧邦"
                        )
                    ),
                    activeIndex = 0,
                    playbackMode = PlaybackMode.LIST_LOOP,
                    showOriginalOrderInShuffle = false,
                    canReorder = true,
                    onDismiss = {},
                    onShowOriginalOrderInShuffleChange = {},
                    onSelect = {},
                    onClearAll = {},
                    onRemove = { removedIndex = it },
                    onMove = { _, _ -> },
                    onOpenSongDetail = { detailId = it.id },
                    onOpenArtist = { artistId = it },
                    onOpenAlbum = { albumId = it },
                    expandedMenuItemIdOverride = "queue-1"
                )
            }
        }

        composeRule.onNodeWithTag("playlist_sheet_more_queue-1").assertIsDisplayed()
        composeRule.onAllNodesWithText("下一首播放").assertCountEquals(0)
        composeRule.onAllNodesWithTag("playlist_sheet_action_detail_queue-1").assertCountEquals(1)
        composeRule.onNodeWithTag("playlist_sheet_action_detail_queue-1")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onAllNodesWithTag("playlist_sheet_action_artist_queue-1").assertCountEquals(1)
        composeRule.onNodeWithTag("playlist_sheet_action_artist_queue-1")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onAllNodesWithTag("playlist_sheet_action_album_queue-1").assertCountEquals(1)
        composeRule.onNodeWithTag("playlist_sheet_action_album_queue-1")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onAllNodesWithTag("playlist_sheet_action_remove_queue-1").assertCountEquals(1)
        composeRule.onNodeWithTag("playlist_sheet_action_remove_queue-1")
            .performSemanticsAction(SemanticsActions.OnClick)

        composeRule.runOnIdle {
            assertEquals("queue-1", detailId)
            assertEquals("artist-1", artistId)
            assertEquals("album-1", albumId)
            assertEquals(0, removedIndex)
        }
    }

    @Test
    fun playlistBottomSheet_missingArtistOrAlbumId_shouldHideUnsupportedQueueActions() {
        composeRule.setContent {
            PlayerLiteTheme {
                PlaylistBottomSheet(
                    visible = true,
                    items = listOf(
                        PlaylistItem(
                            id = "queue-2",
                            uri = "https://example.com/queue-2.mp3",
                            displayName = "稻香",
                            songId = "song-2",
                            title = "稻香"
                        )
                    ),
                    activeIndex = 0,
                    playbackMode = PlaybackMode.LIST_LOOP,
                    showOriginalOrderInShuffle = false,
                    canReorder = true,
                    onDismiss = {},
                    onShowOriginalOrderInShuffleChange = {},
                    onSelect = {},
                    onClearAll = {},
                    onRemove = {},
                    onMove = { _, _ -> },
                    expandedMenuItemIdOverride = "queue-2"
                )
            }
        }

        composeRule.onNodeWithTag("playlist_sheet_more_queue-2").assertIsDisplayed()
        composeRule.onAllNodesWithTag("playlist_sheet_action_detail_queue-2").assertCountEquals(1)
        composeRule.onAllNodesWithTag("playlist_sheet_action_remove_queue-2").assertCountEquals(1)
        composeRule.onAllNodesWithTag("playlist_sheet_action_artist_queue-2").assertCountEquals(0)
        composeRule.onAllNodesWithTag("playlist_sheet_action_album_queue-2").assertCountEquals(0)
    }

    private fun scrollAwayFromActiveItem() {
        composeRule.onNodeWithTag("playlist_sheet_list").performScrollToIndex(0)
        waitUntilFirstVisibleIndex(expected = 0)
    }

    private fun waitUntilFirstVisibleIndex(expected: Int) {
        val matcher = SemanticsMatcher.expectValue(PlaylistSheetFirstVisibleIndexKey, expected)
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithTag("playlist_sheet_list")
                .fetchSemanticsNodes()
                .singleOrNull()
                ?.config
                ?.getOrNull(PlaylistSheetFirstVisibleIndexKey) == expected
        }
        composeRule.onNodeWithTag("playlist_sheet_list").assert(matcher)
    }

    private fun buildPlaylistItems(prefix: String): List<PlaylistItem> {
        return List(40) { index ->
            PlaylistItem(
                id = "$prefix-$index",
                uri = "file:///$prefix-$index.mp3",
                displayName = "歌曲 $index"
            )
        }
    }
}
