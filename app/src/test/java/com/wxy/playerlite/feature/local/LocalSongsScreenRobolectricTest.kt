package com.wxy.playerlite.feature.local

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import com.wxy.playerlite.ui.theme.PlayerLiteTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocalSongsScreenRobolectricTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun cachedSongsState_shouldRenderScanActionAndPlaybackCallbacks() {
        var scanCount = 0
        var playAllCount = 0
        var playTrackIndex = -1
        var insertNextTrackId: String? = null
        var detailTrackId: String? = null

        composeRule.setContent {
            PlayerLiteTheme {
                LocalSongsScreen(
                    state = LocalSongsUiState(
                        songs = listOf(
                            LocalSongEntry(
                                id = "local-1",
                                contentUri = "content://media/external/audio/media/1",
                                title = "晴天",
                                artist = "周杰伦",
                                album = "叶惠美",
                                durationMs = 269000L
                            )
                        ),
                        hasCachedSongs = true
                    ),
                    onBack = {},
                    onRequestPermission = {},
                    onScan = { scanCount += 1 },
                    onPlayAll = { playAllCount += 1 },
                    onSongClick = { playTrackIndex = it },
                    onSongInsertNext = { insertNextTrackId = it.id },
                    onSongOpenDetail = { detailTrackId = it.id }
                )
            }
        }

        composeRule.onNodeWithTag("local_songs_scan_action").assertIsDisplayed().assertHasClickAction()
        composeRule.onNodeWithTag("local_songs_song_count").assertIsDisplayed()
        composeRule.onNodeWithTag("local_songs_play_all").assertIsDisplayed().assertHasClickAction()
        composeRule.onNodeWithTag("local_songs_item_local-1").assertIsDisplayed().assertHasClickAction()
        composeRule.onNodeWithTag(
            testTag = "local_songs_item_duration_local-1",
            useUnmergedTree = true
        ).assertExists()
        composeRule.onNodeWithTag("local_songs_item_more_local-1").assertIsDisplayed().assertHasClickAction()
        composeRule.onNodeWithText("1 首歌曲").assertIsDisplayed()
        composeRule.onNodeWithText("4:29", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("晴天").assertIsDisplayed()

        composeRule.onNodeWithTag("local_songs_scan_action").performClick()
        composeRule.onNodeWithTag("local_songs_play_all").performClick()
        composeRule.onNodeWithTag("local_songs_item_local-1").performClick()
        composeRule.onNodeWithTag("local_songs_item_more_local-1").performClick()
        composeRule.onNodeWithText("下一首播放").performClick()
        composeRule.onNodeWithTag("local_songs_item_more_local-1").performClick()
        composeRule.onNodeWithText("查看歌曲详情").performClick()

        composeRule.runOnIdle {
            assertEquals(1, scanCount)
            assertEquals(1, playAllCount)
            assertEquals(0, playTrackIndex)
            assertEquals("local-1", insertNextTrackId)
            assertEquals("local-1", detailTrackId)
        }
    }

    @Test
    fun cachedSongsState_pullToRefresh_shouldRequestScan() {
        var scanCount = 0

        composeRule.setContent {
            PlayerLiteTheme {
                LocalSongsScreen(
                    state = LocalSongsUiState(
                        songs = listOf(
                            LocalSongEntry(
                                id = "local-1",
                                contentUri = "content://media/external/audio/media/1",
                                title = "晴天",
                                artist = "周杰伦",
                                album = "叶惠美",
                                durationMs = 269000L
                            )
                        ),
                        hasCachedSongs = true
                    ),
                    onBack = {},
                    onRequestPermission = {},
                    onScan = { scanCount += 1 },
                    onPlayAll = {},
                    onSongClick = {},
                    onSongInsertNext = {},
                    onSongOpenDetail = {}
                )
            }
        }

        composeRule.onNodeWithTag("local_songs_list")
            .performTouchInput { swipeDown() }

        composeRule.runOnIdle {
            assertEquals(1, scanCount)
        }
    }

    @Test
    fun emptyState_shouldRenderWithoutPullRefreshCrash() {
        composeRule.setContent {
            PlayerLiteTheme {
                LocalSongsScreen(
                    state = LocalSongsUiState(),
                    onBack = {},
                    onRequestPermission = {},
                    onScan = {},
                    onPlayAll = {},
                    onSongClick = {},
                    onSongInsertNext = {},
                    onSongOpenDetail = {}
                )
            }
        }

        composeRule.onNodeWithText("还没有扫描到本地歌曲").assertIsDisplayed()
    }

    @Test
    fun formatLocalSongDuration_shouldRenderMinuteAndHourFormats() {
        assertEquals("0:00", formatLocalSongDuration(0L))
        assertEquals("4:29", formatLocalSongDuration(269_000L))
        assertEquals("1:01:01", formatLocalSongDuration(3_661_000L))
    }
}
